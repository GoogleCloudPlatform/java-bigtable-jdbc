package com.google.cloud.bigtable.jdbc;

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.*;

public class BigtableDriverTest {
  private BigtableDriver driver;

  @Before
  public void setUp() {
    driver = new BigtableDriver();
  }

  @Test
  public void testAcceptsURL() throws SQLException {
    // Valid URL Prefix
    String validUrl = "jdbc:bigtable:/projects/test-project/instances/test-instance";
    assertTrue(driver.acceptsURL(validUrl));

    // Invalid URL Prefix
    String invalidUrl = "jdbc:mysql://localhost:3306/db";
    assertFalse(driver.acceptsURL(invalidUrl));

    // Empty Prefix
    String emptyPrefix = "";
    assertFalse(driver.acceptsURL(emptyPrefix));
  }

  @Test
  public void testConnectWithValidURL() throws SQLException {
    String url = "jdbc:bigtable:/projects/test-project/instances/test-instance";
    Properties props = new Properties();
    Connection connection = driver.connect(url, props);
    assertNotNull(connection);
    assertFalse(connection.isClosed());
  }

  @Test
  public void testConnectWithInvalidURL() throws SQLException {
    String invalidUrl = "jdbc:invalid-driver:/projects/demo/instances/test";
    Properties props = new Properties();
    Connection connection = driver.connect(invalidUrl, props);
    assertNull(connection);
  }

  @Test
  public void testGetMajorAndMinorVersion() {
    assertEquals(1, driver.getMajorVersion());
    assertEquals(9, driver.getMinorVersion());
  }

  @Test
  public void testJdbcCompliantReturnsFalse() {
    assertFalse(driver.jdbcCompliant());
  }
}
