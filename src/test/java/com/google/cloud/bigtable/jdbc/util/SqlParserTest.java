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
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SqlParserTest {

  @Test
  public void testReplacePlaceholdersWithNamedParams() {
    String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
    String expected = "SELECT * FROM users WHERE id = @param1 AND name = @param2";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 2));
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsNoPlaceholders() {
    String sql = "SELECT * FROM users";
    String expected = "SELECT * FROM users";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 0));
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsInQuotes() {
    String sql = "SELECT * FROM users WHERE name = '?'";
    String expected = "SELECT * FROM users WHERE name = '?'";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 0));
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsInSingleLineComment() {
    String sql = "SELECT * FROM users -- WHERE id = ?";
    String expected = "SELECT * FROM users -- WHERE id = ?";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 0));
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsInMultiLineComment() {
    String sql = "SELECT * FROM users /* WHERE id = ? */";
    String expected = "SELECT * FROM users /* WHERE id = ? */";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 0));
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsMismatchedParamCount() {
    String sql = "SELECT * FROM users WHERE id = ?";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          SqlParser.replacePlaceholdersWithNamedParams(sql, 2);
        });
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsMorePlaceholdersThanParamCount() {
    String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          SqlParser.replacePlaceholdersWithNamedParams(sql, 1);
        });
  }

  @Test
  public void parse_basicStatement() {
    String expectedGoogleSqlStatement = "SELECT 1";

    String actualGoogleSqlStatement = SqlParser.replacePlaceholdersWithNamedParams("SELECT 1", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void parse_basicAlphabetic() {
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = 'test'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'test'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void parse_basicNumeric() {
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = 1";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 1", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsSingleQuote() {
    // JDBC uses '' to escape a single quote. GoogleSQL uses \'.
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = 'O'Malley'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'O''Malley'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsDoubleQuote() {
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = 'He said \"Hi\"'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'He said \"Hi\"'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsSingleAndDoubleQuotes() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = 'It\'s a \"great\" day'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'It''s a \"great\" day'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsDoubleQuoteWithInnerSingleQuote() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = 'She whispered: \'It\'s a secret\''";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'She whispered: ''It''s a secret'''", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsLiteralSingleAndDoubleQuotesAsData() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = 'He likes \'scifi\' and \"fantasy\"'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'He likes ''scifi'' and \"fantasy\"'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsBackslash() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = 'A backslash \\ here'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'A backslash \\ here'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_windowsPathWithBackslashes() {
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = 'C:\\Users\\Ann'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'C:\\Users\\Ann'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsStringWithWrappedSingleQuotes() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = '\'Single quotes inside\''";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = '''Single quotes inside'''", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_containsStringWithWrappedDoubleQuotes() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = '\"Double quotes inside\"'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = '\"Double quotes inside\"'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_allSpecialCharsCombined() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = 'Mix \'em up \"with\" \\ slashes'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'Mix ''em up \"with\" \\ slashes'", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_specialWhitespaceNewline() {
    String expectedGoogleSqlStatement =
        "SELECT * FROM my_table WHERE my_column = 'New line \n here'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'New line \n here'", 0);
    // Until we have a specific reason to, let's not do special translation.
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_specialWhitespaceTab() {
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = 'Tab \t here'";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = 'Tab \t here'", 0);
    // Until we have a specific reason to, let's not do special translation.
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_emptyString() {
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = ''";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = ''", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void render_nullInput() {
    String expectedGoogleSqlStatement = "SELECT * FROM my_table WHERE my_column = NULL";
    String actualGoogleSqlStatement =
        SqlParser.replacePlaceholdersWithNamedParams(
            "SELECT * FROM my_table WHERE my_column = NULL", 0);
    assertEquals(expectedGoogleSqlStatement, actualGoogleSqlStatement);
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsWithSingleQuoteInDoubleQuotes() {
    String sql = "SELECT * FROM users WHERE name = \"O'Malley\" AND notes = ?";
    String expected = "SELECT * FROM users WHERE name = \"O'Malley\" AND notes = @param1";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 1));
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsWithEscapedDoubleQuotes() {
    String sql = "SELECT * FROM users WHERE name = \"O\\\"Malley\" AND notes = ?";
    String expected = "SELECT * FROM users WHERE name = \"O\\\"Malley\" AND notes = @param1";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 1));
  }

  @Test
  public void testReplacePlaceholdersWithNamedParamsWithHashComment() {
    String sql = "SELECT * FROM users # WHERE id = ?";
    String expected = "SELECT * FROM users # WHERE id = ?";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 0));
  }

  @Test
  public void testMixedQuotesAndComments() {
    String sql =
        "SELECT * FROM users -- comment with ?\n"
            + "WHERE name = '?' AND notes = \"/* ? */\" AND extra = ?;";
    String expected =
        "SELECT * FROM users -- comment with ?\n"
            + "WHERE name = '?' AND notes = \"/* ? */\" AND extra = @param1;";
    assertEquals(expected, SqlParser.replacePlaceholdersWithNamedParams(sql, 1));
  }
}
