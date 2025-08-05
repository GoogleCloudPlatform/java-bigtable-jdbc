package com.google.cloud.bigtable.jdbc;

import org.junit.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.*;

public class BigtableConnectionTest {
  private final String baseURL = "jdbc:bigtable:/projects/test-project/instances/test-instance";
  Properties properties = new Properties();

  @Test
  public void testValidClientCreation() throws SQLException {
    BigtableConnection bigtableConnection = new BigtableConnection(baseURL, properties);
    assertNotNull(bigtableConnection);
  }

  @Test
  public void testValidClientCreationWithQueryParams() throws SQLException {
    String queryParamsURL = baseURL + "?appProfileId=dev&endpoint=bigtable.googleapis.com:443";
    BigtableConnection bigtableConnection = new BigtableConnection(queryParamsURL, properties);
    assertNotNull(bigtableConnection);
  }

  @Test
  public void testCheckAuthentication() throws SQLException {
    String queryParamsURL = baseURL + "?accessToken=test-token";
    BigtableConnection bigtableConnection = new BigtableConnection(queryParamsURL,properties);
    assertNotNull(bigtableConnection);
  }
}
