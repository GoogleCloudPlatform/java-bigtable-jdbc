/*
 * Copyright 2026 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.bigtable.data.v2.models.sql.BoundStatement;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;
import com.google.protobuf.ByteString;
import java.sql.Types;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SqlTypeEnumTest {

  @Test
  public void testFromLabel() {
    assertEquals(SqlTypeEnum.STRING, SqlTypeEnum.fromLabel("STRING"));
    assertEquals(SqlTypeEnum.INT, SqlTypeEnum.fromLabel("INT"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromLabelInvalid() {
    SqlTypeEnum.fromLabel("INVALID");
  }

  @Test
  public void testFromJdbcType() {
    assertEquals(SqlTypeEnum.STRING, SqlTypeEnum.fromJdbcType(Types.VARCHAR));
    assertEquals(SqlTypeEnum.INT, SqlTypeEnum.fromJdbcType(Types.BIGINT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromJdbcTypeInvalid() {
    SqlTypeEnum.fromJdbcType(Types.OTHER);
  }

  @Test
  public void testFromSqlType() {
    assertEquals(SqlTypeEnum.STRING, SqlTypeEnum.fromSqlType(SqlType.string()));
    assertEquals(SqlTypeEnum.INT, SqlTypeEnum.fromSqlType(SqlType.int64()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromSqlTypeInvalid() {
    SqlTypeEnum.fromSqlType(SqlType.struct());
  }

  @Test
  public void testBind() {
    BoundStatement.Builder builder = mock(BoundStatement.Builder.class);
    SqlTypeEnum.STRING.bind(builder, "test", "value");
    verify(builder).setStringParam("test", "value");

    SqlTypeEnum.INT.bind(builder, "test", 123L);
    verify(builder).setLongParam("test", 123L);

    SqlTypeEnum.BOOL.bind(builder, "test", true);
    verify(builder).setBooleanParam("test", true);

    SqlTypeEnum.BYTES.bind(builder, "test", new byte[] {1, 2, 3});
    verify(builder).setBytesParam("test", ByteString.copyFrom(new byte[] {1, 2, 3}));

    SqlTypeEnum.FLOAT.bind(builder, "test", 1.23f);
    verify(builder).setFloatParam("test", 1.23f);

    SqlTypeEnum.DOUBLE.bind(builder, "test", 1.23);
    verify(builder).setDoubleParam("test", 1.23);

    Instant now = Instant.now();
    SqlTypeEnum.TIMESTAMP.bind(builder, "test", now);
    verify(builder).setTimestampParam("test", now);
  }
}
