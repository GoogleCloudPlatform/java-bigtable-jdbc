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

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.jdbc.client.BigtableClientFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
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
  private final BigtableDataClient client;
  private boolean isClosed = false;
  private static final Set<String> SUPPORTED_KEYS = new HashSet<>(Arrays.asList("app_profile_id", "universe_domain"));

  public BigtableConnection(String url, Properties info) throws SQLException {
    try {
      Properties urlParams = parseURL(url);
      for (String key : info.stringPropertyNames()) {
        if (urlParams.containsKey(key) && SUPPORTED_KEYS.contains(key)) {
          throw new SQLException("Duplicate property found in both URL and connection properties: " + key);
        }
      }
      Properties connectionParams = new Properties();
      connectionParams.putAll(urlParams);
      connectionParams.putAll(info);
      this.client = createBigtableDataClient(connectionParams);
    } catch (Exception e) {
      throw new SQLException("Failed to connect to Bigtable", e);
    }
  }

  private Properties parseURL(String url) {
    if (url == null || !url.startsWith("jdbc:bigtable:")) {
      throw new IllegalArgumentException("URL must start with jdbc:bigtable:");
    }

    Properties uriProps;
    try {
      URI uri = new URI(url.substring("jdbc:".length()));
      uriProps = parseProjectAndInstanceFromUrl(uri);
      String query = uri.getQuery();
      if (query == null) return uriProps;

      String[] pairs = query.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf('=');
        if (idx > 0) {
          String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
          String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());

          if (!SUPPORTED_KEYS.contains(key)) {
            throw new SQLException("Unrecognized connection parameter: " + key);
          }
          if (uriProps.containsKey(key)) {
            throw new SQLException("Duplicate parameter detected: " + key);
          }
          uriProps.setProperty(key, value);
        }
      }
    } catch (UnsupportedEncodingException | SQLException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Malformed JDBC URL: " + url, e);
    }
    return uriProps;
  }

  private Properties parseProjectAndInstanceFromUrl(URI uri) {
    String url = uri.getPath();
    if (url == null || !url.startsWith("/projects/")) {
      throw new IllegalArgumentException(
        "Invalid Bigtable JDBC URL path: expected it to start with '/projects/', but got: " + url
      );
    }

    String[] parts = url.substring("/projects/".length()).split("/");

    if (parts.length < 3 || !"instances".equals(parts[1])) {
      throw new IllegalArgumentException(
        "Invalid path structure in JDBC URL. Expected 'instances' at position 2, but found: '" +
          (parts.length > 1 ? parts[1] : "null") + "'. Full path: " + url
      );
    }

    Properties properties = new Properties();
    properties.setProperty("projectId", parts[0]);
    properties.setProperty("instanceId", parts[1]);

    return properties;
  }

  private BigtableDataClient createBigtableDataClient(Properties properties) {
    try {
      String projectId = properties.getProperty("projectId");
      String instanceId = properties.getProperty("instanceId");
      String appProfileId = properties.getProperty("app_profile_id");

      BigtableClientFactory bigtableClientFactory = new BigtableClientFactory();

      return bigtableClientFactory.createBigtableDataClient(
        projectId,
        instanceId,
        appProfileId
      );
    } catch (IOException e) {
      throw new RuntimeException("Failed to create bigtable client", e);
    }
  }

  private void checkClosed() throws SQLException {
    if (isClosed) {
      throw new SQLException("This Statement is already closed.");
    }
  }

  @Override
  public Statement createStatement() throws SQLException {
    checkClosed();
    return new BigtableStatement(client);
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    checkClosed();
    return new BigtablePreparedStatement(sql, client);
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
      isClosed = true;
    }
  }

  @Override
  public boolean isClosed() throws SQLException {
    throw new SQLFeatureNotSupportedException("isClosed is not supported");
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return new DatabaseMetaData() {
      @Override
      public boolean allProceduresAreCallable() throws SQLException {
        return false;
      }

      @Override
      public boolean allTablesAreSelectable() throws SQLException {
        return false;
      }

      @Override
      public String getURL() throws SQLException {
        return null;
      }

      @Override
      public String getUserName() throws SQLException {
        return null;
      }

      @Override
      public boolean isReadOnly() throws SQLException {
        return false;
      }

      @Override
      public boolean nullsAreSortedHigh() throws SQLException {
        return false;
      }

      @Override
      public boolean nullsAreSortedLow() throws SQLException {
        return false;
      }

      @Override
      public boolean nullsAreSortedAtStart() throws SQLException {
        return false;
      }

      @Override
      public boolean nullsAreSortedAtEnd() throws SQLException {
        return false;
      }

      @Override
      public String getDatabaseProductName() throws SQLException {
        return null;
      }

      @Override
      public String getDatabaseProductVersion() throws SQLException {
        return null;
      }

      @Override
      public String getDriverName() throws SQLException {
        return null;
      }

      @Override
      public String getDriverVersion() throws SQLException {
        return null;
      }

      @Override
      public int getDriverMajorVersion() {
        return 0;
      }

      @Override
      public int getDriverMinorVersion() {
        return 0;
      }

      @Override
      public boolean usesLocalFiles() throws SQLException {
        return false;
      }

      @Override
      public boolean usesLocalFilePerTable() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public boolean storesUpperCaseIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public boolean storesLowerCaseIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public boolean storesMixedCaseIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return false;
      }

      @Override
      public String getIdentifierQuoteString() throws SQLException {
        return null;
      }

      @Override
      public String getSQLKeywords() throws SQLException {
        return null;
      }

      @Override
      public String getNumericFunctions() throws SQLException {
        return null;
      }

      @Override
      public String getStringFunctions() throws SQLException {
        return null;
      }

      @Override
      public String getSystemFunctions() throws SQLException {
        return null;
      }

      @Override
      public String getTimeDateFunctions() throws SQLException {
        return null;
      }

      @Override
      public String getSearchStringEscape() throws SQLException {
        return null;
      }

      @Override
      public String getExtraNameCharacters() throws SQLException {
        return null;
      }

      @Override
      public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsColumnAliasing() throws SQLException {
        return false;
      }

      @Override
      public boolean nullPlusNonNullIsNull() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsConvert() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsConvert(int fromType, int toType) throws SQLException {
        return false;
      }

      @Override
      public boolean supportsTableCorrelationNames() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsExpressionsInOrderBy() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsOrderByUnrelated() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsGroupBy() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsGroupByUnrelated() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsGroupByBeyondSelect() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsLikeEscapeClause() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsMultipleResultSets() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsMultipleTransactions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsNonNullableColumns() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsMinimumSQLGrammar() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsCoreSQLGrammar() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsExtendedSQLGrammar() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsANSI92FullSQL() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsOuterJoins() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsFullOuterJoins() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsLimitedOuterJoins() throws SQLException {
        return false;
      }

      @Override
      public String getSchemaTerm() throws SQLException {
        return null;
      }

      @Override
      public String getProcedureTerm() throws SQLException {
        return null;
      }

      @Override
      public String getCatalogTerm() throws SQLException {
        return null;
      }

      @Override
      public boolean isCatalogAtStart() throws SQLException {
        return false;
      }

      @Override
      public String getCatalogSeparator() throws SQLException {
        return null;
      }

      @Override
      public boolean supportsSchemasInDataManipulation() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsPositionedDelete() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsPositionedUpdate() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSelectForUpdate() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsStoredProcedures() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSubqueriesInComparisons() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSubqueriesInExists() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSubqueriesInIns() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsCorrelatedSubqueries() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsUnion() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsUnionAll() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return false;
      }

      @Override
      public int getMaxBinaryLiteralLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxCharLiteralLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxColumnNameLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxColumnsInGroupBy() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxColumnsInIndex() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxColumnsInOrderBy() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxColumnsInSelect() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxColumnsInTable() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxConnections() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxCursorNameLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxIndexLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxSchemaNameLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxProcedureNameLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxCatalogNameLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxRowSize() throws SQLException {
        return 0;
      }

      @Override
      public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return false;
      }

      @Override
      public int getMaxStatementLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxStatements() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxTableNameLength() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxTablesInSelect() throws SQLException {
        return 0;
      }

      @Override
      public int getMaxUserNameLength() throws SQLException {
        return 0;
      }

      @Override
      public int getDefaultTransactionIsolation() throws SQLException {
        return 0;
      }

      @Override
      public boolean supportsTransactions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        return false;
      }

      @Override
      public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return false;
      }

      @Override
      public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return false;
      }

      @Override
      public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return false;
      }

      @Override
      public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getSchemas() throws SQLException {
        return null;
      }

      @Override
      public ResultSet getCatalogs() throws SQLException {
        return null;
      }

      @Override
      public ResultSet getTableTypes() throws SQLException {
        return null;
      }

      @Override
      public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getTypeInfo() throws SQLException {
        return null;
      }

      @Override
      public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        return null;
      }

      @Override
      public boolean supportsResultSetType(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        return false;
      }

      @Override
      public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean ownDeletesAreVisible(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean ownInsertsAreVisible(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean othersDeletesAreVisible(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean othersInsertsAreVisible(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean updatesAreDetected(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean deletesAreDetected(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean insertsAreDetected(int type) throws SQLException {
        return false;
      }

      @Override
      public boolean supportsBatchUpdates() throws SQLException {
        return false;
      }

      @Override
      public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        return null;
      }

      @Override
      public Connection getConnection() throws SQLException {
        return null;
      }

      @Override
      public boolean supportsSavepoints() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsNamedParameters() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsMultipleOpenResults() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsGetGeneratedKeys() throws SQLException {
        return false;
      }

      @Override
      public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        return null;
      }

      @Override
      public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        return false;
      }

      @Override
      public int getResultSetHoldability() throws SQLException {
        return 0;
      }

      @Override
      public int getDatabaseMajorVersion() throws SQLException {
        return 0;
      }

      @Override
      public int getDatabaseMinorVersion() throws SQLException {
        return 0;
      }

      @Override
      public int getJDBCMajorVersion() throws SQLException {
        return 0;
      }

      @Override
      public int getJDBCMinorVersion() throws SQLException {
        return 0;
      }

      @Override
      public int getSQLStateType() throws SQLException {
        return 0;
      }

      @Override
      public boolean locatorsUpdateCopy() throws SQLException {
        return false;
      }

      @Override
      public boolean supportsStatementPooling() throws SQLException {
        return false;
      }

      @Override
      public RowIdLifetime getRowIdLifetime() throws SQLException {
        return null;
      }

      @Override
      public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        return null;
      }

      @Override
      public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return false;
      }

      @Override
      public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return false;
      }

      @Override
      public ResultSet getClientInfoProperties() throws SQLException {
        return null;
      }

      @Override
      public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        return null;
      }

      @Override
      public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return null;
      }

      @Override
      public boolean generatedKeyAlwaysReturned() throws SQLException {
        return false;
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
      }
    };
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
    return null;
  }

  @Override
  public void clearWarnings() throws SQLException {

  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
    checkClosed();
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
      throw new SQLFeatureNotSupportedException("Only TYPE_FORWARD_ONLY is supported");
    }
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
      throw new SQLFeatureNotSupportedException("Only CONCUR_READ_ONLY is supported");
    }
    return new BigtableStatement(client);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    checkClosed();
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
      throw new SQLFeatureNotSupportedException("Only TYPE_FORWARD_ONLY is supported");
    }
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
      throw new SQLFeatureNotSupportedException("Only CONCUR_READ_ONLY is supported");
    }
    return new BigtablePreparedStatement(sql, client);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
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
    throw new SQLFeatureNotSupportedException("setHoldability is not supported");
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
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
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
    return new BigtableStatement(client);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
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
    return new BigtablePreparedStatement(sql, client);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
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
    checkClosed();
    return true;
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    try {
      throw new SQLFeatureNotSupportedException(" is not supported");
    } catch (SQLFeatureNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    try {
      throw new SQLFeatureNotSupportedException("setClientInfo is not supported");
    } catch (SQLFeatureNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getClientInfo(String name) throws SQLException {
    throw new SQLFeatureNotSupportedException("getClientInfo is not supported");
  }

  @Override
  public Properties getClientInfo() throws SQLException {
    throw new SQLFeatureNotSupportedException("getClientInfo is not supported");
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
