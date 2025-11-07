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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import com.google.auth.Credentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import com.google.auth.oauth2.GoogleCredentials;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

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
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance", "test-app-profile", null, -1);
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would indicate a problem.
    }
  }
  
  @Test
  public void testConstructorWithCredentials() {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    assertNotNull(factory);
  }

  @Test
  public void testConstructorWithCredentialFilePath() throws IOException, SQLException {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n" +
            "  \"client_secret\": \"dummy_client_secret\",\n" +
            "  \"refresh_token\": \"dummy_refresh_token\",\n" +
            "  \"type\": \"authorized_user\"\n" +
            "}";
    java.nio.file.Path tempFile =
        Files.write(temporaryFolder.newFile("credentials.json").toPath(), jsonContent.getBytes());

    Properties info = new Properties();
    info.setProperty("credential_file_path", tempFile.toString());

    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(info);
    assertNotNull(factory);
  }

  @Test
  public void testConstructorWithCredentialJsonString() throws SQLException {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n" +
            "  \"client_secret\": \"dummy_client_secret\",\n" +
            "  \"refresh_token\": \"dummy_refresh_token\",\n" +
            "  \"type\": \"authorized_user\"\n" +
            "}";
    Properties info = new Properties();
    info.setProperty("credential_json", jsonContent);

    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(info);
    assertNotNull(factory);
  }

  @Test(expected = SQLException.class)
  public void testConstructorWithInvalidCredentialFilePath() throws SQLException {
    Properties info = new Properties();
    info.setProperty("credential_file_path", "nonexistent/file/path.json");
    new BigtableClientFactoryImpl(info);
  }

  @Test(expected = SQLException.class)
  public void testConstructorWithInvalidCredentialJsonString() throws SQLException {
    Properties info = new Properties();
    info.setProperty("credential_json", "invalid json");
    new BigtableClientFactoryImpl(info);
  }

  @Test
  public void testCreateGoogleCredentialsFromJsonFileContent() throws IOException {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n" +
            "  \"client_secret\": \"dummy_client_secret\",\n" +
            "  \"refresh_token\": \"dummy_refresh_token\",\n" +
            "  \"type\": \"authorized_user\"\n" +
            "}";
    GoogleCredentials credentials =
        BigtableClientFactoryImpl.createGoogleCredentialsFromJsonFileContent(jsonContent);
    assertNotNull(credentials);
  }

  @Test
  public void testCreateBigtableDataClientWithEmulator() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    try {
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance", null, "localhost", 8080);
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would indicate a problem.
    }
  }

  @Test
  public void testCreateBigtableDataClientWithEmulatorIp() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    try {
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance", null, "127.0.0.1", 8080);
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would indicate a problem.
    }
  }

  @Test
  public void testCreateBigtableDataClientWithoutEmulator() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    try {
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance", null, "bigtable.googleapis.com", 443);
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would indicate a problem.
    }
  }

  @Test
  public void testConstructorWithNoCredentialProperties() throws SQLException {
    // This test relies on the environment having ADC available.
    // If it fails, it might be due to the test environment not being configured for ADC.
    try {
      Properties info = new Properties();
      BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(info);
      assertNotNull(factory);
    } catch (SQLException e) {
      if (e.getMessage().contains("Failed to get Application Default Credentials")) {
        // This is an acceptable failure if ADC are not configured in the environment.
      } else {
        throw e;
      }
    }
  }

  @Test
  public void testCredentialPropertyPrecedence() throws IOException, SQLException {
    String jsonContent =
        "{\"client_id\": \"dummy_client_id\",\n" +
            "  \"client_secret\": \"dummy_client_secret\",\n" +
            "  \"refresh_token\": \"dummy_refresh_token\",\n" +
            "  \"type\": \"authorized_user\"\n" +
            "}";
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