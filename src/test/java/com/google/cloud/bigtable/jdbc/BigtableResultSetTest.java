package com.google.cloud.bigtable.jdbc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import com.google.cloud.bigtable.data.v2.models.sql.ColumnMetadata;
import com.google.cloud.bigtable.data.v2.models.sql.ResultSet;
import com.google.cloud.bigtable.data.v2.models.sql.ResultSetMetadata;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

public class BigtableResultSetTest {
  private ResultSet mockedBigtableResultSet;
  private ResultSetMetadata mockedMetadata;
  private BigtableResultSet resultSet;

  @Before
  public void setUp() {
    mockedBigtableResultSet = mock(ResultSet.class);
    mockedMetadata = mock(ResultSetMetadata.class);
    when(mockedBigtableResultSet.getMetadata()).thenReturn(mockedMetadata);
    List<Map<String, Object>> rows = new ArrayList<>();
    resultSet = new BigtableResultSet(mockedBigtableResultSet, rows);
  }

  @Test
  public void testNextAndGetString() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
    when(mockedMetadata.getColumnIndex("col1")).thenReturn(0);
    when(mockedMetadata.getColumnIndex("col2")).thenReturn(1);

    // First Row
    when(mockedBigtableResultSet.getString(0)).thenReturn("value1");
    when(mockedBigtableResultSet.getString(1)).thenReturn("123");
    assertTrue(resultSet.next());
    assertEquals("value1", resultSet.getString("col1"));
    assertEquals("123", resultSet.getString("col2"));
    assertEquals("value1", resultSet.getString(1));
    assertEquals("123", resultSet.getString(2));

    // Second Row
    when(mockedBigtableResultSet.getString(0)).thenReturn("value2");
    when(mockedBigtableResultSet.getString(1)).thenReturn("456");
    assertTrue(resultSet.next());
    assertEquals("value2", resultSet.getString("col1"));
    assertEquals("456", resultSet.getString("col2"));
    assertEquals("value2", resultSet.getString(1));
    assertEquals("456", resultSet.getString(2));

