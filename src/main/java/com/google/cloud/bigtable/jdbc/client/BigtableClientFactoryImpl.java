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

package com.google.cloud.bigtable.jdbc.client;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.stub.metrics.NoopMetricsProvider;
import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class BigtableClientFactoryImpl implements IBigtableClientFactory {
  private Credentials credentials;

  @VisibleForTesting
  static final List<String> SCOPES =
      Arrays.asList(
          "https://www.googleapis.com/auth/cloud-platform",
          "https://www.googleapis.com/auth/bigtable.admin",
          "https://www.googleapis.com/auth/bigtable.data.readonly");

  public BigtableClientFactoryImpl() {}

  public BigtableClientFactoryImpl(Properties info) {
    try {
      if (info.containsKey("credential_json")) {
        this.credentials =
            createGoogleCredentialsFromJsonFileContent(info.getProperty("credential_json"));
      } else if (info.containsKey("credential_file_path")) {
        this.credentials =
            GoogleCredentials.fromStream(
                    Files.newInputStream(Paths.get(info.getProperty("credential_file_path"))))
                .createScoped(SCOPES);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to load credentials", e);
    }
  }

  public static GoogleCredentials createGoogleCredentialsFromJsonFileContent(String json)
      throws IOException {
    return GoogleCredentials.fromStream(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
        .createScoped(SCOPES);
  }

  public BigtableClientFactoryImpl(Credentials credentials) {
    this.credentials = credentials;
  }

  private synchronized Credentials getCredentials() throws IOException {
    if (credentials == null) {
      credentials = loadDefaultCredentials();
    }
    return credentials;
  }

  protected Credentials loadDefaultCredentials() throws IOException {
    return GoogleCredentials.getApplicationDefault();
  }

  public BigtableDataClient createBigtableDataClient(
      String projectId, String instanceId, String appProfileId, String universeDomain)
      throws IOException {
    BigtableDataSettings.Builder builder =
        BigtableDataSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(getCredentials()));

    builder.setProjectId(projectId).setInstanceId(instanceId);

    if (appProfileId != null) {
      builder.setAppProfileId(appProfileId);
    }

    if (universeDomain != null) {
      builder.stubSettings().setUniverseDomain(universeDomain);
    }

    // Disable CSM and internal metrics to avoid OpenTelemetry dependencies which can cause
    // connection hangs in some environments (e.g. Looker connector).
    builder
        .stubSettings()
        .setHeaderProvider(FixedHeaderProvider.create("user-agent", "bigtable-jdbc/1.0.0"))
        .setMetricsProvider(NoopMetricsProvider.INSTANCE);

    // Known issue: BigtableDataClient cannot now whether a connection is established unless
    // a table name is specified. The check would leverage `sampleRowKeys(tableId)`, which will
    // throw an exception if connection fails.
    // For now, a connection will always be "valid" until a query is called.
    return BigtableDataClient.create(builder.build());
  }
}
