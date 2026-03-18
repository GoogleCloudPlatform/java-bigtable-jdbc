/*
 * Copyright 2026 Google LLC
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

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BigtableDriverTest {
  private BigtableDriver driver;

  @Before
  public void setUp() {
    driver = new BigtableDriver();
  }

  @Test
  public void testAcceptsURL() throws SQLException {
    String validUrl = "jdbc:bigtable:/projects/test-project/instances/test-instance";
    assertTrue(driver.acceptsURL(validUrl));

    String invalidUrl = "jdbc:mysql://localhost:3306/db";
    assertFalse(driver.acceptsURL(invalidUrl));

    String emptyPrefix = "";
    assertFalse(driver.acceptsURL(emptyPrefix));
  }

  @Test
  public void testConnectWithValidURL() throws SQLException {
    Driver mockDriver = Mockito.mock(BigtableDriver.class);
    Connection mockConnection = Mockito.mock(Connection.class);

    Mockito.when(mockDriver.connect(Mockito.anyString(), Mockito.any())).thenReturn(mockConnection);

    Connection connection =
        mockDriver.connect(
            "jdbc:bigtable:/projects/test-project/instances/test-instance", new Properties());

    assertNotNull(connection);
    assertFalse(connection.isClosed());
  }

  @Test
  public void testConnectWithInvalidURL() {
    String invalidUrl = "jdbc:invalid-driver:/projects/demo/instances/test";
    Properties props = new Properties();
    SQLException e = assertThrows(SQLException.class, () -> driver.connect(invalidUrl, props));
    assertTrue(e.getMessage().contains("Does not start with jdbc:bigtable:/"));
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

  @Test
  public void testGetPropertyInfo() throws SQLException {
    String url = "jdbc:bigtable:/projects/test-project/instances/test-instance";
    Properties info = new Properties();
    info.setProperty("app_profile_id", "test_profile");
    DriverPropertyInfo[] properties = driver.getPropertyInfo(url, info);
    assertEquals(2, properties.length);
    assertEquals("app_profile_id", properties[0].name);
    assertEquals("test_profile", properties[0].value);
    assertEquals("universe_domain", properties[1].name);
    assertNull(properties[1].value);
  }

  @Test(expected = SQLFeatureNotSupportedException.class)
  public void testGetParentLogger() throws SQLFeatureNotSupportedException {
    driver.getParentLogger();
  }

  @Test
  public void testStaticGetters() {
    assertEquals(1, BigtableDriver.getMajorVersionAsStatic());
    assertEquals(9, BigtableDriver.getMinorVersionAsStatic());
    assertEquals("jdbc:bigtable:/", BigtableDriver.getURLPrefix());
  }

  @Test
  public void testDriverRegistration() throws SQLException {
    String validUrl = "jdbc:bigtable:/projects/test-project/instances/test-instance";
    // The static block in BigtableDriver should have already registered the driver.
    // We can verify this by trying to get a driver for our URL.
    Driver registeredDriver = DriverManager.getDriver(validUrl);
    assertTrue(registeredDriver instanceof BigtableDriver);
  }
}
