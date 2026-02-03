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
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import com.google.cloud.bigtable.data.v2.models.sql.ColumnMetadata;
import com.google.cloud.bigtable.data.v2.models.sql.ResultSet;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;

public class BigtableResultSet implements java.sql.ResultSet {
  // Bigtable data result set is 0 index.
  private final ResultSet btDataResultSet;
  private List<Map<String, Object>> rows;
  private int currentRow = -1;
  private boolean lastValueWasNull = false;
  private boolean hasMoved = false;
  private boolean isAfterLast = false;
  private boolean closed = false;

  public BigtableResultSet(ResultSet bigtableResultSet) {
    this.btDataResultSet = bigtableResultSet;
  }

  public BigtableResultSet(ResultSet bigtableResultSet, List<Map<String, Object>> rows) {
    this.btDataResultSet = bigtableResultSet;
    this.rows = rows;
    this.currentRow = -1;
  }

  private int toBigtableDataResultSetIndex(int oneIndex) {
    return oneIndex - 1;
  }

  private int toJDBCResultSetIndex(int zeroBasedIndex) {
    return zeroBasedIndex + 1;
  }

  private Object getTypedValue(ResultSet resultSet, ColumnMetadata column) throws SQLException {
    String columnName = column.name();
    Object value;

    if (resultSet.isNull(columnName)) {
      lastValueWasNull = true;
      return null;
    } else {
      lastValueWasNull = false;
    }

    switch (column.type().getCode()) {
      case STRING:
        value = resultSet.getString(columnName);
        break;
      case INT64:
        value = resultSet.getLong(columnName);
        break;
      case FLOAT64:
        value = resultSet.getDouble(columnName);
        break;
      case FLOAT32:
        value = resultSet.getFloat(columnName);
        break;
      case BOOL:
        value = resultSet.getBoolean(columnName);
        break;
      case BYTES:
        value = resultSet.getBytes(columnName);
        break;
      case STRUCT:
        value = resultSet.getStruct(columnName);
        break;
      case DATE:
        value = resultSet.getDate(columnName);
        break;
      case ARRAY:
        SqlType<?> columnType1 = column.type();
        if (columnType1 instanceof SqlType.Array<?>) {
          SqlType<?> elementType = ((SqlType.Array<?>) columnType1).getElementType();
          value = resultSet.getList(columnName, SqlType.arrayOf(elementType));
        } else {
          throw new SQLException("Expected ARRAY type but got: " + columnType1);
        }
        break;
      case TIMESTAMP:
        value = resultSet.getTimestamp(columnName);
        break;
      case MAP:
        if (column.type() instanceof SqlType.Map) {
          SqlType.Map<Object, Object> mapType = (SqlType.Map<Object, Object>) column.type();
          value = resultSet.getMap(columnName, mapType);
        } else {
          throw new SQLException("Expected MAP type but got: " + column.type().getCode());
        }
        break;
      default:
        value = null;
    }
    lastValueWasNull = (value == null);
    return value;
  }

  private void checkClosed() throws SQLException {
    if (closed) {
      throw new SQLException("ResultSet is closed");
    }
  }

  @Override
  public boolean next() throws SQLException {
    checkClosed();
    hasMoved = true;
    boolean hasNext = btDataResultSet.next();
    isAfterLast = !hasNext;
    return hasNext;
  }

  @Override
  public void close() throws SQLException {
    if (closed) {
      return;
    }
    closed = true;

    try {
      if (btDataResultSet != null) {
        btDataResultSet.close();
      }
    } catch (Exception ignored) {

    }
  }

  @Override
  public boolean wasNull() throws SQLException {
    checkClosed();
    return lastValueWasNull;
  }

