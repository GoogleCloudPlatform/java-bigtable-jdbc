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
import static org.mockito.Mockito.when;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.sql.BoundStatement;
import com.google.cloud.bigtable.data.v2.models.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BigtableStatementTest {

  private static final String SQL = "SELECT * FROM table";
  @Mock private BigtableDataClient mockDataClient;
  @Mock private BigtableConnection mockConnection;
  @Mock private ResultSet mockResultSet;
  @Mock private java.sql.ResultSet mockJdbcResultSet;

  private AutoCloseable closeable;

  @Before
  public void openMocks() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  private BigtableStatement createStatement() {
    return new BigtableStatement(mockConnection, mockDataClient);
  }

  @Test
  public void testClose() throws SQLException {
    BigtableStatement statement = createStatement();
    statement.close();
    assertTrue(statement.isClosed());
  }

  @Test
  public void testCloseWithResults() throws SQLException {
    BigtableStatement statement = createStatement();
    statement.resultSets.add(mockJdbcResultSet);
    statement.close();
    assertTrue(statement.isClosed());
    Mockito.verify(mockJdbcResultSet).close();
  }

  @Test
  public void testGetConnection() throws SQLException {
    BigtableStatement statement = createStatement();
    assertNotNull(statement.getConnection());
  }

  @Test
  public void testGetUpdateCount() throws SQLException {
    BigtableStatement statement = createStatement();
    assertEquals(-1, statement.getUpdateCount());
  }

  @Test
  public void testIsClosed() throws SQLException {
    BigtableStatement statement = createStatement();
    assertFalse(statement.isClosed());
    statement.close();
    assertTrue(statement.isClosed());
  }

  @Test
  public void testGetResultSetWithNoResults() throws SQLException {
    BigtableStatement statement = createStatement();
    assertNull(statement.getResultSet());
  }

  @Test
  public void testGetMoreResultsWithNoResults() throws SQLException {
    BigtableStatement statement = createStatement();
    assertFalse(statement.getMoreResults());
  }

  @Test
  public void testGetResultSet() throws SQLException {
    com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement mockPreparedStatement =
        org.mockito.Mockito.mock(
            com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement.class);
    BoundStatement.Builder mockBoundStatementBuilder =
        org.mockito.Mockito.mock(BoundStatement.Builder.class);
    BoundStatement mockBoundStatement = org.mockito.Mockito.mock(BoundStatement.class);

    when(mockDataClient.prepareStatement(SQL, null)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.bind()).thenReturn(mockBoundStatementBuilder);
    when(mockBoundStatementBuilder.build()).thenReturn(mockBoundStatement);
    when(mockDataClient.executeQuery(mockBoundStatement)).thenReturn(mockResultSet);

    BigtableStatement statement = createStatement();
    statement.execute(SQL);
    assertNotNull(statement.getResultSet());
  }

  @Test
  public void testGetMoreResults() throws SQLException {
    BigtableStatement statement = createStatement();
    statement.resultSets.add(mockJdbcResultSet);
    statement.currentResultIndex = 0;
    assertFalse(statement.getMoreResults());
  }

  @Test
  public void testGetMoreResultsWithMultipleResults() throws SQLException {
    BigtableStatement statement = createStatement();
    statement.resultSets.add(mockJdbcResultSet);
    statement.resultSets.add(mockJdbcResultSet);
    statement.currentResultIndex = 0;

    assertTrue(statement.getMoreResults());
    assertEquals(1, statement.currentResultIndex);
    assertFalse(statement.getMoreResults());
    assertEquals(1, statement.currentResultIndex);
  }

  @Test
  public void testExecute() throws SQLException {
    com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement mockPreparedStatement =
        org.mockito.Mockito.mock(
            com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement.class);
    BoundStatement.Builder mockBoundStatementBuilder =
        org.mockito.Mockito.mock(BoundStatement.Builder.class);
    BoundStatement mockBoundStatement = org.mockito.Mockito.mock(BoundStatement.class);

    when(mockDataClient.prepareStatement(SQL, null)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.bind()).thenReturn(mockBoundStatementBuilder);
    when(mockBoundStatementBuilder.build()).thenReturn(mockBoundStatement);
    when(mockDataClient.executeQuery(mockBoundStatement)).thenReturn(mockResultSet);

    BigtableStatement statement = createStatement();
    statement.execute(SQL);
    assertNotNull(statement.getResultSet());
  }

  @Test
  public void testExecuteQueryWithDml() {
    when(mockDataClient.prepareStatement("INSERT INTO table VALUES (1)", null))
        .thenThrow(new IllegalArgumentException());
    BigtableStatement statement = createStatement();
    assertThrows(
        IllegalArgumentException.class,
        () -> statement.executeQuery("INSERT INTO table VALUES (1)"));
  }

  @Test
  public void testExecuteWithDml() {
    when(mockDataClient.prepareStatement("INSERT INTO table VALUES (1)", null))
        .thenThrow(new IllegalArgumentException());
    BigtableStatement statement = createStatement();
    assertThrows(
        IllegalArgumentException.class, () -> statement.execute("INSERT INTO table VALUES (1)"));
  }

  @Test
  public void testUnsupportedFeatures() {
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.setQueryTimeout(1);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.setFetchDirection(1);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.addBatch("SELECT * FROM table");
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.executeBatch();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.setLargeMaxRows(1);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.setMaxRows(1);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.setMaxFieldSize(1);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.setEscapeProcessing(true);
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.setCursorName("test");
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.getFetchDirection();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.getFetchSize();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.getResultSetConcurrency();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.getResultSetType();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.getResultSetHoldability();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.isPoolable();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.isCloseOnCompletion();
        });
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> {
          BigtableStatement statement = createStatement();
          statement.getLargeMaxRows();
        });
  }

  @Test
  public void testExecuteQuery() throws SQLException {
    com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement mockPreparedStatement =
        org.mockito.Mockito.mock(
            com.google.cloud.bigtable.data.v2.models.sql.PreparedStatement.class);
    BoundStatement.Builder mockBoundStatementBuilder =
        org.mockito.Mockito.mock(BoundStatement.Builder.class);
    BoundStatement mockBoundStatement = org.mockito.Mockito.mock(BoundStatement.class);

    when(mockDataClient.prepareStatement(SQL, null)).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.bind()).thenReturn(mockBoundStatementBuilder);
    when(mockBoundStatementBuilder.build()).thenReturn(mockBoundStatement);
    when(mockDataClient.executeQuery(mockBoundStatement)).thenReturn(mockResultSet);

    BigtableStatement statement = createStatement();
    java.sql.ResultSet resultSet = statement.executeQuery(SQL);
    assertNotNull(resultSet);
  }

  @Test
  public void testWarnings() throws SQLException {
    BigtableStatement statement = createStatement();
    assertNull(statement.getWarnings());
    statement.clearWarnings();
    assertNull(statement.getWarnings());
  }

  @Test
  public void testClosedStatement() throws SQLException {
    BigtableStatement statement = createStatement();
    statement.close();

    assertThrows(SQLException.class, () -> statement.execute("SELECT * FROM foo"));
    assertThrows(SQLException.class, () -> statement.executeQuery("SELECT * FROM foo"));
    assertThrows(SQLException.class, () -> statement.getResultSet());
    assertThrows(SQLException.class, () -> statement.getUpdateCount());
    assertThrows(SQLException.class, () -> statement.getMoreResults());
    assertThrows(SQLException.class, () -> statement.getConnection());
  }
}
