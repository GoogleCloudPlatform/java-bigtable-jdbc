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

package com.google.cloud.bigtable.jdbc.util;

import com.google.cloud.Date;
import com.google.cloud.bigtable.data.v2.models.sql.BoundStatement;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;
import com.google.protobuf.ByteString;

import java.sql.Types;
import java.time.*;
import java.util.Arrays;
import java.util.List;

public enum SqlTypeEnum {
  BYTES(SqlType.bytes(), Types.BINARY, byte[].class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setBytesParam(name, ByteString.copyFrom((byte[]) value));
    }
  },
  STRING(SqlType.string(), Types.VARCHAR, String.class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setStringParam(name, (String) value);
    }
  },
  INT(SqlType.int64(), Types.BIGINT, Long.class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setLongParam(name, ((Number) value).longValue());
    }
  },
  BOOL(SqlType.bool(), Types.BOOLEAN, Boolean.class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setBooleanParam(name, (Boolean) value);
    }
  },
  FLOAT(SqlType.float32(), Types.REAL, Float.class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setFloatParam(name, (Float) value);
    }
  },
  DOUBLE(SqlType.float64(), Types.DOUBLE, Double.class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setDoubleParam(name, (Double) value);
    }
  },
  DATE(SqlType.date(), Types.DATE, Date.class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setDateParam(name, (Date) value);
    }
  },
  TIMESTAMP(SqlType.timestamp(), Types.TIMESTAMP, Instant.class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      bound.setTimestampParam(name, (Instant) value);
    }
  },
  ARRAY(SqlType.arrayOf(SqlType.string()), Types.ARRAY, Object[].class.getName()) {
    public void bind(BoundStatement.Builder bound, String name, Object value) {
      if (value == null) {
        bound.setListParam(name, null, (SqlType.Array<Object>) getSqlType());
        return;
      }
      Object[] array = (Object[]) value;
      List<Object> list = Arrays.asList(array);
      bound.setListParam(name, list, (SqlType.Array<Object>) getSqlType());
    }
  };

  private final SqlType<?> sqlType;
  private final int sqlTypeCode;
  private final String javaClassName;

  SqlTypeEnum(SqlType<?> sqlType, int sqlTypeCode, String javaClassName) {
    this.sqlType = sqlType;
    this.sqlTypeCode = sqlTypeCode;
    this.javaClassName = javaClassName;
  }

  public SqlType<?> getSqlType() {
    return sqlType;
  }

  public int getSqlTypeCode() {
    return sqlTypeCode;
  }

  public String getJavaClassName() {
    return javaClassName;
  }

  public abstract void bind(BoundStatement.Builder bound, String name, Object value);

  public static SqlTypeEnum fromLabel(String label) {
    return Arrays.stream(values())
      .filter(e -> e.name().equalsIgnoreCase(label))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown SqlType: " + label));
  }

  public static SqlTypeEnum fromJdbcType(int jdbcType) {
    return Arrays.stream(values())
      .filter(e -> e.getSqlTypeCode() == jdbcType)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported JDBC type: " + jdbcType));
  }

  public static SqlTypeEnum fromSqlType(SqlType<?> sqlType) {
    for (SqlTypeEnum typeEnum : values()) {
      if (typeEnum.getSqlType().equals(sqlType)) {
        return typeEnum;
      }
    }
    throw new IllegalArgumentException("Unknown SqlType: " + sqlType);
  }
}
