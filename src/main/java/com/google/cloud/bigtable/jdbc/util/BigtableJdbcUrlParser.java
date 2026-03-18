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

package com.google.cloud.bigtable.jdbc.util;

import com.google.common.collect.ImmutableMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigtableJdbcUrlParser {

  private static final String JDBC_SCHEME = "jdbc";
  private static final String BIGTABLE_SSP_PREFIX = "bigtable:";
  private static final int MAX_QUERY_PARAMS = 2;

  // Regex to extract Project ID and Instance ID from the path.
  // Expected format: /projects/{projectId}/instances/{instanceId}
  private static final Pattern BIGTABLE_PATH_PATTERN =
      Pattern.compile("/projects/([^/]+)/instances/([^/]+)");

  /**
   * Parses a Bigtable JDBC URL string into a BigtableJdbcUrl object.
   *
   * <p>The expected URL format is: {@code
   * "jdbc:bigtable:/projects/{projectId}/instances/{instanceId}?param1=val1&param2=val2"}
   *
   * @param url The JDBC URL string to parse.
   * @return A BigtableJdbcUrl object containing the project ID, instance ID, and query parameters.
   * @throws URISyntaxException if the URL is malformed.
   * @throws IllegalArgumentException if the URL does not match the expected Bigtable format.
   */
  public static BigtableJdbcUrl parse(String url) throws URISyntaxException {
    if (url == null) {
      throw new IllegalArgumentException("URL cannot be null.");
    }

    // Parse the URL using java.net.URI, which handles the "scheme:scheme-specific-part"
    // structure.
    URI jdbcUri = new URI(url);

    // Validate the scheme
    if (!JDBC_SCHEME.equals(jdbcUri.getScheme())) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid URL scheme. Expected '%s', but got '%s'.",
              JDBC_SCHEME, jdbcUri.getScheme()));
    }

    String schemeSpecificPart = jdbcUri.getRawSchemeSpecificPart();
    // schemeSpecificPart is never null
    if (!schemeSpecificPart.startsWith(BIGTABLE_SSP_PREFIX)) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid JDBC URL format. Scheme-specific part must start with '%s'.",
              BIGTABLE_SSP_PREFIX));
    }

    // Extract the part after "jdbc:bigtable:"
    String bigtablePathAndQuery = schemeSpecificPart.substring(BIGTABLE_SSP_PREFIX.length());

    // Use java.net.URI to parse the path and query parameters.
    // Prefix with a dummy scheme to make it a valid URI.
    URI internalUri = new URI("dummy:" + bigtablePathAndQuery);

    if (internalUri.getHost() != null || internalUri.getPort() != -1) {
      throw new IllegalArgumentException(
          "Host and port are not supported in Bigtable JDBC URL. Use "
              + "jdbc:bigtable:/projects/{projectId}/instances/{instanceId} format.");
    }

    String path = internalUri.getPath();
    if (path == null) {
      throw new IllegalArgumentException(
          "URL must include a path component (e.g., /projects/...).");
    }

    // Validate and extract project and instance IDs from the path
    Matcher matcher = BIGTABLE_PATH_PATTERN.matcher(path);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid Bigtable resource path format. Expected"
                  + " '/projects/{projectId}/instances/{instanceId}', but got '%s'.",
              path));
    }

    String projectId = matcher.group(1);
    String instanceId = matcher.group(2);

    validateProjectId(projectId);
    validateInstanceId(instanceId);

    // Extract query parameters
    ImmutableMap<String, String> queryParameters = parseQueryParameters(internalUri.getRawQuery());

    return new BigtableJdbcUrl(projectId, instanceId, queryParameters);
  }

  // https://docs.cloud.google.com/resource-manager/docs/creating-managing-projects#before_you_begin
  private static void validateProjectId(String projectId) {
    if (projectId.length() < 6 || projectId.length() > 30) {
      throw new IllegalArgumentException("Project ID must be between 6 and 30 characters.");
    }
    if (!projectId.matches("^[a-z][a-z0-9-]*[a-z0-9]$")) {
      throw new IllegalArgumentException(
          "Project ID must start with a letter, end with a letter or number, "
              + "and contain only lowercase letters, numbers, and hyphens.");
    }
    if (projectId.contains("google") || projectId.contains("ssl")) {
      throw new IllegalArgumentException("Project ID cannot contain 'google' or 'ssl'.");
    }
    if (projectId.contains("undefined") || projectId.contains("null")) {
      // Following recommendation as a strict check
      throw new IllegalArgumentException("Project ID should not contain 'undefined' or 'null'.");
    }
  }

  private static void validateInstanceId(String instanceId) {
    if (instanceId.length() < 6 || instanceId.length() > 33) {
      throw new IllegalArgumentException("Instance ID must be between 6 and 33 characters.");
    }
    if (!instanceId.matches("^[a-z][a-z0-9-]*[a-z0-9]$")) {
      throw new IllegalArgumentException(
          "Instance ID must start with a lowercase letter, end with a letter or number, "
              + "and contain only lowercase letters, numbers, and hyphens.");
    }
  }

  private static ImmutableMap<String, String> parseQueryParameters(String query) {
    if (query == null || query.isEmpty()) {
      return ImmutableMap.of();
    }
    Map<String, String> params = new LinkedHashMap<>();
    for (String param : query.split("&")) {
      String[] pair = param.split("=", MAX_QUERY_PARAMS);
      if (pair.length > 0) {
        try {
          String key =
              java.net.URLDecoder.decode(pair[0], java.nio.charset.StandardCharsets.UTF_8.name());
          String value =
              pair.length == MAX_QUERY_PARAMS
                  ? java.net.URLDecoder.decode(
                      pair[1], java.nio.charset.StandardCharsets.UTF_8.name())
                  : "";
          if (params.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate query parameter: " + key);
          }
          params.put(key, value);
        } catch (java.io.UnsupportedEncodingException e) {
          throw new RuntimeException("UTF-8 not supported", e);
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException(
              String.format("Duplicate or malformed query parameter: %s", param), e);
        }
      }
    }
    return ImmutableMap.copyOf(params);
  }

  /** Data class to hold the parsed components of the Bigtable JDBC URL. */
  public static class BigtableJdbcUrl {
    private final String projectId;
    private final String instanceId;
    private final ImmutableMap<String, String> queryParameters;

    public BigtableJdbcUrl(
        String projectId, String instanceId, ImmutableMap<String, String> queryParameters) {
      this.projectId = projectId;
      this.instanceId = instanceId;
      this.queryParameters = queryParameters;
    }

    public String getProjectId() {
      return projectId;
    }

    public String getInstanceId() {
      return instanceId;
    }

    /** Returns the query parameters. */
    public ImmutableMap<String, String> getQueryParameters() {
      return queryParameters;
    }

    @Override
    public String toString() {
      return String.format(
          "BigtableJdbcUrl{projectId='%s', instanceId='%s', queryParameters=%s}",
          projectId, instanceId, queryParameters);
    }
  }
}
