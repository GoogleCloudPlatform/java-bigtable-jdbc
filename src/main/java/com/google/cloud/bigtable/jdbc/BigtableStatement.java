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
import com.google.cloud.bigtable.data.v2.models.sql.BoundStatement;
import com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class BigtableStatement implements Statement {
  private final BigtableDataClient client;
  private boolean isClosed = false;

  public BigtableStatement(BigtableDataClient client) {
    this.client = client;
  }
  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    checkClosed();
    PreparedStatement preparedStatement = client.prepareStatement(sql, null);
    BoundStatement boundStatement = preparedStatement.bind().build();

    com.google.cloud.bigtable.data.v2.models.sql.ResultSet resultSet =
      client.executeQuery(boundStatement);
    return new BigtableResultSet(resultSet);
  }

  private void checkClosed() throws SQLException {
    if (isClosed) {
      throw new SQLException("This Statement is already closed.");
    }
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public void close() throws SQLException {
    if (!isClosed) {
      isClosed = true;
    }
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    throw new SQLFeatureNotSupportedException("getMaxFieldSize is not supported");
  }

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    throw new SQLFeatureNotSupportedException("setMaxFieldSize is not supported");
  }

  @Override
  public int getMaxRows() throws SQLException {
    throw new SQLFeatureNotSupportedException("getMaxRows is not supported");
  }

  @Override
  public void setMaxRows(int max) throws SQLException {
    throw new SQLFeatureNotSupportedException("setMaxRows is not supported");
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    throw new SQLFeatureNotSupportedException("setEscapeProcessing is not supported");
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    throw new SQLFeatureNotSupportedException("getQueryTimeout is not supported");
  }

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    throw new SQLFeatureNotSupportedException("setQueryTimeout is not supported");
  }

  @Override
  public void cancel() throws SQLException {
    throw new SQLFeatureNotSupportedException("cancel is not supported");
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return null;
  }

  @Override
  public void clearWarnings() throws SQLException {

  }

  @Override
  public void setCursorName(String name) throws SQLException {
    throw new SQLFeatureNotSupportedException("setCursorName is not supported");
  }

  @Override
  public boolean execute(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    throw new SQLFeatureNotSupportedException("getResultSet is not supported");
  }

  @Override
  public int getUpdateCount() throws SQLException {
    throw new SQLFeatureNotSupportedException("getUpdateCount is not supported");
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    throw new SQLFeatureNotSupportedException("getMoreResults is not supported");
  }

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    throw new SQLFeatureNotSupportedException("setFetchDirection is not supported");
  }

  @Override
  public int getFetchDirection() throws SQLException {
    throw new SQLFeatureNotSupportedException("getFetchDirection is not supported");
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    throw new SQLFeatureNotSupportedException("setFetchSize is not supported");
  }

  @Override
  public int getFetchSize() throws SQLException {
    throw new SQLFeatureNotSupportedException("getFetchSize is not supported");
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    throw new SQLFeatureNotSupportedException("getResultSetConcurrency is not supported");
  }

  @Override
  public int getResultSetType() throws SQLException {
    throw new SQLFeatureNotSupportedException("getResultSetType is not supported");
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("addBatch is not supported");
  }

  @Override
  public void clearBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException("clearBatch is not supported");
  }

  @Override
  public int[] executeBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException("executeBatch is not supported");
  }

  @Override
  public Connection getConnection() throws SQLException {
    throw new SQLFeatureNotSupportedException("getConnection is not supported");
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    throw new SQLFeatureNotSupportedException("getMoreResults is not supported");
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    throw new SQLFeatureNotSupportedException("getGeneratedKeys is not supported");
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate is not supported");
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute is not supported");
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    throw new SQLFeatureNotSupportedException("getResultSetHoldability is not supported");
  }

  @Override
  public boolean isClosed() throws SQLException {
    throw new SQLFeatureNotSupportedException("isClosed is not supported");
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    throw new SQLFeatureNotSupportedException("setPoolable is not supported");
  }

  @Override
  public boolean isPoolable() throws SQLException {
    throw new SQLFeatureNotSupportedException("isPoolable is not supported");
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    throw new SQLFeatureNotSupportedException("closeOnCompletion is not supported");
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    throw new SQLFeatureNotSupportedException("isCloseOnCompletion is not supported");
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
