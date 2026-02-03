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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.sql.BoundStatement;
import com.google.cloud.bigtable.data.v2.models.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BigtablePreparedStatementTest {

  private static final String SQL = "SELECT * FROM table WHERE id = ?";
  @Mock private BigtableDataClient mockDataClient;
  @Mock private BigtableConnection mockConnection;

  @Mock
  private com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement mockPreparedStatement;

  @Mock private BoundStatement.Builder mockBoundStatementBuilder;
  @Mock private BoundStatement mockBoundStatement;
  @Mock private ResultSet mockResultSet;

  private AutoCloseable closeable;

  @Before
  public void openMocks() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  private BigtablePreparedStatement createStatement() {
    return new BigtablePreparedStatement(mockConnection, SQL, mockDataClient);
  }

  @Test
  public void testExecuteQuery() throws SQLException {
    when(mockDataClient.prepareStatement(any(), any())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.bind()).thenReturn(mockBoundStatementBuilder);
    when(mockBoundStatementBuilder.build()).thenReturn(mockBoundStatement);
    when(mockDataClient.executeQuery(mockBoundStatement)).thenReturn(mockResultSet);

    PreparedStatement statement = createStatement();
    statement.setLong(1, 123L);
    java.sql.ResultSet resultSet = statement.executeQuery();
    assertNotNull(resultSet);
  }

  @Test
  public void testSetString() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setString(1, "test");
  }

  @Test
  public void testSetByte() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setByte(1, (byte) 1);
  }

  @Test
  public void testSetBoolean() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setBoolean(1, true);
  }

  @Test
  public void testSetDouble() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setDouble(1, 1.23);
  }

  @Test
  public void testSetDate() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setDate(1, new java.sql.Date(new Date().getTime()));
  }

  @Test
  public void testSetTimestamp() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setTimestamp(1, new Timestamp(new Date().getTime()));
  }

  @Test
  public void testSetBytes() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setBytes(1, "test".getBytes());
  }

  @Test
  public void testClearParameters() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setString(1, "test");
    statement.clearParameters();
  }

  @Test
  public void testExecute() throws SQLException {
    when(mockDataClient.prepareStatement(any(), any())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.bind()).thenReturn(mockBoundStatementBuilder);
    when(mockBoundStatementBuilder.build()).thenReturn(mockBoundStatement);
    when(mockDataClient.executeQuery(mockBoundStatement)).thenReturn(mockResultSet);

    PreparedStatement statement = createStatement();
    statement.setLong(1, 123L);
    statement.execute();
    assertNotNull(statement.getResultSet());
  }

  @Test
  public void testSetNString() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setNString(1, "test");
  }

  @Test
  public void testSetNullWithTypeName() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setNull(1, java.sql.Types.VARCHAR, "VARCHAR");
  }

  @Test
  public void testSetNullWithTypeNameUnsupported() {
    assertThrows(
        SQLException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setNull(1, java.sql.Types.ARRAY, "ARRAY");
        });
  }

  @Test
  public void testGetParameterMetaData() throws SQLException {
    BigtablePreparedStatement statement = createStatement();
    statement.setString(1, "test");
    statement.setLong(2, 123L);
    java.sql.ParameterMetaData metaData = statement.getParameterMetaData();
    assertNotNull(metaData);
    assertEquals(2, metaData.getParameterCount());
    assertEquals(java.sql.Types.VARCHAR, metaData.getParameterType(1));
    assertEquals(java.sql.Types.BIGINT, metaData.getParameterType(2));
  }

  @Test
  public void testUnsupportedFeatures() {
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.executeUpdate();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setObject(1, new Object());
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setCharacterStream(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setRef(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setBlob(1, (java.sql.Blob) null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setClob(1, (java.sql.Clob) null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setURL(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setRowId(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setNCharacterStream(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setNClob(1, (java.sql.NClob) null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setSQLXML(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setTime(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setTime(1, null, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setAsciiStream(1, null);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setUnicodeStream(1, null, 0);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setBinaryStream(1, null);
        });
  }

  @Test
  public void testSetNull() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setNull(1, java.sql.Types.VARCHAR);
  }

  @Test
  public void testSetArray() throws SQLException {
    PreparedStatement statement = createStatement();
    java.sql.Array mockArray = org.mockito.Mockito.mock(java.sql.Array.class);
    when(mockArray.getBaseType()).thenReturn(java.sql.Types.VARCHAR);
    try {
      when(mockArray.getArray()).thenReturn(new String[] {"a", "b"});
    } catch (SQLException e) {
      // This should not happen in a mock
    }
    statement.setArray(1, mockArray);
  }

  @Test
  public void testSetDateWithCalendar() throws SQLException {
    PreparedStatement statement = createStatement();
    java.util.Calendar cal = java.util.Calendar.getInstance();
    statement.setDate(1, new java.sql.Date(new Date().getTime()), cal);
  }

  @Test
  public void testSetTimestampWithCalendar() throws SQLException {
    PreparedStatement statement = createStatement();
    java.util.Calendar cal = java.util.Calendar.getInstance();
    statement.setTimestamp(1, new Timestamp(new Date().getTime()), cal);
  }

  @Test
  public void testExecuteWithNoSql() {
    assertThrows(
        SQLException.class,
        () -> {
          BigtablePreparedStatement statement =
              new BigtablePreparedStatement(mockConnection, null, mockDataClient);
          statement.execute();
        });
  }

  @Test
  public void testClosedStatement() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.close();

    assertThrows(SQLException.class, () -> statement.executeQuery());
    assertThrows(SQLException.class, () -> statement.setLong(1, 123L));
    assertThrows(SQLException.class, () -> statement.setString(1, "test"));
    assertThrows(SQLException.class, () -> statement.setNull(1, java.sql.Types.VARCHAR));
    assertThrows(SQLException.class, () -> statement.setArray(1, null));
    assertThrows(SQLException.class, () -> statement.setDate(1, null, null));
    assertThrows(SQLException.class, () -> statement.setTimestamp(1, null, null));
    assertThrows(SQLException.class, () -> statement.clearParameters());
  }

  @Test
  public void testSetFloat() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setFloat(1, 1.23f);
  }

  @Test
  public void testSetInt() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setInt(1, 123);
  }

  @Test
  public void testSetShort() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setShort(1, (short) 123);
  }

  @Test
  public void testSetBigDecimal() {
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setBigDecimal(1, new java.math.BigDecimal("123.45"));
        });
  }

  @Test
  public void testSetParameterOnClosedStatement() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.close();
    assertThrows(SQLException.class, () -> statement.setString(1, "test"));
  }

  @Test
  public void testSetLargeMaxRows() {
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setLargeMaxRows(100L);
        });
  }

  @Test
  public void testSetQueryTimeout() {
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setQueryTimeout(100);
        });
  }

  @Test
  public void testGetParameterMetaData_Detailed() throws SQLException {
    BigtablePreparedStatement statement = createStatement();
    statement.setString(1, "test");
    statement.setLong(2, 123L);
    statement.setDouble(3, 1.23);
    statement.setBoolean(4, true);
    statement.setBytes(5, new byte[0]);
    statement.setDate(6, new java.sql.Date(0));
    statement.setTimestamp(7, new Timestamp(0));
    statement.setFloat(8, 1.23f);

    java.sql.ParameterMetaData metaData = statement.getParameterMetaData();
    assertEquals(8, metaData.getParameterCount());

    // Test type names and classes
    assertEquals("STRING", metaData.getParameterTypeName(1));
    assertEquals(String.class.getName(), metaData.getParameterClassName(1));
    assertEquals("INT64", metaData.getParameterTypeName(2));
    assertEquals(Long.class.getName(), metaData.getParameterClassName(2));
    assertEquals("FLOAT64", metaData.getParameterTypeName(3));
    assertEquals(Double.class.getName(), metaData.getParameterClassName(3));
    assertEquals("FLOAT32", metaData.getParameterTypeName(8));
    assertEquals(Float.class.getName(), metaData.getParameterClassName(8));

    // Test precision and scale
    assertEquals(Integer.MAX_VALUE, metaData.getPrecision(1));
    assertEquals(10, metaData.getPrecision(2));
    assertEquals(53, metaData.getPrecision(3));
    assertEquals(15, metaData.getScale(3));
    assertEquals(24, metaData.getPrecision(8));
    assertEquals(7, metaData.getScale(8));

    // Test signed
    assertEquals(false, metaData.isSigned(1));
    assertEquals(true, metaData.isSigned(2));
    assertEquals(true, metaData.isSigned(3));
    assertEquals(true, metaData.isSigned(8));

    // Test nullable and mode
    assertEquals(java.sql.ParameterMetaData.parameterNullable, metaData.isNullable(1));
    assertEquals(java.sql.ParameterMetaData.parameterModeIn, metaData.getParameterMode(1));

    // Test invalid parameter index
    assertThrows(SQLException.class, () -> metaData.getParameterType(9));
  }

  @Test
  public void testSetNullUnsupportedType() {
    assertThrows(
        SQLException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setNull(1, java.sql.Types.ARRAY);
        });
  }

  @Test
  public void testSetDateWithNullCalendar() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setDate(1, new java.sql.Date(new Date().getTime()), null);
  }

  @Test
  public void testSetTimestampWithNullCalendar() throws SQLException {
    PreparedStatement statement = createStatement();
    statement.setTimestamp(1, new Timestamp(new Date().getTime()), null);
  }

  @Test
  public void testSetArrayNull() {
    assertThrows(
        SQLException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setArray(1, null);
        });
  }

  @Test
  public void testExecuteWithEmptySql() {
    assertThrows(
        SQLException.class,
        () -> {
          BigtablePreparedStatement statement =
              new BigtablePreparedStatement(mockConnection, " ", mockDataClient);
          statement.execute();
        });
  }

  @Test
  public void testExecuteQueryFails() {
    when(mockDataClient.prepareStatement(any(), any())).thenThrow(new RuntimeException("test"));
    assertThrows(
        SQLException.class,
        () -> {
          PreparedStatement statement = createStatement();
          statement.setLong(1, 123L);
          statement.executeQuery();
        });
  }

  @Test
  public void testChangeParameterTypeOnCachedStatement() throws SQLException {
    when(mockDataClient.prepareStatement(any(), any())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.bind()).thenReturn(mockBoundStatementBuilder);
    when(mockBoundStatementBuilder.build()).thenReturn(mockBoundStatement);
    when(mockDataClient.executeQuery(mockBoundStatement)).thenReturn(mockResultSet);

    PreparedStatement statement = createStatement();
    statement.setLong(1, 123L);
    statement.executeQuery(); // Caches the statement

    assertThrows(
        SQLException.class,
        () -> {
          statement.setString(1, "new value");
        });
  }
}