    assertFalse(resultSet.next());
  }

  @Test
  public void testGettersWithNullValues() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true);
    when(mockedMetadata.getColumnIndex("col1")).thenReturn(0);
    when(mockedBigtableResultSet.isNull(0)).thenReturn(true);

    assertTrue(resultSet.next());

    assertNull(resultSet.getString("col1"));
    assertTrue(resultSet.wasNull());

    assertFalse(resultSet.getBoolean("col1"));
    assertTrue(resultSet.wasNull());

    assertEquals(0, resultSet.getByte("col1"));
    assertTrue(resultSet.wasNull());

    assertEquals(0, resultSet.getShort("col1"));
    assertTrue(resultSet.wasNull());

    assertEquals(0, resultSet.getInt("col1"));
    assertTrue(resultSet.wasNull());

    assertEquals(0L, resultSet.getLong("col1"));
    assertTrue(resultSet.wasNull());

    assertEquals(0.0f, resultSet.getFloat("col1"), 0.0);
    assertTrue(resultSet.wasNull());

    assertEquals(0.0, resultSet.getDouble("col1"), 0.0);
    assertTrue(resultSet.wasNull());

    assertNull(resultSet.getBytes("col1"));
    assertTrue(resultSet.wasNull());

    assertNull(resultSet.getTimestamp("col1"));
    assertTrue(resultSet.wasNull());

    assertNull(resultSet.getDate("col1"));
    assertTrue(resultSet.wasNull());
  }

  @Test
  public void testGettersWithNonNullValues() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true);
    when(mockedMetadata.getColumnIndex("stringCol")).thenReturn(0);
    when(mockedMetadata.getColumnIndex("boolCol")).thenReturn(1);
    when(mockedMetadata.getColumnIndex("byteCol")).thenReturn(2);
    when(mockedMetadata.getColumnIndex("shortCol")).thenReturn(3);
    when(mockedMetadata.getColumnIndex("intCol")).thenReturn(4);
    when(mockedMetadata.getColumnIndex("longCol")).thenReturn(5);
    when(mockedMetadata.getColumnIndex("floatCol")).thenReturn(6);
    when(mockedMetadata.getColumnIndex("doubleCol")).thenReturn(7);
    when(mockedMetadata.getColumnIndex("bytesCol")).thenReturn(8);
    when(mockedMetadata.getColumnIndex("timestampCol")).thenReturn(9);
    when(mockedMetadata.getColumnIndex("dateCol")).thenReturn(10);

    when(mockedBigtableResultSet.getString(0)).thenReturn("test");
    when(mockedBigtableResultSet.getBoolean(1)).thenReturn(true);
    when(mockedBigtableResultSet.getLong(2)).thenReturn(50L);
    when(mockedBigtableResultSet.getLong(3)).thenReturn(1000L);
    when(mockedBigtableResultSet.getLong(4)).thenReturn(100000L);
    when(mockedBigtableResultSet.getLong(5)).thenReturn(1000000000L);
    when(mockedBigtableResultSet.getFloat(6)).thenReturn(3.14f);
    when(mockedBigtableResultSet.getDouble(7)).thenReturn(3.14159);
    byte[] bytes = {1, 2, 3};
    when(mockedBigtableResultSet.getBytes(8)).thenReturn(ByteString.copyFrom(bytes));
    Instant now = Instant.now();
    when(mockedBigtableResultSet.getTimestamp(9)).thenReturn(now);
    when(mockedBigtableResultSet.getTimestamp(10)).thenReturn(now);

    assertTrue(resultSet.next());

    assertEquals("test", resultSet.getString("stringCol"));
    assertTrue(resultSet.getBoolean("boolCol"));
    assertEquals((byte) 50, resultSet.getByte("byteCol"));
    assertEquals((short) 1000, resultSet.getShort("shortCol"));
    assertEquals(100000, resultSet.getInt("intCol"));
    assertEquals(1000000000L, resultSet.getLong("longCol"));
    assertEquals(3.14f, resultSet.getFloat("floatCol"), 0.0);
    assertEquals(3.14159, resultSet.getDouble("doubleCol"), 0.0);
    assertArrayEquals(bytes, resultSet.getBytes("bytesCol"));
    assertEquals(Timestamp.from(now), resultSet.getTimestamp("timestampCol"));
    assertEquals(Date.valueOf(now.atZone(java.time.ZoneId.systemDefault()).toLocalDate()),
        resultSet.getDate("dateCol"));
  }

  @Test
  public void testGetObject() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true);
    ColumnMetadata stringColumn = mock(ColumnMetadata.class);
    when(stringColumn.name()).thenReturn("col1");
    when(stringColumn.type()).thenReturn((SqlType) SqlType.string());

    ColumnMetadata longColumn = mock(ColumnMetadata.class);
    when(longColumn.name()).thenReturn("col2");
    when(longColumn.type()).thenReturn((SqlType) SqlType.int64());

    when(mockedMetadata.getColumns()).thenReturn(ImmutableList.of(stringColumn, longColumn));
    when(mockedBigtableResultSet.getString("col1")).thenReturn("value1");
    when(mockedBigtableResultSet.getLong("col2")).thenReturn(123L);

    assertTrue(resultSet.next());
    assertEquals("value1", resultSet.getObject(1));
    assertEquals(123L, resultSet.getObject(2));
    assertFalse(resultSet.wasNull());
  }

  @Test
  public void testGetObjectWithNull() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true);
    ColumnMetadata mockedColumnMetadata = mock(ColumnMetadata.class);
    when(mockedColumnMetadata.name()).thenReturn("col1");
    when(mockedColumnMetadata.type()).thenReturn((SqlType) SqlType.string());
    when(mockedMetadata.getColumns()).thenReturn(ImmutableList.of(mockedColumnMetadata));
    when(mockedBigtableResultSet.isNull("col1")).thenReturn(true);

    assertTrue(resultSet.next());
    assertNull(resultSet.getObject(1));
    assertTrue(resultSet.wasNull());
  }

  @Test(expected = SQLException.class)
  public void testGetObjectWithInvalidIndex() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true);
    when(mockedMetadata.getColumns()).thenReturn(ImmutableList.of());
    assertTrue(resultSet.next());
    resultSet.getObject(1);
  }

  @Test(expected = SQLException.class)
  public void testGetObjectArrayTypeMismatchThrowsException() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true);

    SqlType<?> mockedSqlType = mock(SqlType.class);
    when(mockedSqlType.getCode()).thenReturn(SqlType.Code.ARRAY);

    ColumnMetadata arrayColumn = mock(ColumnMetadata.class);
    when(arrayColumn.name()).thenReturn("arrayCol");
    when(arrayColumn.type()).thenReturn((SqlType) mockedSqlType);

    when(mockedMetadata.getColumns()).thenReturn(ImmutableList.of(arrayColumn));

    assertTrue(resultSet.next());
    resultSet.getObject(1);
  }

  @Test(expected = SQLException.class)
  public void testGetObjectMapTypeMismatchThrowsException() throws SQLException {
    when(mockedBigtableResultSet.next()).thenReturn(true);

    SqlType<?> mockedSqlType = mock(SqlType.class);
    when(mockedSqlType.getCode()).thenReturn(SqlType.Code.MAP);

    ColumnMetadata mapColumn = mock(ColumnMetadata.class);
    when(mapColumn.name()).thenReturn("mapCol");
    when(mapColumn.type()).thenReturn((SqlType) mockedSqlType);

    when(mockedMetadata.getColumns()).thenReturn(ImmutableList.of(mapColumn));

    assertTrue(resultSet.next());
    resultSet.getObject(1);
  }

  @Test
  public void testClose() throws Exception {
    resultSet.close();
    verify(mockedBigtableResultSet).close();
  }

  @Test(expected = SQLException.class)
  public void testThrowsExceptionIfClosed() throws SQLException {
    resultSet.close();
    resultSet.next();
  }

  @Test
  public void testIsBeforeFirst() throws SQLException {
    assertTrue(resultSet.isBeforeFirst());
    when(mockedBigtableResultSet.next()).thenReturn(true);
    resultSet.next();
    assertFalse(resultSet.isBeforeFirst());
  }

  @Test
  public void testIsAfterLast() throws SQLException {
    assertFalse(resultSet.isAfterLast());
    when(mockedBigtableResultSet.next()).thenReturn(false);
    resultSet.next();
    assertTrue(resultSet.isAfterLast());
  }

  @Test
  public void testFindColumn() throws SQLException {
    when(mockedMetadata.getColumnIndex("col1")).thenReturn(0);
    assertEquals(1, resultSet.findColumn("col1"));
  }

  @Test(expected = SQLException.class)
  public void testFindColumnThrowsException() throws SQLException {
    when(mockedMetadata.getColumnIndex("invalid"))
        .thenThrow(new RuntimeException("Column not found"));
    resultSet.findColumn("invalid");
  }

  @Test
  public void testGetMetaData() throws SQLException {
    assertNotNull(resultSet.getMetaData());
  }

  @Test
  public void testGetWarnings() throws SQLException {
    assertNull(resultSet.getWarnings());
  }

  @Test
  public void testClearWarnings() throws SQLException {
    resultSet.clearWarnings();
    assertNull(resultSet.getWarnings());
  }

  @Test
  public void testIsClosed() throws SQLException {
    assertFalse(resultSet.isClosed());
    resultSet.close();
    assertTrue(resultSet.isClosed());
  }

  @Test
  public void testGetDateWithCalendar() throws SQLException {
    assertNull(resultSet.getDate(1, Calendar.getInstance()));
    assertNull(resultSet.getDate("col1", Calendar.getInstance()));
  }

  @Test
  public void testGetTimestampWithCalendar() throws SQLException {
    assertNull(resultSet.getTimestamp(1, Calendar.getInstance()));
    assertNull(resultSet.getTimestamp("col1", Calendar.getInstance()));
  }

  @Test
  public void testUnsupportedFeatures() {
    assertUnsupported(() -> resultSet.getBigDecimal(1, 1));
    assertUnsupported(() -> resultSet.getTime(1));
    assertUnsupported(() -> resultSet.getAsciiStream(1));
    assertUnsupported(() -> resultSet.getUnicodeStream(1));
    assertUnsupported(() -> resultSet.getBinaryStream(1));
    assertUnsupported(() -> resultSet.getCursorName());
    assertUnsupported(() -> resultSet.isFirst());
    assertUnsupported(() -> resultSet.isLast());
    assertUnsupported(() -> resultSet.beforeFirst());
    assertUnsupported(() -> resultSet.afterLast());
    assertUnsupported(() -> resultSet.first());
    assertUnsupported(() -> resultSet.last());
    assertUnsupported(() -> resultSet.absolute(1));
    assertUnsupported(() -> resultSet.relative(1));
    assertUnsupported(() -> resultSet.previous());
    assertUnsupported(() -> resultSet.setFetchDirection(1));
    assertUnsupported(() -> resultSet.getFetchDirection());
    assertUnsupported(() -> resultSet.setFetchSize(1));
    assertUnsupported(() -> resultSet.getFetchSize());
    assertUnsupported(() -> resultSet.getType());
    assertUnsupported(() -> resultSet.getConcurrency());
    assertUnsupported(() -> resultSet.rowUpdated());
    assertUnsupported(() -> resultSet.rowInserted());
    assertUnsupported(() -> resultSet.rowDeleted());
    assertUnsupported(() -> resultSet.updateNull(1));
    assertUnsupported(() -> resultSet.updateBoolean(1, false));
    assertUnsupported(() -> resultSet.updateByte(1, (byte) 0));
    assertUnsupported(() -> resultSet.updateShort(1, (short) 0));
    assertUnsupported(() -> resultSet.updateInt(1, 0));
    assertUnsupported(() -> resultSet.updateLong(1, 0L));
    assertUnsupported(() -> resultSet.updateFloat(1, 0f));
    assertUnsupported(() -> resultSet.updateDouble(1, 0.0));
    assertUnsupported(() -> resultSet.updateBigDecimal(1, null));
    assertUnsupported(() -> resultSet.updateString(1, null));
    assertUnsupported(() -> resultSet.updateBytes(1, null));
    assertUnsupported(() -> resultSet.updateDate(1, null));
    assertUnsupported(() -> resultSet.updateTime(1, null));
    assertUnsupported(() -> resultSet.updateTimestamp(1, null));
    assertUnsupported(() -> resultSet.updateAsciiStream(1, null, 0));
    assertUnsupported(() -> resultSet.updateBinaryStream(1, null, 0));
    assertUnsupported(() -> resultSet.updateCharacterStream(1, null, 0));
    assertUnsupported(() -> resultSet.updateObject(1, null, 0));
    assertUnsupported(() -> resultSet.updateObject(1, null));
    assertUnsupported(() -> resultSet.insertRow());
    assertUnsupported(() -> resultSet.updateRow());
    assertUnsupported(() -> resultSet.deleteRow());
    assertUnsupported(() -> resultSet.refreshRow());
    assertUnsupported(() -> resultSet.cancelRowUpdates());
    assertUnsupported(() -> resultSet.moveToInsertRow());
    assertUnsupported(() -> resultSet.moveToCurrentRow());
    assertUnsupported(() -> resultSet.getStatement());
    assertUnsupported(() -> resultSet.getObject(1, (Map<String, Class<?>>) null));
    assertUnsupported(() -> resultSet.getRef(1));
    assertUnsupported(() -> resultSet.getBlob(1));
    assertUnsupported(() -> resultSet.getClob(1));
    assertUnsupported(() -> resultSet.getArray(1));
    assertUnsupported(() -> resultSet.getURL(1));
    assertUnsupported(() -> resultSet.getRowId(1));
    assertUnsupported(() -> resultSet.getNClob(1));
    assertUnsupported(() -> resultSet.getSQLXML(1));
    assertUnsupported(() -> resultSet.getNString(1));
    assertUnsupported(() -> resultSet.getNCharacterStream(1));
  }

  private void assertUnsupported(SQLRunnable runnable) {
    try {
      runnable.run();
      fail("Expected SQLFeatureNotSupportedException");
    } catch (SQLFeatureNotSupportedException e) {
      // expected
    } catch (SQLException e) {
      fail("Expected SQLFeatureNotSupportedException, but got " + e.getClass().getName());
    }
  }

  @FunctionalInterface
  interface SQLRunnable {
    void run() throws SQLException;
  }
}
