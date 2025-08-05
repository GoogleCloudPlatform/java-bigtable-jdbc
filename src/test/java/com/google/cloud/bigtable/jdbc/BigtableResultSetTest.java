package com.google.cloud.bigtable.jdbc;

import org.junit.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class BigtableResultSetTest {

  @Test
  public void testNextAndGetString() throws SQLException {
    Map<String, Object> row = new HashMap<>();
    row.put("name", "Alice");

    List<Map<String, Object>> rows = Collections.singletonList(row);
    BigtableResultSet resultSet = new BigtableResultSet(rows);

    assertTrue(resultSet.next());
    assertEquals("Alice", resultSet.getString("name"));
    assertFalse(resultSet.next());
  }

  @Test
  public void testGetIntAndGetBoolean() throws SQLException {
    Map<String, Object> row = new HashMap<>();
    row.put("age", 42);
    row.put("active", true);

    BigtableResultSet resultSet = new BigtableResultSet(Collections.singletonList(row));

    assertTrue(resultSet.next());
    assertEquals(42, resultSet.getInt("age"));
    assertTrue(resultSet.getBoolean("active"));
  }

  @Test
  public void testIsClosedFlag() throws SQLException {
    BigtableResultSet resultSet = new BigtableResultSet(Collections.emptyList());
    assertFalse(resultSet.isClosed());
    resultSet.close();
    assertTrue(resultSet.isClosed());
  }
}
