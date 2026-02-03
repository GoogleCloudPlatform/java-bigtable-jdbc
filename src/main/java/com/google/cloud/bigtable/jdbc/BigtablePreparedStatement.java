/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.bigtable.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
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
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.sql.BoundStatement;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;
import com.google.cloud.bigtable.jdbc.util.Parameter;
import com.google.cloud.bigtable.jdbc.util.SqlParser;
import com.google.cloud.bigtable.jdbc.util.SqlTypeEnum;

public class BigtablePreparedStatement extends BigtableStatement implements PreparedStatement {
  protected final String sql;
  protected com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement cachedPreparedStatement =
      null;
  protected boolean isCached = false;
  protected String cachedSql = null;

  protected final Map<Integer, Parameter> parameters = new HashMap<>();
  protected static final String PARAM_PREFIX = "param";

  public BigtablePreparedStatement(BigtableConnection connection, String sql,
      BigtableDataClient client) {
    super(connection, client);
    this.sql = sql;
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException(
        "This method cannot be called on a PreparedStatement.");
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException(
        "This method cannot be called on a PreparedStatement.");
  }

  @Override
  public boolean execute(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException(
        "This method cannot be called on a PreparedStatement.");
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException(
        "This method cannot be called on a PreparedStatement.");
  }

  private String getParamName(int parameterIndex) {
    return PARAM_PREFIX + parameterIndex;
  }

  private SqlType<?> mapToSqlType(String type) {
    return SqlTypeEnum.fromLabel(type).getSqlType();
  }

  private void setParameter(int parameterIndex, String type, Object value) throws SQLException {
    Parameter existing = parameters.get(parameterIndex);

    if (isCached && existing != null && !existing.getTypeLabel().equals(type)) {
      throw new SQLException("Cannot change parameter type after statement is cached. "
          + "Expected: " + existing.getTypeLabel() + ", got: " + type);
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

  @Override
  public ResultSet executeQuery() throws SQLException {
    checkClosed();
    try {
      com.google.cloud.bigtable.data.v2.models.sql.ResultSet resultSet = prepareQuery();
      this.resultSets.clear();
      this.resultSets.add(new BigtableResultSet(resultSet));
      this.currentResultIndex = 0;
      return this.resultSets.get(0);
    } catch (Exception e) {
      throw new SQLException("Failed to execute query: " + e.getMessage(), e);
    }
  }

  private com.google.cloud.bigtable.data.v2.models.sql.ResultSet prepareQuery() {
    if (!isCached) {
      cachedSql = SqlParser.replacePlaceholdersWithNamedParams(sql, parameters.size());

      Map<String, SqlType<?>> parameterTypes = new HashMap<>();
      for (Map.Entry<Integer, Parameter> entry : parameters.entrySet()) {
        String paramName = getParamName(entry.getKey());
        String type = entry.getValue().getTypeLabel();
        parameterTypes.put(paramName, mapToSqlType(type));
      }

      cachedPreparedStatement = client.prepareStatement(cachedSql, parameterTypes);
      isCached = true;
    }
    BoundStatement.Builder bound = cachedPreparedStatement.bind();

    for (Map.Entry<Integer, Parameter> entry : parameters.entrySet()) {
      String paramName = getParamName(entry.getKey());
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
  public void setCharacterStream(int parameterIndex, Reader reader, int length)
      throws SQLException {
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
    com.google.cloud.Date cloudDate = com.google.cloud.Date.fromYearMonthDay(localDate.getYear(),
        localDate.getMonthValue(), localDate.getDayOfMonth());
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
      instant = x.toInstant().atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId).toInstant();
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
        Parameter parameter = parameters.get(param);
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

        Parameter parameter = parameters.get(param);
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

        Parameter parameter = parameters.get(param);
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
        if (parameter == null) {
          throw new SQLException("Parameter not found: " + param);
        }
        return SqlTypeEnum.fromLabel(parameter.getTypeLabel()).getSqlTypeCode();
      }

      @Override
      public String getParameterTypeName(int param) throws SQLException {
        Parameter parameter = parameters.get(param);
        if (parameter == null) {
          throw new SQLException("Parameter not found: " + param);
        }
        SqlType<?> sqlType = SqlTypeEnum.fromLabel(parameter.getTypeLabel()).getSqlType();
        return sqlType.toString();
      }

      @Override
      public String getParameterClassName(int param) throws SQLException {
        Parameter parameter = parameters.get(param);
        if (parameter == null) {
          throw new SQLException("Parameter not found: " + param);
        }
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
  public void setNCharacterStream(int parameterIndex, Reader value, long length)
      throws SQLException {
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
  public void setBlob(int parameterIndex, InputStream inputStream, long length)
      throws SQLException {
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
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
      throws SQLException {
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
  public void setCharacterStream(int parameterIndex, Reader reader, long length)
      throws SQLException {
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
}
