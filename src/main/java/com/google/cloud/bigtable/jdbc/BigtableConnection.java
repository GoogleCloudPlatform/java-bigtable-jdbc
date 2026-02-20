/*
 * Copyright 2026 Google LLC
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

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.jdbc.client.BigtableClientFactoryImpl;
import com.google.cloud.bigtable.jdbc.client.IBigtableClientFactory;
import com.google.cloud.bigtable.jdbc.util.BigtableJdbcUrlParser;
import java.io.IOException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

public class BigtableConnection implements Connection {

  private Map<String, Class<?>> typeMap = new HashMap<>();
  // The actual client, responsible for operations and communicates with Bigtable.
  private final BigtableDataClient client;
  private boolean isClosed = false;
  private static final Set<String> SUPPORTED_KEYS =
      new HashSet<>(
          Arrays.asList(
              "app_profile_id", "universe_domain", "credential_file_path", "credential_json"));
  private final IBigtableClientFactory bigtableClientFactory;
  private SQLWarning warnings;

  public BigtableConnection(String url, Properties info) throws SQLException {
    this(url, info, null);
  }

  public BigtableConnection(String url, Properties info, BigtableDataClient dataClient)
      throws SQLException {
    this(url, info, dataClient, createClientFactory(info));
  }

  private static IBigtableClientFactory createClientFactory(Properties info) {
    return new BigtableClientFactoryImpl(info);
  }

  public BigtableConnection(
      String url,
      Properties info,
      BigtableDataClient dataClient,
      IBigtableClientFactory bigtableClientFactory)
      throws SQLException {
    this.bigtableClientFactory = bigtableClientFactory;
    try {
      BigtableJdbcUrlParser.BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);
      Properties urlParams = new Properties();
      urlParams.setProperty("projectId", parsedUrl.getProjectId());
      urlParams.setProperty("instanceId", parsedUrl.getInstanceId());

      for (Map.Entry<String, String> entry : parsedUrl.getQueryParameters().entrySet()) {
        String key = entry.getKey();
        if (!SUPPORTED_KEYS.contains(key)) {
          throw new SQLException("Unrecognized connection parameter: " + key);
        }
        urlParams.setProperty(key, entry.getValue());
      }

      if (dataClient != null) {
        this.client = dataClient;
        return;
      }
      for (String key : info.stringPropertyNames()) {
        if (urlParams.containsKey(key) && SUPPORTED_KEYS.contains(key)) {
          throw new SQLException(
              "Duplicate property found in both URL and connection properties: " + key);
        }
      }
      Properties connectionParams = new Properties();
      connectionParams.putAll(urlParams);
      connectionParams.putAll(info);
      this.client = createBigtableDataClient(connectionParams);
      // Test the connection by executing a simple query.
      // This will help catch any issues with the connection
      validateConnection();
    } catch (java.net.URISyntaxException | IllegalArgumentException e) {
      throw new SQLException("Malformed JDBC URL: " + url, e);
    } catch (Exception e) {
      throw new SQLException("Failed to connect to Bigtable: " + e.getMessage(), e);
    }
  }

  private void validateConnection() throws SQLException {
    try (PreparedStatement statement = this.prepareStatement("select 1");
        ResultSet rs = statement.executeQuery()) {}
  }

  BigtableDataClient createBigtableDataClient(Properties properties) throws IOException {
    String projectId = properties.getProperty("projectId");
    String instanceId = properties.getProperty("instanceId");
    String appProfileId = properties.getProperty("app_profile_id");
    String universeDomain = properties.getProperty("universe_domain");

    return this.bigtableClientFactory.createBigtableDataClient(
        projectId, instanceId, appProfileId, universeDomain);
  }

  private void checkClosed() throws SQLException {
    if (isClosed) {
      throw new SQLException("This Connection is already closed.");
    }
  }

  @Override
  public Statement createStatement() throws SQLException {
    checkClosed();
    return new BigtableStatement(this, client);
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    checkClosed();
    return new BigtablePreparedStatement(this, sql, client);
  }

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("prepareCall is not supported");
  }

  @Override
  public String nativeSQL(String sql) throws SQLException {
    checkClosed();
    return sql;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    throw new SQLFeatureNotSupportedException("setAutoCommit is not supported");
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    throw new SQLFeatureNotSupportedException("getAutoCommit is not supported");
  }

  @Override
  public void commit() throws SQLException {
    throw new SQLFeatureNotSupportedException("commit is not supported");
  }

  @Override
  public void rollback() throws SQLException {
    throw new SQLFeatureNotSupportedException("rollback is not supported");
  }

  @Override
  public void close() throws SQLException {
    if (!isClosed) {
      client.close();
      isClosed = true;
    }
  }

  @Override
  public boolean isClosed() throws SQLException {
    return isClosed;
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    throw new SQLFeatureNotSupportedException("getMetaData is not supported");
  }

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    throw new SQLFeatureNotSupportedException("setReadOnly is not supported");
  }

  @Override
  public boolean isReadOnly() throws SQLException {
    checkClosed();
    return true;
  }

  @Override
  public void setCatalog(String catalog) throws SQLException {
    throw new SQLFeatureNotSupportedException("setCatalog is not supported");
  }

  @Override
  public String getCatalog() throws SQLException {
    throw new SQLFeatureNotSupportedException("getCatalog is not supported");
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    throw new SQLFeatureNotSupportedException("setTransactionIsolation is not supported");
  }

  @Override
  public int getTransactionIsolation() throws SQLException {
    throw new SQLFeatureNotSupportedException("getTransactionIsolation is not supported");
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    checkClosed();
    return warnings;
  }

  @Override
  public void clearWarnings() throws SQLException {
    checkClosed();
    warnings = null;
  }

  /**
   * Add a warning to the chain of warnings.
   *
   * @param warning The warning to be added.
   */
  public void pushWarning(SQLWarning warning) {
    if (this.warnings == null) {
      this.warnings = warning;
    } else {
      this.warnings.setNextWarning(warning);
    }
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency)
      throws SQLException {
    checkClosed();
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
      throw new SQLFeatureNotSupportedException("Only TYPE_FORWARD_ONLY is supported");
    }
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
      throw new SQLFeatureNotSupportedException("Only CONCUR_READ_ONLY is supported");
    }
    return new BigtableStatement(this, client);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    checkClosed();
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
      throw new SQLFeatureNotSupportedException("Only TYPE_FORWARD_ONLY is supported");
    }
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
      throw new SQLFeatureNotSupportedException("Only CONCUR_READ_ONLY is supported");
    }
    return new BigtablePreparedStatement(this, sql, client);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    throw new SQLFeatureNotSupportedException("prepareCall is not supported");
  }

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    checkClosed();
    return typeMap;
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    checkClosed();
    if (map == null) {
      throw new SQLException("Type map cannot be null");
    }
    typeMap = new HashMap<>(map);
  }

  @Override
  public void setHoldability(int holdability) throws SQLException {
    checkClosed();
    if (holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT) {
      throw new SQLFeatureNotSupportedException("CLOSE_CURSORS_AT_COMMIT is not supported");
    }
  }

  @Override
  public int getHoldability() throws SQLException {
    checkClosed();
    return ResultSet.HOLD_CURSORS_OVER_COMMIT;
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {
    throw new SQLFeatureNotSupportedException("setSavepoint is not supported");
  }

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    throw new SQLFeatureNotSupportedException("setSavepoint is not supported");
  }

  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    throw new SQLFeatureNotSupportedException("rollback is not supported");
  }

  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    throw new SQLFeatureNotSupportedException("releaseSavepoint is not supported");
  }

  @Override
  public Statement createStatement(
      int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkClosed();
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
      throw new SQLFeatureNotSupportedException("Only TYPE_FORWARD_ONLY is supported");
    }
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
      throw new SQLFeatureNotSupportedException("Only CONCUR_READ_ONLY is supported");
    }
    if (resultSetHoldability != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
      throw new SQLFeatureNotSupportedException("Only HOLD_CURSORS_OVER_COMMIT is supported");
    }
    return new BigtableStatement(this, client);
  }

  @Override
  public PreparedStatement prepareStatement(
      String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    checkClosed();
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
      throw new SQLFeatureNotSupportedException("Only TYPE_FORWARD_ONLY is supported");
    }
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
      throw new SQLFeatureNotSupportedException("Only CONCUR_READ_ONLY is supported");
    }
    if (resultSetHoldability != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
      throw new SQLFeatureNotSupportedException("Only HOLD_CURSORS_OVER_COMMIT is supported");
    }
    return new BigtablePreparedStatement(this, sql, client);
  }

  @Override
  public CallableStatement prepareCall(
      String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {

    throw new SQLFeatureNotSupportedException("prepareCall is not supported");
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException("prepareStatement is not supported");
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("prepareStatement is not supported");
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("prepareStatement is not supported");
  }

  @Override
  public Clob createClob() throws SQLException {
    throw new SQLFeatureNotSupportedException(" is not supported");
  }

  @Override
  public Blob createBlob() throws SQLException {
    throw new SQLFeatureNotSupportedException("createBlob is not supported");
  }

  @Override
  public NClob createNClob() throws SQLException {
    throw new SQLFeatureNotSupportedException("createNClob is not supported");
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    throw new SQLFeatureNotSupportedException("createSQLXML is not supported");
  }

  @Override
  public boolean isValid(int timeout) throws SQLException {
    if (isClosed) {
      return false;
    }
    SQLWarning warning = new SQLWarning("timeout is not supported in isValid and will be ignored.");
    this.pushWarning(warning);
    try {
      validateConnection();
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  /**
   * This driver does not use any client info properties. A {@link SQLWarning} will also be
   * generated.
   *
   * @param name The name of the client info property to set.
   * @param value The value to set the client info property to.
   */
  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    SQLWarning warning = new SQLWarning("Client info properties are not supported.");
    this.pushWarning(warning);
  }

  /**
   * This driver does not use any client info properties.
   *
   * @param properties The list of client info properties to set.
   */
  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    SQLWarning warning = new SQLWarning("Client info properties are not supported.");
    this.pushWarning(warning);
  }

  @Override
  public String getClientInfo(String name) throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public Properties getClientInfo() throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    throw new SQLFeatureNotSupportedException("createArrayOf is not supported");
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    throw new SQLFeatureNotSupportedException("createStruct is not supported");
  }

  @Override
  public void setSchema(String schema) throws SQLException {
    throw new SQLFeatureNotSupportedException("setSchema is not supported");
  }

  @Override
  public String getSchema() throws SQLException {
    throw new SQLFeatureNotSupportedException("getSchema is not supported");
  }

  @Override
  public void abort(Executor executor) throws SQLException {
    throw new SQLFeatureNotSupportedException("abort is not supported");
  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    throw new SQLFeatureNotSupportedException("setNetworkTimeout is not supported");
  }

  @Override
  public int getNetworkTimeout() throws SQLException {
    throw new SQLFeatureNotSupportedException("getNetworkTimeout is not supported");
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
