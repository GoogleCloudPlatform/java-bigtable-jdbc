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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.jdbc.client.IBigtableClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class BigtableConnectionTest {
  private final String baseURL = "jdbc:bigtable:/projects/test-project/instances/test-instance";
  private Properties properties = new Properties();
  @Mock
  private BigtableDataClient mockDataClient;
  @Mock
  private IBigtableClientFactory mockClientFactory;
  private AutoCloseable closeable;

  @Before
  public void openMocks() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  private BigtableConnection createConnection() throws SQLException {
    return new BigtableConnection(baseURL, properties, mockDataClient, mockClientFactory);
  }

  @Test
  public void testValidClientCreation() throws SQLException, IOException {
    when(mockClientFactory.createBigtableDataClient("test-project", "test-instance", null, null,
        443)).thenReturn(mockDataClient);
    new BigtableConnection(baseURL, properties, null, mockClientFactory);
    verify(mockClientFactory).createBigtableDataClient("test-project", "test-instance", null, null,
        443);
  }

  @Test
  public void testValidClientCreationWithAppProfile() throws SQLException, IOException {
    String url = baseURL + "?app_profile_id=test-profile";
    when(mockClientFactory.createBigtableDataClient("test-project", "test-instance", "test-profile",
        null, 443)).thenReturn(mockDataClient);
    new BigtableConnection(url, properties, null, mockClientFactory);
    verify(mockClientFactory).createBigtableDataClient("test-project", "test-instance",
        "test-profile", null, 443);
  }

  @Test
  public void testValidClientCreationWithHostAndPort() throws SQLException, IOException {
    String url = "jdbc:bigtable://localhost:8080/projects/test-project/instances/test-instance";
    when(mockClientFactory.createBigtableDataClient("test-project", "test-instance", null,
        "localhost", 8080)).thenReturn(mockDataClient);
    new BigtableConnection(url, properties, null, mockClientFactory);
    verify(mockClientFactory).createBigtableDataClient("test-project", "test-instance", null,
        "localhost", 8080);
  }

  @Test
  public void testValidClientCreationWithHost() throws SQLException, IOException {
    String url = "jdbc:bigtable://localhost/projects/test-project/instances/test-instance";
    when(mockClientFactory.createBigtableDataClient("test-project", "test-instance", null,
        "localhost", 443)).thenReturn(mockDataClient);
    new BigtableConnection(url, properties, null, mockClientFactory);
    verify(mockClientFactory).createBigtableDataClient("test-project", "test-instance", null,
        "localhost", 443);
  }

  @Test
  public void testValidClientCreationWithHostPortAndAppProfile() throws SQLException, IOException {
    String url =
        "jdbc:bigtable://localhost:8080/projects/test-project/instances/test-instance?app_profile_id=test-profile";
    when(mockClientFactory.createBigtableDataClient("test-project", "test-instance", "test-profile",
        "localhost", 8080)).thenReturn(mockDataClient);
    new BigtableConnection(url, properties, null, mockClientFactory);
    verify(mockClientFactory).createBigtableDataClient("test-project", "test-instance",
        "test-profile", "localhost", 8080);
  }

  @Test
  public void testValidClientCreationWithHostAndDefaultPort() throws SQLException, IOException {
    String url = "jdbc:bigtable://localhost/projects/test-project/instances/test-instance";
    when(mockClientFactory.createBigtableDataClient("test-project", "test-instance", null,
        "localhost", 443)).thenReturn(mockDataClient);
    new BigtableConnection(url, properties, null, mockClientFactory);
    verify(mockClientFactory).createBigtableDataClient("test-project", "test-instance", null,
        "localhost", 443);
  }

  @Test
  public void testConnectionWithDataClient() throws SQLException {
    BigtableConnection bigtableConnection = createConnection();
    assertNotNull(bigtableConnection);
  }

  @Test
  public void testIsClosed() throws SQLException {
    Connection connection = createConnection();
    assertFalse(connection.isClosed());
    connection.close();
    assertTrue(connection.isClosed());
  }

  @Test
  public void testClose() throws SQLException {
    Connection connection = createConnection();
    assertFalse(connection.isClosed());
    connection.close();
    verify(mockDataClient).close();
    assertTrue(connection.isClosed());
    connection.close();
    assertTrue(connection.isClosed());
  }

  @Test
  public void testCreateStatement() throws SQLException {
    Connection connection = createConnection();
    assertNotNull(connection.createStatement());
  }

  @Test
  public void testCreateStatementWithUnsupportedType() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createStatement(0, 0);
    });
  }

  @Test
  public void testCreateStatementWithUnsupportedConcurrency() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, 0);
    });
  }

  @Test
  public void testPrepareStatement() throws SQLException {
    Connection connection = createConnection();
    assertNotNull(connection.prepareStatement("SELECT * FROM table"));
  }

  @Test
  public void testNativeSQL() throws SQLException {
    Connection connection = createConnection();
    String sql = "SELECT * FROM table";
    assertEquals(sql, connection.nativeSQL(sql));
  }

  @Test
  public void testGetTypeMap() throws SQLException {
    Connection connection = createConnection();
    assertNotNull(connection.getTypeMap());
  }

  @Test
  public void testSetTypeMap() throws SQLException {
    Connection connection = createConnection();
    java.util.Map<String, Class<?>> map = new java.util.HashMap<>();
    connection.setTypeMap(map);
    assertEquals(map, connection.getTypeMap());
  }

  @Test
  public void testSetTypeMapWithNull() {
    assertThrows(SQLException.class, () -> {
      Connection connection = createConnection();
      connection.setTypeMap(null);
    });
  }

  @Test
  public void testIsValid() throws SQLException {
    Connection connection = createConnection();
    assertTrue(connection.isValid(0));
  }

  @Test
  public void testSetClientInfoProperties() throws SQLException {
    BigtableConnection connection = createConnection();
    Properties properties = new Properties();
    properties.setProperty("test", "test");
    connection.setClientInfo(properties);
    assertNull(connection.getClientInfo("test"));
    assertNotNull(connection.getWarnings());
    assertEquals("Client info properties are not supported.",
        connection.getWarnings().getMessage());
  }

  @Test
  public void testSetClientInfoWithNameAndValue() throws SQLException {
    BigtableConnection connection = createConnection();
    connection.setClientInfo("test", "test");
    assertNull(connection.getClientInfo("test"));
    assertNotNull(connection.getWarnings());
    assertEquals("Client info properties are not supported.",
        connection.getWarnings().getMessage());
  }

  @Test
  public void testGetClientInfo() throws SQLException {
    BigtableConnection connection = createConnection();
    assertNull(connection.getClientInfo());
    assertNull(connection.getClientInfo("name"));
  }

  @Test
  public void testWarnings() throws SQLException {
    BigtableConnection connection = createConnection();
    assertNull(connection.getWarnings());

    SQLWarning warning1 = new SQLWarning("Warning 1");
    connection.pushWarning(warning1);
    assertEquals(warning1, connection.getWarnings());

    SQLWarning warning2 = new SQLWarning("Warning 2");
    connection.pushWarning(warning2);
    assertEquals(warning1, connection.getWarnings());
    assertEquals(warning2, connection.getWarnings().getNextWarning());

    connection.clearWarnings();
    assertNull(connection.getWarnings());
  }

  @Test
  public void testIsReadOnly() throws SQLException {
    Connection connection = createConnection();
    assertTrue(connection.isReadOnly());
  }

  @Test
  public void testUnsupportedFeatures() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.prepareCall("SELECT * FROM table");
    });

    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.setAutoCommit(true);
    });

    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.getAutoCommit();
    });

    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.commit();
    });

    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.rollback();
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createClob();
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createBlob();
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createNClob();
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createSQLXML();
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createStruct("my_type", new Object[0]);
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.setTransactionIsolation(Connection.TRANSACTION_NONE);
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.setReadOnly(true);
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.setCatalog("test");
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.setSchema("test");
    });
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.setSavepoint();
    });
  }

  @Test
  public void testSetHoldability() throws SQLException {
    Connection connection = createConnection();
    connection.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
    });
  }

  @Test
  public void testClosedConnection() throws SQLException {
    Connection connection = createConnection();
    connection.close();

    assertThrows(SQLException.class, connection::createStatement);
    assertThrows(SQLException.class, () -> connection.prepareStatement("SELECT * FROM table"));
    assertThrows(SQLException.class, () -> connection.nativeSQL("SELECT * FROM table"));
    assertThrows(SQLException.class, connection::getTypeMap);
    assertThrows(SQLException.class, () -> connection.setTypeMap(new java.util.HashMap<>()));
    assertThrows(SQLException.class, connection::getHoldability);
    assertThrows(SQLException.class, () -> connection.isValid(0));
  }

  @Test
  public void testInvalidURL() {
    assertThrows(SQLException.class, () -> {
      new BigtableConnection(null, properties, null);
    });

    assertThrows(SQLException.class, () -> {
      new BigtableConnection("jdbc:mysql://localhost", properties, null);
    });

    assertThrows(SQLException.class, () -> {
      new BigtableConnection("jdbc:bigtable:/projects/test-project", properties, null);
    });
  }

  @Test
  public void testDuplicatePropertyInURL() {
    String urlWithDuplicate = baseURL + "?app_profile_id=dev&app_profile_id=prod";
    assertThrows(SQLException.class, () -> {
      new BigtableConnection(urlWithDuplicate, properties, null);
    });
  }

  @Test
  public void testDuplicatePropertyInURLAndProperties() {
    Properties props = new Properties();
    props.setProperty("app_profile_id", "prod");
    String url = baseURL + "?app_profile_id=dev";
    assertThrows(SQLException.class, () -> {
      new BigtableConnection(url, props, null);
    });
  }

  @Test
  public void testUnsupportedPropertyInURL() {
    String urlWithUnsupported = baseURL + "?unsupported_key=value";
    assertThrows(SQLException.class, () -> {
      new BigtableConnection(urlWithUnsupported, properties, null);
    });
  }

  @Test
  public void testCreateStatementWithValidParameters() throws SQLException {
    Connection connection = createConnection();
    assertNotNull(
        connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
  }

  @Test
  public void testCreateStatementWithHoldability() throws SQLException {
    Connection connection = createConnection();
    assertNotNull(connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT));
  }

  @Test
  public void testCreateStatementWithUnsupportedHoldability() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
          ResultSet.CLOSE_CURSORS_AT_COMMIT);
    });
  }

  @Test
  public void testPrepareStatementWithValidParameters() throws SQLException {
    Connection connection = createConnection();
    assertNotNull(connection.prepareStatement("SELECT * FROM table", ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY));
  }

  @Test
  public void testPrepareStatementWithHoldability() throws SQLException {
    Connection connection = createConnection();
    assertNotNull(connection.prepareStatement("SELECT * FROM table", ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT));
  }

  @Test
  public void testPrepareStatementWithUnsupportedHoldability() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.prepareStatement("SELECT * FROM table", ResultSet.TYPE_FORWARD_ONLY,
          ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    });
  }

  @Test
  public void testPrepareStatementWithAutoGeneratedKeys() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.prepareStatement("SELECT * FROM table", Statement.RETURN_GENERATED_KEYS);
    });
  }

  @Test
  public void testPrepareStatementWithColumnIndexes() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.prepareStatement("SELECT * FROM table", new int[] {1});
    });
  }

  @Test
  public void testPrepareStatementWithColumnNames() {
    assertThrows(SQLFeatureNotSupportedException.class, () -> {
      Connection connection = createConnection();
      connection.prepareStatement("SELECT * FROM table", new String[] {"col1"});
    });
  }

  @Test
  public void testGetHoldability() throws SQLException {
    Connection connection = createConnection();
    assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, connection.getHoldability());
  }

  @Test
  public void testEmptyURL() {
    assertThrows(SQLException.class, () -> {
      new BigtableConnection("", properties, null);
    });
  }
}
