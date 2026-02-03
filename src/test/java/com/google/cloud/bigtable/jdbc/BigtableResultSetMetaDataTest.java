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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.bigtable.data.v2.models.sql.ColumnMetadata;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BigtableResultSetMetaDataTest {

  private BigtableResultSetMetaData resultSetMetaData;
  private List<ColumnMetadata> columns;

  @Before
  public void setUp() {
    columns = new ArrayList<>();
    
    ColumnMetadata colString = mock(ColumnMetadata.class);
    when(colString.name()).thenReturn("col_string");
    when(colString.type()).thenReturn((SqlType)SqlType.string());
    columns.add(colString);

    ColumnMetadata colLong = mock(ColumnMetadata.class);
    when(colLong.name()).thenReturn("col_long");
    when(colLong.type()).thenReturn((SqlType)SqlType.int64());
    columns.add(colLong);

    ColumnMetadata colDouble = mock(ColumnMetadata.class);
    when(colDouble.name()).thenReturn("col_double");
    when(colDouble.type()).thenReturn((SqlType)SqlType.float64());
    columns.add(colDouble);

    ColumnMetadata colBytes = mock(ColumnMetadata.class);
    when(colBytes.name()).thenReturn("col_bytes");
    when(colBytes.type()).thenReturn((SqlType)SqlType.bytes());
    columns.add(colBytes);

    ColumnMetadata colBool = mock(ColumnMetadata.class);
    when(colBool.name()).thenReturn("col_bool");
    when(colBool.type()).thenReturn((SqlType)SqlType.bool());
    columns.add(colBool);

    ColumnMetadata colTimestamp = mock(ColumnMetadata.class);
    when(colTimestamp.name()).thenReturn("col_timestamp");
    when(colTimestamp.type()).thenReturn((SqlType)SqlType.timestamp());
    columns.add(colTimestamp);

    ColumnMetadata colDate = mock(ColumnMetadata.class);
    when(colDate.name()).thenReturn("col_date");
    when(colDate.type()).thenReturn((SqlType)SqlType.date());
    columns.add(colDate);

    resultSetMetaData = new BigtableResultSetMetaData(columns);
  }

  @Test
  public void testGetColumnCount() throws SQLException {
    assertEquals(7, resultSetMetaData.getColumnCount());
  }

  @Test
  public void testIsAutoIncrement() throws SQLException {
    assertFalse(resultSetMetaData.isAutoIncrement(1));
  }

  @Test
  public void testIsCaseSensitive() throws SQLException {
    assertTrue(resultSetMetaData.isCaseSensitive(1)); // String
    assertFalse(resultSetMetaData.isCaseSensitive(2)); // Long
  }

  @Test(expected = SQLException.class)
  public void testIsCaseSensitiveInvalidColumn() throws SQLException {
    resultSetMetaData.isCaseSensitive(10);
  }

  @Test
  public void testIsSearchable() throws SQLException {
    assertTrue(resultSetMetaData.isSearchable(1));
  }

  @Test
  public void testIsCurrency() throws SQLException {
    assertFalse(resultSetMetaData.isCurrency(1));
  }

  @Test
  public void testIsNullable() throws SQLException {
    assertEquals(ResultSetMetaData.columnNullableUnknown, resultSetMetaData.isNullable(1));
  }

  @Test
  public void testIsSigned() throws SQLException {
    assertFalse(resultSetMetaData.isSigned(1)); // String
    assertTrue(resultSetMetaData.isSigned(2)); // Long
    assertTrue(resultSetMetaData.isSigned(3)); // Double
  }

  @Test(expected = SQLException.class)
  public void testIsSignedInvalidColumn() throws SQLException {
    resultSetMetaData.isSigned(10);
  }

  @Test
  public void testGetColumnDisplaySize() throws SQLException {
    assertEquals(10, resultSetMetaData.getColumnDisplaySize(1)); // String
    assertEquals(10, resultSetMetaData.getColumnDisplaySize(2)); // Long
    assertEquals(7, resultSetMetaData.getColumnDisplaySize(3)); // Double/Real
    assertEquals(10, resultSetMetaData.getColumnDisplaySize(4)); // Bytes
    assertEquals(5, resultSetMetaData.getColumnDisplaySize(5)); // Bool
    assertEquals(16, resultSetMetaData.getColumnDisplaySize(6)); // Timestamp
    assertEquals(10, resultSetMetaData.getColumnDisplaySize(7)); // Date
  }

  @Test
  public void testGetColumnLabel() throws SQLException {
    assertEquals("col_string", resultSetMetaData.getColumnLabel(1));
  }

  @Test
  public void testGetColumnName() throws SQLException {
    assertEquals("col_string", resultSetMetaData.getColumnName(1));
  }

  @Test
  public void testGetSchemaName() throws SQLException {
    assertEquals("", resultSetMetaData.getSchemaName(1));
  }

  @Test
  public void testGetPrecision() throws SQLException {
    assertEquals(50, resultSetMetaData.getPrecision(1)); // String
    assertEquals(10, resultSetMetaData.getPrecision(2)); // Long
    assertEquals(7, resultSetMetaData.getPrecision(3)); // Double/Real
    assertEquals(50, resultSetMetaData.getPrecision(4)); // Bytes
    assertEquals(1, resultSetMetaData.getPrecision(5)); // Bool
    assertEquals(24, resultSetMetaData.getPrecision(6)); // Timestamp
    assertEquals(10, resultSetMetaData.getPrecision(7)); // Date
  }

  @Test
  public void testGetScale() throws SQLException {
    assertEquals(0, resultSetMetaData.getScale(1));
    assertEquals(7, resultSetMetaData.getScale(3)); // Double/Real
  }

  @Test
  public void testGetTableName() throws SQLException {
    assertEquals("", resultSetMetaData.getTableName(1));
  }

  @Test
  public void testGetCatalogName() throws SQLException {
    assertEquals("", resultSetMetaData.getCatalogName(1));
  }

  @Test
  public void testGetColumnType() throws SQLException {
    assertEquals(Types.VARCHAR, resultSetMetaData.getColumnType(1));
    assertEquals(Types.BIGINT, resultSetMetaData.getColumnType(2));
    assertEquals(Types.REAL, resultSetMetaData.getColumnType(3));
    assertEquals(Types.LONGVARBINARY, resultSetMetaData.getColumnType(4));
    assertEquals(Types.BOOLEAN, resultSetMetaData.getColumnType(5));
    assertEquals(Types.TIMESTAMP, resultSetMetaData.getColumnType(6));
    assertEquals(Types.DATE, resultSetMetaData.getColumnType(7));
  }

  @Test
  public void testGetColumnTypeName() throws SQLException {
    assertEquals("STRING", resultSetMetaData.getColumnTypeName(1));
    assertEquals("INT64", resultSetMetaData.getColumnTypeName(2));
    assertEquals("FLOAT64", resultSetMetaData.getColumnTypeName(3));
    assertEquals("BYTES", resultSetMetaData.getColumnTypeName(4));
    assertEquals("BOOL", resultSetMetaData.getColumnTypeName(5));
    assertEquals("TIMESTAMP", resultSetMetaData.getColumnTypeName(6));
    assertEquals("DATE", resultSetMetaData.getColumnTypeName(7));
  }

  @Test
  public void testIsReadOnly() throws SQLException {
    assertTrue(resultSetMetaData.isReadOnly(1));
  }

  @Test
  public void testIsWritable() throws SQLException {
    assertFalse(resultSetMetaData.isWritable(1));
  }

  @Test
  public void testIsDefinitelyWritable() throws SQLException {
    assertFalse(resultSetMetaData.isDefinitelyWritable(1));
  }

  @Test
  public void testGetColumnClassName() throws SQLException {
    assertEquals("java.lang.String", resultSetMetaData.getColumnClassName(1));
    assertEquals("java.lang.Long", resultSetMetaData.getColumnClassName(2));
    assertEquals("java.lang.Double", resultSetMetaData.getColumnClassName(3));
    assertEquals("com.google.protobuf.ByteString", resultSetMetaData.getColumnClassName(4));
    assertEquals("java.lang.Boolean", resultSetMetaData.getColumnClassName(5));
    assertEquals("java.time.Instant", resultSetMetaData.getColumnClassName(6));
    assertEquals("com.google.cloud.Date", resultSetMetaData.getColumnClassName(7));
  }

  @Test(expected = SQLFeatureNotSupportedException.class)
  public void testUnwrap() throws SQLException {
    resultSetMetaData.unwrap(Object.class);
  }

  @Test(expected = SQLFeatureNotSupportedException.class)
  public void testIsWrapperFor() throws SQLException {
    resultSetMetaData.isWrapperFor(Object.class);
  }
}