  @Override
  public String getString(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return null;
    }
    lastValueWasNull = false;
    return btDataResultSet.getString(bigtableDataResultSetIndex);
  }

  @Override
  public boolean getBoolean(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return false;
    }

    lastValueWasNull = false;
    return btDataResultSet.getBoolean(bigtableDataResultSetIndex);
  }

  @Override
  public byte getByte(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return 0;
    }

    lastValueWasNull = false;
    return (byte) btDataResultSet.getLong(bigtableDataResultSetIndex);
  }

  @Override
  public short getShort(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return 0;
    }

    lastValueWasNull = false;
    return (short) btDataResultSet.getLong(bigtableDataResultSetIndex);
  }

  @Override
  public int getInt(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return 0;
    }

    lastValueWasNull = false;
    return Math.toIntExact(btDataResultSet.getLong(bigtableDataResultSetIndex));
  }

  @Override
  public long getLong(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return 0L;
    }

    lastValueWasNull = false;
    return btDataResultSet.getLong(bigtableDataResultSetIndex);
  }

  @Override
  public float getFloat(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return 0.0F;
    }

    lastValueWasNull = false;
    return btDataResultSet.getFloat(bigtableDataResultSetIndex);
  }

  @Override
  public double getDouble(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return 0.0;
    }

    lastValueWasNull = false;
    return btDataResultSet.getDouble(bigtableDataResultSetIndex);
  }

  @Override
  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBigDecimal is not supported");
  }

  @Override
  public byte[] getBytes(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return null;
    }

    lastValueWasNull = false;
    return btDataResultSet.getBytes(bigtableDataResultSetIndex).toByteArray();
  }

  @Override
  public Date getDate(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return null;
    }

    lastValueWasNull = false;
    Instant instant = btDataResultSet.getTimestamp(bigtableDataResultSetIndex);
    return java.sql.Date.valueOf(instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate());
  }

  @Override
  public Time getTime(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getTime is not supported");
  }

  @Override
  public Timestamp getTimestamp(int columnIndex) throws SQLException {
    checkClosed();

    int bigtableDataResultSetIndex = toBigtableDataResultSetIndex(columnIndex);

    if (btDataResultSet.isNull(bigtableDataResultSetIndex)) {
      lastValueWasNull = true;
      return null;
    }

    lastValueWasNull = false;
    Instant instant = btDataResultSet.getTimestamp(bigtableDataResultSetIndex);
    return java.sql.Timestamp.from(instant);
  }

  @Override
  public InputStream getAsciiStream(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getAsciiStream is not supported");
  }

  @Override
  public InputStream getUnicodeStream(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getUnicodeStream is not supported");
  }

  @Override
  public InputStream getBinaryStream(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBinaryStream is not supported");
  }

  @Override
  public String getString(String columnLabel) throws SQLException {
    checkClosed();
    try {
      int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
      int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
      String value = getString(columnIndex);
      lastValueWasNull = (value == null);
      return value;
    } catch (IllegalArgumentException e) {
      throw new SQLException(e.getMessage(), e);
    }
  }

  @Override
  public boolean getBoolean(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getBoolean(columnIndex);
  }

  @Override
  public byte getByte(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getByte(columnIndex);
  }

  @Override
  public short getShort(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getShort(columnIndex);
  }

  @Override
  public int getInt(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getInt(columnIndex);
  }

  @Override
  public long getLong(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getLong(columnIndex);
  }

  @Override
  public float getFloat(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getFloat(columnIndex);
  }

  @Override
  public double getDouble(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getDouble(columnIndex);
  }

  @Override
  public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBigDecimal is not supported");
  }

  @Override
  public byte[] getBytes(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getBytes(columnIndex);
  }

  @Override
  public Date getDate(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getDate(columnIndex);
  }

  @Override
  public Time getTime(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getTime is not supported");
  }

  @Override
  public Timestamp getTimestamp(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);
    return getTimestamp(columnIndex);
  }

  @Override
  public InputStream getAsciiStream(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getAsciiStream is not supported");
  }

  @Override
  public InputStream getUnicodeStream(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getUnicodeStream is not supported");
  }

  @Override
  public InputStream getBinaryStream(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBinaryStream is not supported");
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return null;
  }

  @Override
  public void clearWarnings() throws SQLException {}

  @Override
  public String getCursorName() throws SQLException {
    throw new SQLFeatureNotSupportedException("getCursorName is not supported");
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return new BigtableResultSetMetaData(btDataResultSet.getMetadata().getColumns());
  }

  @Override
  public Object getObject(int columnIndex) throws SQLException {
    checkClosed();
    if (btDataResultSet == null) {
      throw new SQLException("Result set is null");
    }
    int zeroBasedIndex = toBigtableDataResultSetIndex(columnIndex);

    if (zeroBasedIndex < 0 || zeroBasedIndex >= btDataResultSet.getMetadata().getColumns().size()) {
      throw new SQLException("Invalid column index: " + columnIndex);
    }

    ColumnMetadata column = btDataResultSet.getMetadata().getColumns().get(zeroBasedIndex);
    return getTypedValue(btDataResultSet, column);
  }

  @Override
  public Object getObject(String columnLabel) throws SQLException {
    checkClosed();
    int zeroBasedIndex = btDataResultSet.getMetadata().getColumnIndex(columnLabel);
    int columnIndex = toJDBCResultSetIndex(zeroBasedIndex);

    return getObject(columnIndex);
  }

  @Override
  public int findColumn(String columnLabel) throws SQLException {
    checkClosed();
    try {
      return toJDBCResultSetIndex(btDataResultSet.getMetadata().getColumnIndex(columnLabel));
    } catch (RuntimeException e) {
      throw new SQLException("Column not found: " + columnLabel);
    }
  }

  @Override
  public Reader getCharacterStream(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getCharacterStream is not supported");
  }

  @Override
  public Reader getCharacterStream(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getCharacterStream is not supported");
  }

  @Override
  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBigDecimal(int) is not supported");
  }

  @Override
  public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBigDecimal(String) is not supported");
  }

  @Override
  public boolean isBeforeFirst() throws SQLException {
    checkClosed();
    return !hasMoved;
  }

  @Override
  public boolean isAfterLast() throws SQLException {
    checkClosed();
    return isAfterLast;
  }

  @Override
  public boolean isFirst() throws SQLException {
    throw new SQLFeatureNotSupportedException("isFirst is not supported");
  }

  @Override
  public boolean isLast() throws SQLException {
    throw new SQLFeatureNotSupportedException("isLast is not supported");
  }

  @Override
  public void beforeFirst() throws SQLException {
    throw new SQLFeatureNotSupportedException("beforeFirst is not supported");
  }

  @Override
  public void afterLast() throws SQLException {
    throw new SQLFeatureNotSupportedException("afterLast is not supported");
  }

  @Override
  public boolean first() throws SQLException {
    throw new SQLFeatureNotSupportedException("first is not supported");
  }

  @Override
  public boolean last() throws SQLException {
    throw new SQLFeatureNotSupportedException("last is not supported");
  }

  @Override
  public int getRow() throws SQLException {
    return 0;
  }

  @Override
  public boolean absolute(int row) throws SQLException {
    throw new SQLFeatureNotSupportedException("absolute is not supported");
  }

  @Override
  public boolean relative(int rows) throws SQLException {
    throw new SQLFeatureNotSupportedException("relative is not supported");
  }

  @Override
  public boolean previous() throws SQLException {
    throw new SQLFeatureNotSupportedException("previous is not supported");
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
  public int getType() throws SQLException {
    throw new SQLFeatureNotSupportedException("getType is not supported");
  }

  @Override
  public int getConcurrency() throws SQLException {
    throw new SQLFeatureNotSupportedException("getConcurrency is not supported");
  }

  @Override
  public boolean rowUpdated() throws SQLException {
    throw new SQLFeatureNotSupportedException("rowUpdated is not supported");
  }

  @Override
  public boolean rowInserted() throws SQLException {
    throw new SQLFeatureNotSupportedException("rowInserted is not supported");
  }

  @Override
  public boolean rowDeleted() throws SQLException {
    throw new SQLFeatureNotSupportedException("rowDeleted is not supported");
  }

  @Override
  public void updateNull(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNull is not supported");
  }

  @Override
  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBoolean is not supported");
  }

  @Override
  public void updateByte(int columnIndex, byte x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateByte is not supported");
  }

  @Override
  public void updateShort(int columnIndex, short x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateShort is not supported");
  }

  @Override
  public void updateInt(int columnIndex, int x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateInt is not supported");
  }

  @Override
  public void updateLong(int columnIndex, long x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateLong is not supported");
  }

  @Override
  public void updateFloat(int columnIndex, float x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateFloat is not supported");
  }

  @Override
  public void updateDouble(int columnIndex, double x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateDouble is not supported");
  }

  @Override
  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBigDecimal is not supported");
  }

  @Override
  public void updateString(int columnIndex, String x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateString is not supported");
  }

  @Override
  public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBytes is not supported");
  }

  @Override
  public void updateDate(int columnIndex, Date x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateDate is not supported");
  }

  @Override
  public void updateTime(int columnIndex, Time x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateTime is not supported");
  }

  @Override
  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateTimestamp is not supported");
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateAsciiStream is not supported");
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBinaryStream is not supported");
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateCharacterStream is not supported");
  }

  @Override
  public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateObject is not supported");
  }

  @Override
  public void updateObject(int columnIndex, Object x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateObject is not supported");
  }

  @Override
  public void updateNull(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNull is not supported");
  }

  @Override
  public void updateBoolean(String columnLabel, boolean x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBoolean is not supported");
  }

  @Override
  public void updateByte(String columnLabel, byte x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateByte is not supported");
  }

  @Override
  public void updateShort(String columnLabel, short x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateShort is not supported");
  }

  @Override
  public void updateInt(String columnLabel, int x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateInt is not supported");
  }

  @Override
  public void updateLong(String columnLabel, long x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateLong is not supported");
  }

  @Override
  public void updateFloat(String columnLabel, float x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateFloat is not supported");
  }

  @Override
  public void updateDouble(String columnLabel, double x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateDouble is not supported");
  }

  @Override
  public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBigDecimal is not supported");
  }

  @Override
  public void updateString(String columnLabel, String x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateString is not supported");
  }

  @Override
  public void updateBytes(String columnLabel, byte[] x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBytes is not supported");
  }

  @Override
  public void updateDate(String columnLabel, Date x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateDate is not supported");
  }

  @Override
  public void updateTime(String columnLabel, Time x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateTime is not supported");
  }

  @Override
  public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateTimestamp is not supported");
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateAsciiStream is not supported");
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, int length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBinaryStream is not supported");
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, int length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateCharacterStream is not supported");
  }

  @Override
  public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateObject is not supported");
  }

  @Override
  public void updateObject(String columnLabel, Object x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateObject is not supported");
  }

  @Override
  public void insertRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("insertRow is not supported");
  }

  @Override
  public void updateRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("updateRow is not supported");
  }

  @Override
  public void deleteRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("deleteRow is not supported");
  }

  @Override
  public void refreshRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("refreshRow is not supported");
  }

  @Override
  public void cancelRowUpdates() throws SQLException {
    throw new SQLFeatureNotSupportedException("cancelRowUpdates is not supported");
  }

  @Override
  public void moveToInsertRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("moveToInsertRow is not supported");
  }

  @Override
  public void moveToCurrentRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("moveToCurrentRow is not supported");
  }

  @Override
  public Statement getStatement() throws SQLException {
    throw new SQLFeatureNotSupportedException("getStatement is not supported");
  }

  @Override
  public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
    throw new SQLFeatureNotSupportedException("getObject is not supported");
  }

  @Override
  public Ref getRef(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getRef is not supported");
  }

  @Override
  public Blob getBlob(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBlob is not supported");
  }

  @Override
  public Clob getClob(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getClob is not supported");
  }

  @Override
  public Array getArray(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getArray is not supported");
  }

  @Override
  public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
    throw new SQLFeatureNotSupportedException("getObject is not supported");
  }

  @Override
  public Ref getRef(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getRef is not supported");
  }

  @Override
  public Blob getBlob(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getBlob is not supported");
  }

  @Override
  public Clob getClob(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getClob is not supported");
  }

  @Override
  public Array getArray(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getArray is not supported");
  }

  @Override
  public Date getDate(int columnIndex, Calendar cal) throws SQLException {
    return null;
  }

  @Override
  public Date getDate(String columnLabel, Calendar cal) throws SQLException {
    return null;
  }

  @Override
  public Time getTime(int columnIndex, Calendar cal) throws SQLException {
    throw new SQLFeatureNotSupportedException("getTime is not supported");
  }

  @Override
  public Time getTime(String columnLabel, Calendar cal) throws SQLException {
    throw new SQLFeatureNotSupportedException("getTime is not supported");
  }

  @Override
  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    return null;
  }

  @Override
  public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
    return null;
  }

  @Override
  public URL getURL(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getURL is not supported");
  }

  @Override
  public URL getURL(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getURL is not supported");
  }

  @Override
  public void updateRef(int columnIndex, Ref x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateRef is not supported");
  }

  @Override
  public void updateRef(String columnLabel, Ref x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateRef is not supported");
  }

  @Override
  public void updateBlob(int columnIndex, Blob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBlob is not supported");
  }

  @Override
  public void updateBlob(String columnLabel, Blob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBlob is not supported");
  }

  @Override
  public void updateClob(int columnIndex, Clob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateClob is not supported");
  }

  @Override
  public void updateClob(String columnLabel, Clob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateClob is not supported");
  }

  @Override
  public void updateArray(int columnIndex, Array x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateArray is not supported");
  }

  @Override
  public void updateArray(String columnLabel, Array x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateArray is not supported");
  }

  @Override
  public RowId getRowId(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getRowId is not supported");
  }

  @Override
  public RowId getRowId(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException(" is not supported");
  }

  @Override
  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateRowId is not supported");
  }

  @Override
  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateRowId is not supported");
  }

  @Override
  public int getHoldability() throws SQLException {
    throw new SQLFeatureNotSupportedException("getHoldability is not supported");
  }

  @Override
  public boolean isClosed() throws SQLException {
    return closed;
  }

  @Override
  public void updateNString(int columnIndex, String nString) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNString is not supported");
  }

  @Override
  public void updateNString(String columnLabel, String nString) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNString is not supported");
  }

  @Override
  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNClob is not supported");
  }

  @Override
  public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNClob is not supported");
  }

  @Override
  public NClob getNClob(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getNClob is not supported");
  }

  @Override
  public NClob getNClob(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getNClob is not supported");
  }

  @Override
  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getSQLXML is not supported");
  }

  @Override
  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getSQLXML is not supported");
  }

  @Override
  public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateSQLXML is not supported");
  }

  @Override
  public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateSQLXML is not supported");
  }

  @Override
  public String getNString(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getNString is not supported");
  }

  @Override
  public String getNString(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getNString is not supported");
  }

  @Override
  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("getNCharacterStream is not supported");
  }

  @Override
  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("getNCharacterStream is not supported");
  }

  @Override
  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNCharacterStream is not supported");
  }

  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader, long length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNCharacterStream is not supported");
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateAsciiStream is not supported");
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBinaryStream is not supported");
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateCharacterStream is not supported");
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, long length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateAsciiStream is not supported");
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, long length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBinaryStream is not supported");
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, long length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateCharacterStream is not supported");
  }

  @Override
  public void updateBlob(int columnIndex, InputStream inputStream, long length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBlob is not supported");
  }

  @Override
  public void updateBlob(String columnLabel, InputStream inputStream, long length)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBlob is not supported");
  }

  @Override
  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateClob is not supported");
  }

  @Override
  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateClob is not supported");
  }

  @Override
  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException(" is not supported");
  }

  @Override
  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNClob is not supported");
  }

  @Override
  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNClob is not supported");
  }

  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNCharacterStream is not supported");
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateAsciiStream is not supported");
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBinaryStream is not supported");
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateCharacterStream is not supported");
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateAsciiStream is not supported");
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBinaryStream is not supported");
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateCharacterStream is not supported");
  }

  @Override
  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBlob is not supported");
  }

  @Override
  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateBlob is not supported");
  }

  @Override
  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateClob is not supported");
  }

  @Override
  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateClob is not supported");
  }

  @Override
  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNClob is not supported");
  }

  @Override
  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("updateNClob is not supported");
  }

  @Override
  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    throw new SQLFeatureNotSupportedException("getObject is not supported");
  }

  @Override
  public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    throw new SQLFeatureNotSupportedException(" is not supported");
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
