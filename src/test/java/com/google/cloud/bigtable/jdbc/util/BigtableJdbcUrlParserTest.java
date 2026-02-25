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

package com.google.cloud.bigtable.jdbc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.bigtable.jdbc.util.BigtableJdbcUrlParser.BigtableJdbcUrl;
import com.google.common.collect.ImmutableMap;
import java.net.URISyntaxException;
import org.junit.Test;

public class BigtableJdbcUrlParserTest {

  @Test
  public void testParseValidUrlWithParameters() throws URISyntaxException {
    String url =
        "jdbc:bigtable:/projects/my-project-123/instances/my-instance-abc?param1=val1&param2=val2";
    BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);

    assertNotNull(parsedUrl);
    assertEquals("my-project-123", parsedUrl.getProjectId());
    assertEquals("my-instance-abc", parsedUrl.getInstanceId());

    ImmutableMap<String, String> expectedParams =
        ImmutableMap.of("param1", "val1", "param2", "val2");
    assertEquals(expectedParams, parsedUrl.getQueryParameters());
  }

  @Test
  public void testParseUrlWithDuplicateParameters() {
    String url =
        "jdbc:bigtable:/projects/test-project/instances/test-instance?multi=a&multi=b&single=c";
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
    assertTrue(e.getMessage().contains("Duplicate or malformed query parameter: multi=b"));
  }

  @Test
  public void testParseValidUrlWithoutParameters() throws URISyntaxException {
    String url = "jdbc:bigtable:/projects/no-params-project/instances/instance1";
    BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);

    assertNotNull(parsedUrl);
    assertEquals("no-params-project", parsedUrl.getProjectId());
    assertEquals("instance1", parsedUrl.getInstanceId());
    assertTrue(parsedUrl.getQueryParameters().isEmpty());
  }

  @Test
  public void testParseNullUrl() {
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(null));
  }

  @Test
  public void testParseInvalidScheme() {
    String url = "jdbc:mysql:/projects/project-id/instances/instance-id";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseInvalidPathStructureExtraPath() {
    String url = "jdbc:bigtable:/projects/project-id/instances/instance-id/extra";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseInvalidPathStructure() {
    String url = "jdbc:bigtable:/project/project-id/instance/instance-id";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParsePathWithNoProject() {
    String url = "jdbc:bigtable:/instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParsePathWithNoInstance() {
    String url = "jdbc:bigtable:/projects/my-project/";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithEmptyQuery() throws URISyntaxException {
    String url = "jdbc:bigtable:/projects/my-project/instances/my-instance?";
    BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);

    assertNotNull(parsedUrl);
    assertEquals("my-project", parsedUrl.getProjectId());
    assertEquals("my-instance", parsedUrl.getInstanceId());
    assertTrue(parsedUrl.getQueryParameters().isEmpty());
  }

  @Test
  public void testParseUrlWithEmptyParamValue() throws URISyntaxException {
    String url = "jdbc:bigtable:/projects/my-project/instances/my-instance?param=";
    BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);

    assertNotNull(parsedUrl);
    assertEquals("my-project", parsedUrl.getProjectId());
    assertEquals("my-instance", parsedUrl.getInstanceId());
    ImmutableMap<String, String> expectedParams = ImmutableMap.of("param", "");
    assertEquals(expectedParams, parsedUrl.getQueryParameters());
  }

  @Test
  public void testParseUrlWithEmptyProjectId() {
    String url = "jdbc:bigtable:/projects//instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithEmptyInstanceId() {
    String url = "jdbc:bigtable:/projects/my-project/instances/";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithMalformedPath() {
    String url = "jdbc:bigtable:/projects/my-project/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithEncodedSpacesInQuery() throws URISyntaxException {
    String url =
        "jdbc:bigtable:/projects/my-project/instances/my-instance?app_profile_id=val%20with%20spaces";
    BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);
    assertEquals("val with spaces", parsedUrl.getQueryParameters().get("app_profile_id"));
  }

  @Test
  public void testParseUrlWithEncodedHyphenInPath() throws URISyntaxException {
    String url = "jdbc:bigtable:/projects/my%2Dproject/instances/my%2Dinstance";
    BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);
    assertEquals("my-project", parsedUrl.getProjectId());
    assertEquals("my-instance", parsedUrl.getInstanceId());
  }

  @Test
  public void testParseUrlWithInvalidCharactersInPath() {
    String url = "jdbc:bigtable:/projects/my_project/instances/my_instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithTooShortProjectId() {
    String url = "jdbc:bigtable:/projects/proj1/instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithTooLongProjectId() {
    String url = "jdbc:bigtable:/projects/this-project-id-is-too-long-12345/instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithRestrictedProjectId() {
    String url = "jdbc:bigtable:/projects/my-google-project/instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithProjectIdEndingInHyphen() {
    String url = "jdbc:bigtable:/projects/my-project-/instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithSpecialCharactersInQuery() throws URISyntaxException {
    String url =
        "jdbc:bigtable:/projects/project-id/instances/instance-id?app_profile_id=val+with+plus&universe_domain=foo%26bar";
    BigtableJdbcUrl parsedUrl = BigtableJdbcUrlParser.parse(url);
    assertEquals("val with plus", parsedUrl.getQueryParameters().get("app_profile_id"));
    assertEquals("foo&bar", parsedUrl.getQueryParameters().get("universe_domain"));
  }

  @Test
  public void testParseUrlWithHostAndPortRejected() {
    String url = "jdbc:bigtable://localhost:8086/projects/my-project/instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }

  @Test
  public void testParseUrlWithHostAndNoPortRejected() {
    String url = "jdbc:bigtable://localhost/projects/my-project/instances/my-instance";
    assertThrows(IllegalArgumentException.class, () -> BigtableJdbcUrlParser.parse(url));
  }
}
