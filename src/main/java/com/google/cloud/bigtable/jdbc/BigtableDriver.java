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

package com.google.cloud.bigtable.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class BigtableDriver implements Driver {
  /** Url_Prefix for using this driver */
  private static final String URL_PREFIX = "jdbc:bigtable:/";
  /** MAJOR Version of the driver */
  private static final int MAJOR_VERSION = 1;
  /** Minor Version of the driver */
  private static final int MINOR_VERSION = 9;

  static {
    try {
      DriverManager.registerDriver(new BigtableDriver());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets Major Version of the Driver as static
   *
   * @return Major Version of the Driver as static
   */
  public static int getMajorVersionAsStatic() {
    return BigtableDriver.MAJOR_VERSION;
  }

  /**
   * Gets Minor Version of the Driver as static
   *
   * @return Minor Version of the Driver as static
   */
  public static int getMinorVersionAsStatic() {
    return BigtableDriver.MINOR_VERSION;
  }

  /** It returns the URL prefix for using BQDriver */
  public static String getURLPrefix() {
    return BigtableDriver.URL_PREFIX;
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!this.acceptsURL(url)) {
      throw new SQLException("Invalid URL: " + url + "\nDoes not start with " + getURLPrefix());
    }
    return new BigtableConnection(url, info);
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url.startsWith(getURLPrefix());
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    String[] supportedProps = new String[] {
      "app_profile_id",
      "universe_domain"
    };
    DriverPropertyInfo[] driverProps = new DriverPropertyInfo[supportedProps.length];
    for (int i = 0; i < supportedProps.length; i++) {
      String propName = supportedProps[i];
      DriverPropertyInfo dpi = new DriverPropertyInfo(propName, info.getProperty(propName));
      dpi.required = false;
      dpi.description = "Property for " + propName;
      driverProps[i] = dpi;
    }
    return driverProps;
  }

  @Override
  public int getMajorVersion() {
    return BigtableDriver.MAJOR_VERSION;
  }

  @Override
  public int getMinorVersion() {
    return BigtableDriver.MINOR_VERSION;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException("getParentLogger is not supported");
  }
}
