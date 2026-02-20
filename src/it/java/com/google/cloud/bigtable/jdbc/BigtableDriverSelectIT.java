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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BigtableDriverSelectIT {

  private static String getProperty(String key, String defaultValue) {
    String value = System.getProperty(key);
    return (value == null || value.isEmpty()) ? defaultValue : value;
  }

  private static final String PROJECT = getProperty("google.bigtable.project.id", "fakeProject");
  private static final String INSTANCE = getProperty("google.bigtable.instance.id", "fakeInstance");
  private static final String TABLE = getProperty("google.bigtable.table.id", "hotels");
  /*
   * The test instance in CI constains 1 table: `hotels`, consists of the following column families: `booking_info`, `hotel_info`.
   * The `hotel_info` contains ["id", "name", "location", "price_tier"] columns
   * The `booking_info` contains ["id", "checkin_date", "checkout_date", "booked"] columns.
   * We also have 2 logical views: `hotels_view` and `bookings_view`
   *  - `hotel_view` contains all columns in `hotel_info` column family
   *  - `booking_view` contains all columns in `booking_info` column family and hotel_id.
   */

  static final long NOW = 5_000_000_000L;
  static final long LATER = NOW + 1_000L;

  @Test
  public void testValidConnection() throws Exception {
    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);

    try (Connection connection = DriverManager.getConnection(url)) {
      assertTrue(connection.isValid(0));
    }
  }

  @Test
  public void testInvalidConnection() throws Exception {
    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    String url =
        String.format("jdbc:bigtable:/projects/%s/instances/%s", "fakeProject", "fakeInstance");
    assertThrows(java.sql.SQLException.class, () -> DriverManager.getConnection(url));
  }

  @Test
  public void testBasicSelectRowKeyStatement() throws Exception {
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    String select = String.format("SELECT * FROM `%s` WHERE _key = ?", TABLE);

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setBytes(1, "hotels#1#Basel#Hilton Basel#Luxury".getBytes());
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("hotels#1#Basel#Hilton Basel#Luxury", new String(rs.getBytes("_key")));
      }
    }
  }

  @Test
  public void testBasicSelectStatement() throws Exception {
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    String select = String.format("SELECT * FROM `%s` WHERE hotel_info['id'] = ?", TABLE);

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setBytes(1, "1".getBytes());
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("hotels#1#Basel#Hilton Basel#Luxury", new String(rs.getBytes("_key")));
      }
    }
  }

  @Test
  public void testSelectWithMultipleParams() throws Exception {
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    String select =
        String.format(
            "SELECT * FROM `%s` WHERE hotel_info['name'] = ? AND hotel_info['location'] = ?",
            TABLE);

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setBytes(1, "Hilton Basel".getBytes());
      statement.setBytes(2, "Basel".getBytes());
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("hotels#1#Basel#Hilton Basel#Luxury", new String(rs.getBytes("_key")));
      }
    }
  }

  @Test
  public void testSelectLogicalView() throws Exception {
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    String select = String.format("SELECT * FROM `%s` WHERE id = ?", "hotel_view");

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setInt(1, 1);
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("Hilton Basel", new String(rs.getBytes("hotel_name")));
      }
    }
  }

  @Test
  public void testSelectDateLogicalView() throws Exception {
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    String select = String.format("SELECT * FROM `%s` WHERE id = ?", "booking_view");

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setInt(1, 1);
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("2024-04-20", rs.getDate("checkin_date").toString());
      }
    }
  }

  @Test
  public void testSelectTimestampLogicalView() throws Exception {
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    String select = String.format("SELECT * FROM `%s` WHERE id = ?", "booking_view");

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setInt(1, 1);
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("2024-04-20 00:00:00.0", rs.getTimestamp("checkin_date_time").toString());
      }
    }
  }

  @Test
  public void testSelectBooleanLogicalView() throws Exception {
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    String select = String.format("SELECT * FROM `%s` WHERE id = ?", "booking_view");

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setInt(1, 1);
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(false, rs.getBoolean("booked"));
      }
    }
  }
}
