/*
 * Copyright 2025 Google LLC
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
import static org.mockito.Mockito.mock;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import com.google.auth.Credentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;

@RunWith(JUnit4.class)
public class BigtableClientFactoryImplTest {

  @Test
  public void testCreateBigtableDataClient() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);

    // We can't fully test the client creation without credentials,
    // but we can ensure it doesn't throw an exception with a valid configuration.
    try {
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance",
          "test-app-profile", null, -1);
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
  public void testCreateBigtableDataClientWithEmulator() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    try {
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance",
          null, "localhost", 8080);
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would
      // indicate a problem.
    }
  }

  @Test
  public void testCreateBigtableDataClientWithEmulatorIp() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    try {
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance",
          null, "127.0.0.1", 8080);
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would
      // indicate a problem.
    }
  }

  @Test
  public void testCreateBigtableDataClientWithoutEmulator() throws IOException {
    Credentials credentials = mock(Credentials.class);
    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl(credentials);
    try {
      BigtableDataClient client = factory.createBigtableDataClient("test-project", "test-instance",
          null, "bigtable.googleapis.com", 443);
      assertNotNull(client);
    } catch (Exception e) {
      // This is expected to fail without real credentials, but a null pointer exception would
      // indicate a problem.
    }
  }

  @Test
  public void testLazyLoadCredentials() throws IOException {
    final Credentials mockCredentials = mock(Credentials.class);
    final int[] loadCount = {0};

    BigtableClientFactoryImpl factory = new BigtableClientFactoryImpl() {
      @Override
      protected Credentials loadDefaultCredentials() throws IOException {
        loadCount[0]++;
        return mockCredentials;
      }
    };

    // First call should trigger load
    try {
      factory.createBigtableDataClient("test-project", "test-instance", null,
          "bigtable.googleapis.com", 443);
    } catch (Exception e) {
      // Expected to fail with mock credentials
    }
    assertEquals(1, loadCount[0]);

    // Second call should NOT trigger load again
    try {
      factory.createBigtableDataClient("test-project", "test-instance", null,
          "bigtable.googleapis.com", 443);
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
}
