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

package com.google.cloud.bigtable.jdbc;

import static com.google.cloud.bigtable.jdbc.BigtableColumnType.fromSqlType;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.sql.Types;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;

@RunWith(JUnit4.class)
public class BigtableColumnTypeTest {

  @Test
  public void testFromSqlType() {
    assertEquals(BigtableColumnType.STRING, fromSqlType(SqlType.string()));
    assertEquals(BigtableColumnType.INT64, fromSqlType(SqlType.int64()));
    assertEquals(BigtableColumnType.FLOAT64, fromSqlType(SqlType.float64()));
    assertEquals(BigtableColumnType.FLOAT32, fromSqlType(SqlType.float32()));
    assertEquals(BigtableColumnType.BOOL, fromSqlType(SqlType.bool()));
    assertEquals(BigtableColumnType.BYTES, fromSqlType(SqlType.bytes()));
    assertEquals(BigtableColumnType.STRUCT, fromSqlType(SqlType.struct()));
    assertEquals(BigtableColumnType.DATE, fromSqlType(SqlType.date()));
    SqlType mockArrayType = mock(SqlType.class);
    when(mockArrayType.getCode()).thenReturn(SqlType.Code.ARRAY);
    assertEquals(BigtableColumnType.ARRAY, fromSqlType(mockArrayType));
    assertEquals(BigtableColumnType.TIMESTAMP, fromSqlType(SqlType.timestamp()));
    SqlType mockMapType = mock(SqlType.class);
    when(mockMapType.getCode()).thenReturn(SqlType.Code.MAP);
    assertEquals(BigtableColumnType.MAP, fromSqlType(mockMapType));
    assertEquals(BigtableColumnType.UNKNOWN, fromSqlType(null));
  }

  @Test
  public void testGetSqlTypeCode() {
    assertEquals(Types.VARCHAR, BigtableColumnType.STRING.getSqlTypeCode());
    assertEquals(Types.BIGINT, BigtableColumnType.INT64.getSqlTypeCode());
    assertEquals(Types.REAL, BigtableColumnType.FLOAT32.getSqlTypeCode());
    assertEquals(Types.REAL, BigtableColumnType.FLOAT64.getSqlTypeCode());
    assertEquals(Types.BOOLEAN, BigtableColumnType.BOOL.getSqlTypeCode());
    assertEquals(Types.LONGVARBINARY, BigtableColumnType.BYTES.getSqlTypeCode());
    assertEquals(Types.STRUCT, BigtableColumnType.STRUCT.getSqlTypeCode());
    assertEquals(Types.DATE, BigtableColumnType.DATE.getSqlTypeCode());
    assertEquals(Types.ARRAY, BigtableColumnType.ARRAY.getSqlTypeCode());
    assertEquals(Types.TIMESTAMP, BigtableColumnType.TIMESTAMP.getSqlTypeCode());
    assertEquals(Types.OTHER, BigtableColumnType.UNKNOWN.getSqlTypeCode());
  }

  @Test
  public void testGetJavaClassName() {
    assertEquals(String.class.getName(), BigtableColumnType.STRING.getJavaClassName());
    assertEquals(Long.class.getName(), BigtableColumnType.INT64.getJavaClassName());
    assertEquals(Float.class.getName(), BigtableColumnType.FLOAT32.getJavaClassName());
    assertEquals(Double.class.getName(), BigtableColumnType.FLOAT64.getJavaClassName());
    assertEquals(Boolean.class.getName(), BigtableColumnType.BOOL.getJavaClassName());
    assertEquals(com.google.protobuf.ByteString.class.getName(),
        BigtableColumnType.BYTES.getJavaClassName());
    assertEquals(com.google.cloud.bigtable.data.v2.models.sql.Struct.class.getName(),
        BigtableColumnType.STRUCT.getJavaClassName());
    assertEquals(java.util.Map.class.getName(), BigtableColumnType.MAP.getJavaClassName());
    assertEquals(com.google.cloud.Date.class.getName(), BigtableColumnType.DATE.getJavaClassName());
    assertEquals(java.util.List.class.getName(), BigtableColumnType.ARRAY.getJavaClassName());
    assertEquals(java.time.Instant.class.getName(),
        BigtableColumnType.TIMESTAMP.getJavaClassName());
    assertEquals(Object.class.getName(), BigtableColumnType.UNKNOWN.getJavaClassName());
  }
}
