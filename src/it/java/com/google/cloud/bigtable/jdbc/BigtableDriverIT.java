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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.bigtable.emulator.v2.BigtableEmulatorRule;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BigtableDriverIT {

  @ClassRule
  public static final BigtableEmulatorRule BIGTABLE_EMULATOR = BigtableEmulatorRule.create();

  private static BigtableEmulatorClientWrapper emulatorWrapper;

  private static final String PROJECT =
      System.getProperty("google.bigtable.project.id", "fakeProject");
  private static final String INSTANCE =
      System.getProperty("google.bigtable.instance.id", "fakeInstance");
  private static final boolean USE_EMULATOR =
      System.getProperty("google.bigtable.project.id") == null
          || System.getProperty("google.bigtable.project.id").isEmpty();

  static final String KEY1 = "key1";
  static final String KEY2 = "key2";
  static final String BOOL_COLUMN = "boolColumn";
  static final String LONG_COLUMN = "longColumn";
  static final String STRING_COLUMN = "stringColumn";
  static final String DOUBLE_COLUMN = "doubleColumn";
  static final String FAMILY_TEST = "familyTest";

  static final long NOW = 5_000_000_000L;
  static final long LATER = NOW + 1_000L;

  private static byte[] booleanToByteArray(boolean bool) {
    return ByteBuffer.allocate(1).put(bool ? (byte) 1 : (byte) 0).array();
  }

  private static byte[] longToByteArray(long l) {
    return ByteBuffer.allocate(Long.BYTES).putLong(l).array();
  }

  private static byte[] doubleToByteArray(double d) {
    return ByteBuffer.allocate(Double.BYTES).putDouble(d).array();
  }

  private static void writeRow(String key, String table) {
    emulatorWrapper.writeRow(key, table, FAMILY_TEST, BOOL_COLUMN, booleanToByteArray(true), NOW);
    emulatorWrapper.writeRow(
        key, table, FAMILY_TEST, BOOL_COLUMN, booleanToByteArray(false), LATER);
    emulatorWrapper.writeRow(
        key, table, FAMILY_TEST, STRING_COLUMN, "string1".getBytes(UTF_8), NOW);
    emulatorWrapper.writeRow(
        key, table, FAMILY_TEST, STRING_COLUMN, "string2".getBytes(UTF_8), LATER);
    emulatorWrapper.writeRow(key, table, FAMILY_TEST, LONG_COLUMN, longToByteArray(1L), NOW);
    emulatorWrapper.writeRow(key, table, FAMILY_TEST, LONG_COLUMN, longToByteArray(2L), LATER);
    emulatorWrapper.writeRow(key, table, FAMILY_TEST, DOUBLE_COLUMN, doubleToByteArray(1.10), NOW);
    emulatorWrapper.writeRow(
        key, table, FAMILY_TEST, DOUBLE_COLUMN, doubleToByteArray(2.20), LATER);
  }

  @BeforeClass
  public static void setUp() throws Exception {
    Integer port = USE_EMULATOR ? BIGTABLE_EMULATOR.getPort() : null;
    emulatorWrapper = new BigtableEmulatorClientWrapper(PROJECT, INSTANCE, port, null);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    emulatorWrapper.closeSession();
  }

  private String getJdbcUrl() {
    if (USE_EMULATOR) {
      return String.format(
          "jdbc:bigtable://localhost:%d/projects/%s/instances/%s",
          BIGTABLE_EMULATOR.getPort(), PROJECT, INSTANCE);
    } else {
      return String.format("jdbc:bigtable:/projects/%s/instances/%s", PROJECT, INSTANCE);
    }
  }

  @Test
  public void testValidConnection() throws Exception {
    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    String url = getJdbcUrl();
    try (Connection connection = DriverManager.getConnection(url)) {
      assertTrue(connection.isValid(0));
    }
  }

  @Test
  public void testInvalidConnection() throws Exception {
    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    String url;
    if (USE_EMULATOR) {
      url =
          String.format(
              "jdbc:bigtable://localhost:%d/projects/%s/instances/%s",
              BIGTABLE_EMULATOR.getPort(), "bogus-project", "bogus-instance");
    } else {
      url = "jdbc:bigtable:/projects/bogus-project/instances/bogus-instance";
    }
    try (Connection connection = DriverManager.getConnection(url)) {
      // Known issue: Bigtable cannot now whether a connection is established unless
      // a table name is specified. The check would leverage `sampleRowKeys(tableId)`, which will
      // throw an exception if connection fails.
      // For now, a connection will always be "valid" until a query is called.
      // This test is a temporary test to codify this expected behavior.
      assertTrue(connection.isValid(0));
    }
  }

  @Test
  @Ignore(
      "Enable this test after Emulator supports PreparedQuery"
          + " https://github.com/googleapis/google-cloud-go/issues/12049.")
  public void testSelectStatement() throws Exception {
    String url = getJdbcUrl();
    String tableName = "test-table";
    String select = String.format("SELECT * FROM `%s` WHERE _key = ?", tableName);

    emulatorWrapper.createTable(tableName, FAMILY_TEST);
    writeRow(KEY1, tableName);

    Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    try (Connection connection = DriverManager.getConnection(url);
        PreparedStatement statement = connection.prepareStatement(select)) {

      statement.setBytes(1, KEY1.getBytes());
      try (ResultSet rs = statement.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(KEY1, rs.getString("rowkey"));
        assertEquals("test-value", rs.getString(FAMILY_TEST + ":col1"));
        assertFalse(rs.next());
      }
    } finally {
      emulatorWrapper.deleteTable(tableName);
    }
  }
}
