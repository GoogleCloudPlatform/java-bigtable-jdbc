package com.google.cloud.bigtable.jdbc.util;

public class SqlParser {

  /**
   * Replaces '?' placeholders with named parameters @param1, @param2, ...
   * Ignores placeholders inside quotes, triple quotes, or comments.
   *
   * @param sql        the SQL string with '?' placeholders
   * @param paramCount expected number of parameters
   * @return SQL with named parameters
   */
  public static String replacePlaceholdersWithNamedParams(String sql, int paramCount) {
    StringBuilder parsed = new StringBuilder();
    int paramIndex = 1;

    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean inTripleQuote = false;

    boolean inSingleLineComment = false;
    boolean inMultiLineComment = false;

    int length = sql.length();

    for (int i = 0; i < length; i++) {
      char c = sql.charAt(i);

      // Check for start/end of multi-line comments /* ... */
      if (!inSingleQuote && !inDoubleQuote && !inTripleQuote && !inSingleLineComment) {
        if (!inMultiLineComment && c == '/' && i + 1 < length && sql.charAt(i + 1) == '*') {
          inMultiLineComment = true;
          parsed.append("/*");
          i++;
          continue;
        } else if (inMultiLineComment && c == '*' && i + 1 < length && sql.charAt(i + 1) == '/') {
          inMultiLineComment = false;
          parsed.append("*/");
          i++;
          continue;
        }
      }

      if (inMultiLineComment) {
        // Inside multi-line comment - just append chars
        parsed.append(c);
        continue;
      }

      // Check for start of single-line comment (-- or #), only if not in quotes or triple quote
      if (!inSingleQuote && !inDoubleQuote && !inTripleQuote && !inSingleLineComment) {
        if (c == '-' && i + 1 < length && sql.charAt(i + 1) == '-') {
          inSingleLineComment = true;
          parsed.append("--");
          i++;
          continue;
        } else if (c == '#') {
          inSingleLineComment = true;
          parsed.append('#');
          continue;
        }
      }

      // End single line comment at line break
      if (inSingleLineComment) {
        parsed.append(c);
        if (c == '\n' || c == '\r') {
          inSingleLineComment = false;
        }
        continue;
      }

      // Handle triple quotes (""" ... """)
      if (!inSingleQuote && !inDoubleQuote && !inSingleLineComment && !inMultiLineComment) {
        if (!inTripleQuote && c == '"' && i + 2 < length
          && sql.charAt(i + 1) == '"' && sql.charAt(i + 2) == '"') {
          inTripleQuote = true;
          parsed.append("\"\"\"");
          i += 2;
          continue;
        } else if (inTripleQuote && c == '"' && i + 2 < length
          && sql.charAt(i + 1) == '"' && sql.charAt(i + 2) == '"') {
          inTripleQuote = false;
          parsed.append("\"\"\"");
          i += 2;
          continue;
        }
      }

      if (inTripleQuote) {
        // Inside triple quote, just append
        parsed.append(c);
        continue;
      }

      // Handle single quotes (') with escapes
      if (!inDoubleQuote && !inTripleQuote && !inSingleLineComment && !inMultiLineComment) {
        if (c == '\'') {
          // Check for escaped quote: '' inside single quote string
          if (inSingleQuote && i + 1 < length && sql.charAt(i + 1) == '\'') {
            parsed.append("''");
            i++; // skip next quote
            continue;
          }
          inSingleQuote = !inSingleQuote;
          parsed.append(c);
          continue;
        }
      }

      // Handle double quotes (") with escapes
      if (!inSingleQuote && !inTripleQuote && !inSingleLineComment && !inMultiLineComment) {
        if (c == '"') {
          // Check for escaped double quote \" inside double quotes
          if (inDoubleQuote && i > 0 && sql.charAt(i - 1) == '\\') {
            parsed.append(c);
            continue;
          }
          inDoubleQuote = !inDoubleQuote;
          parsed.append(c);
          continue;
        }
      }

      // Replace placeholder '?' only if outside any quote or comment
      if (c == '?' && !inSingleQuote && !inDoubleQuote && !inTripleQuote && !inSingleLineComment && !inMultiLineComment) {
        if (paramIndex > paramCount) {
          throw new IllegalArgumentException("More placeholders than paramCount");
        }
        parsed.append("@param").append(paramIndex++);
      } else {
        parsed.append(c);
      }
    }

    if (paramIndex <= paramCount) {
      throw new IllegalArgumentException("Fewer placeholders than paramCount");
    }

    return parsed.toString();
  }
}


