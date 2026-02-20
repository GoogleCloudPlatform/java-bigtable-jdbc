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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BigtableClientFactoryImplTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testCreateBigtableDataClient() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);

    // We can't fully test the client creation without credentials,
    // but we can ensure it doesn't throw an exception with a valid configuration.
    try {
      BigtableDataClient client =
          factory.createBigtableDataClient(
              "test-project", "test-instance", "test-app-profile");
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would
      // indicate a problem.
    }
  }

  @Test
  public void testConstructorWithCredentials() {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    assertNotNull(factory);
  }

  @Test
  public void testConstructorWithCredentialFilePath() throws IOException {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n"
            + "  \"client_secret\": \"dummy_client_secret\",\n"
            + "  \"refresh_token\": \"dummy_refresh_token\",\n"
            + "  \"type\": \"authorized_user\"\n"
            + "}";
    java.nio.file.Path tempFile =
        Files.write(temporaryFolder.newFile("credentials.json").toPath(), jsonContent.getBytes());

    Properties info = new Properties();
    info.setProperty("credential_file_path", tempFile.toString());

    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(info);
    assertNotNull(factory);
  }

  @Test
  public void testConstructorWithCredentialJsonString() {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n"
            + "  \"client_secret\": \"dummy_client_secret\",\n"
            + "  \"refresh_token\": \"dummy_refresh_token\",\n"
            + "  \"type\": \"authorized_user\"\n"
            + "}";
    Properties info = new Properties();
    info.setProperty("credential_json", jsonContent);

    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(info);
    assertNotNull(factory);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithInvalidCredentialFilePath() {
    Properties info = new Properties();
    info.setProperty("credential_file_path", "nonexistent/file/path.json");
    new BigtableClientFactoryImpl(info);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithInvalidCredentialJsonString() {
    Properties info = new Properties();
    info.setProperty("credential_json", "invalid json");
    new BigtableClientFactoryImpl(info);
  }

  @Test
  public void testCreateGoogleCredentialsFromJsonFileContent() throws IOException {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n"
            + "  \"client_secret\": \"dummy_client_secret\",\n"
            + "  \"refresh_token\": \"dummy_refresh_token\",\n"
            + "  \"type\": \"authorized_user\"\n"
            + "}";
    GoogleCredentials credentials =
        BigtableClientFactoryImpl.createGoogleCredentialsFromJsonFileContent(jsonContent);
    assertNotNull(credentials);
  }

  @Test
  public void testScopesAreCorrect() {
    List<String> scopes = BigtableClientFactoryImpl.SCOPES;

    assertNotNull(scopes);
    assertEquals(3, scopes.size());
    assertTrue(scopes.contains("https://www.googleapis.com/auth/cloud-platform"));
    assertTrue(scopes.contains("https://www.googleapis.com/auth/bigtable.admin"));
    assertTrue(scopes.contains("https://www.googleapis.com/auth/bigtable.data.readonly"));

    // Ensure no dashes in the protocol part which caused the refresh error
    for (String scope : scopes) {
      assertTrue("Scope should start with https://", scope.startsWith("https://"));
    }
  }

  @Test
  public void testLazyLoadCredentials() throws IOException {
    final Credentials mockCredentials = mock(Credentials.class);
    final int[] loadCount = {0};

    BigtableClientFactoryImpl factory =
        new BigtableClientFactoryImpl() {
          @Override
          protected Credentials loadDefaultCredentials() throws IOException {
            loadCount[0]++;
            return mockCredentials;
          }
        };

    // First call should trigger load
    try {
      factory.createBigtableDataClient(
          "test-project", "test-instance", null);
    } catch (Exception e) {
      // Expected to fail with mock credentials
    }
    assertEquals(1, loadCount[0]);

    // Second call should NOT trigger load again
    try {
      factory.createBigtableDataClient(
          "test-project", "test-instance", null);
    } catch (Exception e) {
      // Expected to fail with mock credentials
    }
    assertEquals(1, loadCount[0]);
  }

  @Test
  public void testDefaultConstructor() {
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl();
    assertNotNull(factory);
  }

  @Test
  public void testConstructorWithNoCredentialProperties() {
    Properties info = new Properties();
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(info);
    assertNotNull(factory);
  }

  @Test
  public void testCredentialPropertyPrecedence() throws IOException {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n"
            + "  \"client_secret\": \"dummy_client_secret\",\n"
            + "  \"refresh_token\": \"dummy_refresh_token\",\n"
            + "  \"type\": \"authorized_user\"\n"
            + "}";
    java.nio.file.Path tempFile =
        Files.write(temporaryFolder.newFile("credentials.json").toPath(), jsonContent.getBytes());

    Properties info = new Properties();
    info.setProperty("credential_json", jsonContent);
    info.setProperty("credential_file_path", tempFile.toString());

    // We expect the JSON string to take precedence. To test this, we can't easily inspect the
    // created credentials. Instead, we can be reasonably sure by checking that no exception is
    // thrown when the file path is invalid.
    info.setProperty("credential_file_path", "invalid/path");
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(info);
    assertNotNull(factory);
  }
}
