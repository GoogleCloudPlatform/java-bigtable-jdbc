/*
 * Copyright 2025 Google LLC
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

package com.google.cloud.bigtable.jdbc.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.stub.metrics.NoopMetricsProvider;

public class BigtableClientFactoryImpl implements IBigtableClientFactory {
  private final Credentials credentials;
  private static final List<String> SCOPES =
      Arrays.asList("https-www.googleapis.com/auth/cloud-platform",
          "https-www.googleapis.com/auth/bigtable.admin",
          "https-www.googleapis.com/auth/bigtable.data.readonly");

  public BigtableClientFactoryImpl() throws IOException {
    this.credentials = GoogleCredentials.getApplicationDefault();
  }

  public BigtableClientFactoryImpl(Properties info) throws SQLException {
    String credFilePath = info.getProperty("credential_file_path");
    String credJsonString = info.getProperty("credential_json");
    String jsonContent = null;
    GoogleCredentials credentials = null;
    try {
      // Direct JSON string is provided
      if (credJsonString != null && !credJsonString.trim().isEmpty()) {
        System.out.println("Using credentials from 'credential_json' property.");
        credentials = createGoogleCredentialsFromJsonFileContent(credJsonString);
      } else if (credFilePath != null && !credFilePath.trim().isEmpty()) {
        // JSON file path is provided
        System.out.println("Using credentials from file: " + credFilePath);
        try {
          jsonContent =
              new String(Files.readAllBytes(Paths.get(credFilePath)), StandardCharsets.UTF_8);
          credentials = createGoogleCredentialsFromJsonFileContent(jsonContent);
        } catch (IOException e) {
          throw new SQLException("Failed to read or parse credential file: " + credFilePath, e);
        }
      } else {
        // No explicit JSON credentials, try default (e.g., ADC)
        System.out.println(
            "No explicit JSON credentials provided. Trying Application Default Credentials.");
        try {
          credentials = GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
        } catch (IOException e) {
          throw new SQLException("Failed to get Application Default Credentials", e);
        }
      }
      this.credentials = credentials;
    } catch (IOException e) {
      throw new SQLException("Authentication failed", e);
    }
  }

  public static GoogleCredentials createGoogleCredentialsFromJsonFileContent(String jsonContent)
      throws IOException {
    return GoogleCredentials
        .fromStream(new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8)))
        .createScoped(SCOPES);
  }

  public BigtableClientFactoryImpl(Credentials credentials) {
    this.credentials = credentials;
  }

  public BigtableDataClient createBigtableDataClient(String projectId, String instanceId,
      String appProfileId, String host, int port) throws IOException {
    BigtableDataSettings.Builder builder;
    if (host != null && (host.equals("localhost") || host.equals("127.0.0.1")) && port != -1) {
      builder = BigtableDataSettings.newBuilderForEmulator(port);
    } else {
      builder = BigtableDataSettings.newBuilder()
          .setCredentialsProvider(FixedCredentialsProvider.create(credentials));
    }
    builder.setProjectId(projectId).setInstanceId(instanceId);

    if (appProfileId != null) {
      builder.setAppProfileId(appProfileId);
    }

    builder.stubSettings()
        .setHeaderProvider(FixedHeaderProvider.create("user-agent", "bigtable-jdbc/1.0.0"))
        .setMetricsProvider(NoopMetricsProvider.INSTANCE);

    // Known issue: BigtableDataClient cannot now whether a connection is established unless
    // a table name is specified. The check would leverage `sampleRowKeys(tableId)`, which will
    // throw an exception if connection fails.
    // For now, a connection will always be "valid" until a query is called.
    return BigtableDataClient.create(builder.build());
  }
}
