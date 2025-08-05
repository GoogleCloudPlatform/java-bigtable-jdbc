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

package com.google.cloud.bigtable.jdbc.client;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;

import java.io.IOException;

public class BigtableClientFactory {
  private final Credentials credentials;
  public BigtableClientFactory() throws IOException {
    this.credentials = GoogleCredentials.getApplicationDefault();
  }

  public BigtableClientFactory(Credentials credentials) {
    this.credentials = credentials;
  }

  public BigtableDataClient createBigtableDataClient(
    String projectId,
    String instanceId,
    String appProfileId) throws IOException {
    BigtableDataSettings.Builder builder = BigtableDataSettings.newBuilder()
      .setProjectId(projectId)
      .setInstanceId(instanceId)
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials));

    if (appProfileId != null) {
      builder.setAppProfileId(appProfileId);
    }

    builder.stubSettings()
      .setHeaderProvider(FixedHeaderProvider.create("user-agent", "bigtable-jdbc/1.0.0"));

    return BigtableDataClient.create(builder.build());
  }
}
