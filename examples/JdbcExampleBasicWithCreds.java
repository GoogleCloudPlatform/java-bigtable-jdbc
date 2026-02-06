/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcExampleBasicWithCreds {

  public static void main(String[] args) {
    if (args.length != 4) {
      System.err.println(
          "Usage: JdbcExampleBasicWithCreds <project_id> <instance_id> <table_id>"
              + " <cred_file_path>");
      System.exit(1);
    }
    // Replace with your project and instance IDs
    String projectId = args[0];
    String instanceId = args[1];
    String tableId = args[2];
    String credFilePath = args[3];

    System.out.println("Project ID: " + projectId);
    System.out.println("Instance ID: " + instanceId);
    System.out.println("Table ID: " + tableId);
    System.out.println("Credential File Path: " + credFilePath);

    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", projectId, instanceId);

    System.out.println("Connecting to " + url);

    try {
      // Load the Bigtable JDBC driver
      Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
    } catch (ClassNotFoundException e) {
      System.err.println("Failed to load Bigtable JDBC driver: " + e.getMessage());
      e.printStackTrace();
      return;
    }

    String sql = "SELECT * from " + tableId;

    // This is a sample service account key. Please replace with your own service account key.

    Properties info = new Properties();
    info.setProperty("credential_file_path", credFilePath);
    try (Connection connection = DriverManager.getConnection(url, info);
        PreparedStatement statement = connection.prepareStatement(sql)) {

      System.out.println("Connection successful!");

      System.out.println("Executing query: " + sql);

      try (ResultSet resultSet = statement.executeQuery()) {
        System.out.println("Query executed successfully.");
        int columnCount = resultSet.getMetaData().getColumnCount();
        System.out.println("Result set has " + columnCount + " columns.");

        while (resultSet.next()) {
          System.out.println("--- Row ---");
          for (int i = 1; i <= columnCount; i++) {
            String columnName = resultSet.getMetaData().getColumnName(i);
            Object value = resultSet.getObject(i);
            System.out.println(columnName + ": " + value);
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("An SQL error occurred: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
