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

import com.google.cloud.bigtable.data.v2.models.sql.SqlType;

import java.sql.Types;

public enum BigtableColumnType {
  STRING, INT64, FLOAT32, FLOAT64, BOOL, BYTES, UNKNOWN, STRUCT, DATE, ARRAY, TIMESTAMP, MAP;

  public static BigtableColumnType fromSqlType(SqlType sqlType) {
    if (sqlType == null) {
      return UNKNOWN;
    }
    switch (sqlType.getCode()) {
      case STRING:
        return STRING;
      case INT64:
        return INT64;
      case FLOAT64:
        return FLOAT64;
      case FLOAT32:
        return FLOAT32;
      case BOOL:
        return BOOL;
      case BYTES:
        return BYTES;
      case STRUCT:
        return STRUCT;
      case DATE:
        return DATE;
      case ARRAY:
        return ARRAY;
      case TIMESTAMP:
        return TIMESTAMP;
      case MAP:
        return MAP;
      default:
        return UNKNOWN;
    }
  }

  public int getSqlTypeCode() {
    switch (this) {
      case STRING:
        return Types.VARCHAR;
      case INT64:
        return Types.BIGINT;
      case FLOAT32:
      case FLOAT64:
        return Types.REAL;
      case BOOL:
        return Types.BOOLEAN;
      case BYTES:
        return Types.LONGVARBINARY;
      case STRUCT:
        return Types.STRUCT;
      case DATE:
        return Types.DATE;
      case ARRAY:
        return Types.ARRAY;
      case TIMESTAMP:
        return Types.TIMESTAMP;
      case UNKNOWN:
      default:
        return Types.OTHER;
    }
  }

  public String getJavaClassName() {
    switch (this) {
      case STRING:
        return String.class.getName();
      case INT64:
        return Long.class.getName();
      case FLOAT32:
        return Float.class.getName();
      case FLOAT64:
        return Double.class.getName();
      case BOOL:
        return Boolean.class.getName();
      case BYTES:
        return com.google.protobuf.ByteString.class.getName();
      case STRUCT:
        return com.google.cloud.bigtable.data.v2.models.sql.Struct.class.getName();
      case MAP:
        return java.util.Map.class.getName();
      case DATE:
        return com.google.cloud.Date.class.getName();
      case ARRAY:
        return java.util.List.class.getName();
      case TIMESTAMP:
        return java.time.Instant.class.getName();
      case UNKNOWN:
      default:
        return Object.class.getName();
    }
  }
}
