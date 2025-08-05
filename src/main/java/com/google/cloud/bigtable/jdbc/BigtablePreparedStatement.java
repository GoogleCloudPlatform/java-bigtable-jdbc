/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.bigtable.jdbc;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.sql.BoundStatement;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;
import com.google.cloud.bigtable.jdbc.util.Parameter;
import com.google.cloud.bigtable.jdbc.util.SqlParser;
import com.google.cloud.bigtable.jdbc.util.SqlTypeEnum;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BigtablePreparedStatement implements PreparedStatement {
  private final BigtableDataClient client;
  private final String sql;
  private com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement cachedPreparedStatement = null;
  private boolean isCached = false;
  private boolean isClosed = false;
  private final List<ResultSet> resultSets = new ArrayList<>();
  private int currentResultIndex = -1;
  private String cachedSql = null;

  private final Map<Integer, Parameter> parameters = new HashMap<>();
  private static final String PARAM_PREFIX = "param";
  public BigtablePreparedStatement(String sql, BigtableDataClient client) {
    this.sql = sql;
    this.client = client;
  }

  private String replacePlaceholdersWithNamedParams(String sql, int paramCount) {
    StringBuilder parsed = new StringBuilder();
    int paramIndex = 1;

    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);

      if (c == '\'' && !inDoubleQuote) {
        inSingleQuote = !inSingleQuote;
        parsed.append(c);
        continue;
      }
      if (c == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
        parsed.append(c);
        continue;
      }

      if (c == '?' && !inSingleQuote && !inDoubleQuote) {
        if (paramIndex > paramCount) {
          throw new IllegalArgumentException("More placeholders than paramCount");
        }
        parsed.append("@param").append(paramIndex++);
      } else {
        parsed.append(c);
      }
    }

    if (paramIndex <= paramCount) {
      throw new IllegalArgumentException("Fewer placeholders than paramCount");
    }

    return parsed.toString();
  }

  private SqlType<?> mapToSqlType(String type) {
    return SqlTypeEnum.fromLabel(type).getSqlType();
  }

  private void setParameter(int parameterIndex, String type, Object value) throws SQLException {
    Parameter existing = parameters.get(parameterIndex);

    if (isCached && existing != null && !existing.getTypeLabel().equals(type)) {
      throw new SQLException("Cannot change parameter type after statement is cached. " +
        "Expected: " + existing.getTypeLabel() + ", got: " + type);
    }
    try {
      SqlTypeEnum.fromLabel(type);
      parameters.put(parameterIndex, new Parameter(type, value));
    } catch (IllegalArgumentException e) {
      throw new SQLException("Unsupported SQL type: " + type, e);
    }
  }

  private SqlType sqlTypeToTypeLabel(int sqlType) throws SQLException {
    switch (sqlType) {
      case Types.INTEGER:
      case Types.SMALLINT:
      case Types.TINYINT:
      case Types.BIGINT:
        return SqlType.int64();
      case Types.BOOLEAN:
      case Types.BIT:
        return SqlType.bool();
      case Types.VARCHAR:
      case Types.CHAR:
      case Types.LONGVARCHAR:
        return SqlType.string();
      case Types.DOUBLE:
      case Types.FLOAT:
      case Types.REAL:
        return SqlType.float64();
      case Types.DATE:
        return SqlType.date();
      case Types.TIMESTAMP:
        return SqlType.timestamp();
      case Types.BINARY:
      case Types.VARBINARY:
      case Types.LONGVARBINARY:
        return SqlType.bytes();
      case Types.JAVA_OBJECT:
        return SqlType.struct();
      default:
        throw new SQLException("Unsupported SQL type: " + sqlType);
    }
  }

  private void checkClosed() throws SQLException {
    if (isClosed) {
      throw new SQLException("This Statement is already closed.");
    }
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    checkClosed();
    com.google.cloud.bigtable.data.v2.models.sql.ResultSet resultSet = prepareQuery();
    return new BigtableResultSet(resultSet);
  }

  private com.google.cloud.bigtable.data.v2.models.sql.ResultSet prepareQuery() {
    if (!isCached) {
      cachedSql = SqlParser.replacePlaceholdersWithNamedParams(sql, parameters.size());

      Map<String, SqlType<?>> parameterTypes = new HashMap<>();
      for (Map.Entry<Integer, Parameter> entry : parameters.entrySet()) {
        String paramName = PARAM_PREFIX + (entry.getKey() - 1);
        String type = entry.getValue().getTypeLabel();
        parameterTypes.put(paramName, mapToSqlType(type));
      }

      cachedPreparedStatement = client.prepareStatement(cachedSql, parameterTypes);
      isCached = true;
    }
    BoundStatement.Builder bound = cachedPreparedStatement.bind();

    for (Map.Entry<Integer, Parameter> entry : parameters.entrySet()) {
      String paramName = PARAM_PREFIX + (entry.getKey() - 1);
      String type = entry.getValue().getTypeLabel();
      Object value = entry.getValue().getValue();

      SqlTypeEnum.fromLabel(type).bind(bound, paramName, value);
    }
    return client.executeQuery(bound.build());
  }

  @Override
  public int executeUpdate() throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    checkClosed();
    SqlType type = sqlTypeToTypeLabel(sqlType);
    setParameter(parameterIndex, type.toString(), null);
  }

  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "bool", x);
  }

  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "int", (long) x);
  }

  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "int", (long) x);
  }

  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "int", x);
  }

  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "int", x);
  }

  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "float", x);
  }

  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "double", x);
  }

  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setBigDecimal is not supported");
  }

  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "string", x);
  }

  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "bytes", x);
  }

  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "date", com.google.cloud.Date.fromJavaUtilDate(x));
  }

  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setTime is not supported");
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "timestamp", x.toInstant());
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setAsciiStream is not supported");
  }

  @Override
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setUnicodeStream is not supported");
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setBinaryStream is not supported");
  }

  @Override
  public void clearParameters() throws SQLException {
    checkClosed();
    parameters.clear();
    isCached = false;
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    throw new SQLFeatureNotSupportedException("setObject is not supported");
  }

  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setObject is not supported");
  }

  @Override
  public boolean execute() throws SQLException {
    checkClosed();
    if (sql == null || sql.trim().isEmpty()) {
      throw new SQLException("No SQL statement set.");
    }

    String trimmedSql = sql.trim().toLowerCase();
    try {
      com.google.cloud.bigtable.data.v2.models.sql.ResultSet bigtableResultSet = prepareQuery();
      resultSets.clear();
      resultSets.add(new BigtableResultSet(bigtableResultSet));
      currentResultIndex = 0;
      return true;
    } catch (Exception e) {
      throw new SQLException("Failed to execute query: " + e.getMessage(), e);
    }
  }

  @Override
  public void addBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException("addBatch is not supported");
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setCharacterStream is not supported");
  }

  @Override
  public void setRef(int parameterIndex, Ref x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setRef is not supported");
  }

  @Override
  public void setBlob(int parameterIndex, Blob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setBlob is not supported");
  }

  @Override
  public void setClob(int parameterIndex, Clob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setClob is not supported");
  }

  @Override
  public void setArray(int parameterIndex, Array x) throws SQLException {
    checkClosed();
    if (x == null) {
      throw new SQLException("Null arrays are not supported");
    }

    SqlTypeEnum elementEnum = SqlTypeEnum.fromJdbcType(x.getBaseType());
    SqlType<?> elementType = elementEnum.getSqlType();
    SqlType.Array<?> arrayType = SqlType.arrayOf(elementType);
    String arrayTypeLabel = null;
    Object[] array = (Object[]) x.getArray();

    for (SqlTypeEnum e : SqlTypeEnum.values()) {
      if (e.getSqlType().equals(arrayType)) {
        arrayTypeLabel = e.name();
      }
    }
    setParameter(parameterIndex, arrayTypeLabel, array);
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    throw new SQLFeatureNotSupportedException("getMetaData is not supported");
  }

  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    checkClosed();
    LocalDate localDate;
    if (cal != null) {
      ZonedDateTime zonedDateTime = x.toLocalDate().atStartOfDay(cal.getTimeZone().toZoneId());
      localDate = zonedDateTime.toLocalDate();
    } else {
      localDate = x.toLocalDate();
    }
    com.google.cloud.Date cloudDate = com.google.cloud.Date.fromYearMonthDay(
      localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth()
    );
    setParameter(parameterIndex, "date", cloudDate);
  }

  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    throw new SQLFeatureNotSupportedException("setTime is not supported");
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    checkClosed();
    Instant instant;
    if (cal != null) {
      ZoneId zoneId = cal.getTimeZone().toZoneId();
      instant = x.toInstant().atZone(ZoneOffset.UTC)
        .withZoneSameInstant(zoneId)
        .toInstant();
    } else {
      instant = x.toInstant();
    }

    setParameter(parameterIndex, "timestamp", instant);
  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    checkClosed();
    SqlType type = sqlTypeToTypeLabel(sqlType);
    setParameter(parameterIndex, type.toString(), null);
  }

  @Override
  public void setURL(int parameterIndex, URL x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setURL is not supported");
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    return new ParameterMetaData() {
      @Override
      public int getParameterCount() throws SQLException {
        return parameters.size();
      }

      @Override
      public int isNullable(int param) throws SQLException {
        return ParameterMetaData.parameterNullable;
      }

      @Override
      public boolean isSigned(int param) throws SQLException {
        Parameter parameter = parameters.get(param - 1);
        if (parameter == null) {
          throw new SQLException("Parameter not found: " + param);
        }

        String sqlType = parameter.getTypeLabel();
        SqlTypeEnum type = SqlTypeEnum.fromLabel(sqlType);

        switch (type) {
          case INT:
          case FLOAT:
          case DOUBLE:
            return true;
          default:
            return false;
        }
      }

      @Override
      public int getPrecision(int param) throws SQLException {
        checkClosed();

        Parameter parameter = parameters.get(param - 1);
        if (parameter == null) {
          throw new SQLException("Parameter not found: " + param);
        }

        String sqlTypeLabel = parameter.getTypeLabel();
        SqlTypeEnum type = SqlTypeEnum.fromLabel(sqlTypeLabel);

        switch (type) {
          case INT:
          case DATE:
            return 10;
          case FLOAT:
            return 24;
          case DOUBLE:
            return 53;
          case TIMESTAMP:
            return 23;
          case STRING:
          case BYTES:
            return Integer.MAX_VALUE;
          default:
            return 0;
        }
      }

      @Override
      public int getScale(int param) throws SQLException {
        checkClosed();

        Parameter parameter = parameters.get(param - 1);
        if (parameter == null) {
          throw new SQLException("Parameter not found: " + param);
        }

        String sqlType = parameter.getTypeLabel();
        SqlTypeEnum type = SqlTypeEnum.fromLabel(sqlType);

        switch (type) {
          case FLOAT:
            return 7;
          case DOUBLE:
            return 15;
          default:
            return 0;
        }
      }

      @Override
      public int getParameterType(int param) throws SQLException {
        Parameter parameter = parameters.get(param);
        return SqlTypeEnum.fromLabel(parameter.getTypeLabel()).getSqlTypeCode();
      }

      @Override
      public String getParameterTypeName(int param) throws SQLException {
        Parameter parameter = parameters.get(param);
        SqlType<?> sqlType = SqlTypeEnum.fromLabel(parameter.getTypeLabel()).getSqlType();
        return sqlType.toString();
      }

      @Override
      public String getParameterClassName(int param) throws SQLException {
        Parameter parameter = parameters.get(param);
        return SqlTypeEnum.fromLabel(parameter.getTypeLabel()).getJavaClassName();
      }

      @Override
      public int getParameterMode(int param) throws SQLException {
        return ParameterMetaData.parameterModeIn;
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("unwrap is not supported");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("isWrapperFor is not supported");
      }
    };
  }

  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setRowId is not supported");
  }

  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    checkClosed();
    setParameter(parameterIndex, "string", value);
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setNCharacterStream is not supported");
  }

  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    throw new SQLFeatureNotSupportedException("setNClob is not supported");
  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setClob is not supported");
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setBlob is not supported");
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setNClob is not supported");
  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    throw new SQLFeatureNotSupportedException("setSQLXML is not supported");
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
    throw new SQLFeatureNotSupportedException("setObject is not supported");
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setAsciiStream is not supported");
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setBinaryStream is not supported");
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("setCharacterStream is not supported");
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setAsciiStream is not supported");
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    throw new SQLFeatureNotSupportedException("setBinaryStream is not supported");
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("setCharacterStream is not supported");
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    throw new SQLFeatureNotSupportedException("setNCharacterStream is not supported");
  }

  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("setClob is not supported");
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    throw new SQLFeatureNotSupportedException("setBlob is not supported");
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("setNClob is not supported");
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeQuery is not supported");
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public void close() throws SQLException {
    if (!isClosed) {
      isClosed = true;
    }
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    throw new SQLFeatureNotSupportedException("getMaxFieldSize is not supported");
  }

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    throw new SQLFeatureNotSupportedException("setMaxFieldSize is not supported");
  }

  @Override
  public int getMaxRows() throws SQLException {
    throw new SQLFeatureNotSupportedException("getMaxRows is not supported");
  }

  @Override
  public void setMaxRows(int max) throws SQLException {
    throw new SQLFeatureNotSupportedException("setMaxRows is not supported");
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    throw new SQLFeatureNotSupportedException("setEscapeProcessing is not supported");
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    throw new SQLFeatureNotSupportedException("getQueryTimeout is not supported");
  }

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    throw new SQLFeatureNotSupportedException("setQueryTimeout is not supported");
  }

  @Override
  public void cancel() throws SQLException {
    throw new SQLFeatureNotSupportedException("cancel is not supported");
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return null;
  }

  @Override
  public void clearWarnings() throws SQLException {

  }

  @Override
  public void setCursorName(String name) throws SQLException {
    throw new SQLFeatureNotSupportedException("setCursorName is not supported");
  }

  @Override
  public boolean execute(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    if (currentResultIndex >= 0 && currentResultIndex < resultSets.size()) {
      return resultSets.get(currentResultIndex);
    }
    return null;
  }

  @Override
  public int getUpdateCount() throws SQLException {
    throw new SQLFeatureNotSupportedException("getUpdateCount is not supported");
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    if (currentResultIndex + 1 < resultSets.size()) {
      currentResultIndex++;
      return true;
    }
    return false;
  }

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    throw new SQLFeatureNotSupportedException("setFetchDirection is not supported");
  }

  @Override
  public int getFetchDirection() throws SQLException {
    throw new SQLFeatureNotSupportedException("getFetchDirection is not supported");
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    throw new SQLFeatureNotSupportedException("setFetchSize is not supported");
  }

  @Override
  public int getFetchSize() throws SQLException {
    throw new SQLFeatureNotSupportedException("getFetchSize is not supported");
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    throw new SQLFeatureNotSupportedException("getResultSetConcurrency is not supported");
  }

  @Override
  public int getResultSetType() throws SQLException {
    throw new SQLFeatureNotSupportedException("getResultSetType is not supported");
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("addBatch is not supported");
  }

  @Override
  public void clearBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException("clearBatch is not supported");
  }

  @Override
  public int[] executeBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException("executeBatch is not supported");
  }

  @Override
  public Connection getConnection() throws SQLException {
    throw new SQLFeatureNotSupportedException("getConnection is not supported");
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    throw new SQLFeatureNotSupportedException("getMoreResults is not supported");
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    throw new SQLFeatureNotSupportedException("getGeneratedKeys is not supported");
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    throw new SQLFeatureNotSupportedException("getResultSetHoldability is not supported");
  }

  @Override
  public boolean isClosed() throws SQLException {
    throw new SQLFeatureNotSupportedException("isClosed is not supported");
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    throw new SQLFeatureNotSupportedException("setPoolable is not supported");
  }

  @Override
  public boolean isPoolable() throws SQLException {
    throw new SQLFeatureNotSupportedException("isPoolable is not supported");
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    throw new SQLFeatureNotSupportedException("closeOnCompletion is not supported");
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    throw new SQLFeatureNotSupportedException("isCloseOnCompletion is not supported");
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException("unwrap is not supported");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException("isWrapperFor is not supported");
  }
}
