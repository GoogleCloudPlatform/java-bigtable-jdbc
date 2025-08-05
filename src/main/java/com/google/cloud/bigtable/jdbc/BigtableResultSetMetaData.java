package com.google.cloud.bigtable.jdbc;

import com.google.cloud.bigtable.data.v2.models.sql.ColumnMetadata;
import com.google.cloud.bigtable.data.v2.models.sql.SqlType;
import com.google.cloud.bigtable.jdbc.util.SqlTypeEnum;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.List;

public class BigtableResultSetMetaData implements ResultSetMetaData {
  private final List<ColumnMetadata> columns;
  private static final int DEFAULT_COL_DISPLAY_SIZE_FOR_VARIABLE_LENGTH_COLS = 50;

  public BigtableResultSetMetaData(List<ColumnMetadata> columns) {
    this.columns = columns;
  }

  @Override
  public int getColumnCount() throws SQLException {
    return columns.size();
  }

  @Override
  public boolean isAutoIncrement(int column) throws SQLException {
    return false;
  }

  @Override
  public boolean isCaseSensitive(int column) throws SQLException {
    if (column < 1 || column > columns.size()) {
      throw new SQLException("Invalid column index: " + column);
    }
    SqlType<?> type = columns.get(column - 1).type();
    return SqlType.string().equals(type);
  }

  @Override
  public boolean isSearchable(int column) throws SQLException {
    return true;
  }

  @Override
  public boolean isCurrency(int column) throws SQLException {
    return false;
  }

  @Override
  public int isNullable(int column) throws SQLException {
    return ResultSetMetaData.columnNullableUnknown;
  }

  @Override
  public boolean isSigned(int column) throws SQLException {
    if (column < 1 || column > columns.size()) {
      throw new SQLException("Invalid column index: " + column);
    }

    SqlTypeEnum typeEnum;
    try {
      typeEnum = SqlTypeEnum.fromSqlType(columns.get(column - 1).type());
    } catch (IllegalArgumentException e) {
      return false;
    }

    switch (typeEnum.getSqlTypeCode()) {
      case Types.TINYINT:
      case Types.SMALLINT:
      case Types.INTEGER:
      case Types.BIGINT:
      case Types.FLOAT:
      case Types.REAL:
      case Types.DOUBLE:
      case Types.NUMERIC:
      case Types.DECIMAL:
        return true;
      default:
        return false;
    }
  }

  @Override
  public int getColumnDisplaySize(int column) throws SQLException {
    int colType = getColumnType(column);
    switch (colType) {
      case Types.ARRAY:
        return DEFAULT_COL_DISPLAY_SIZE_FOR_VARIABLE_LENGTH_COLS;
      case Types.BOOLEAN:
        return 5;
      case Types.BINARY:
      case Types.NVARCHAR:
        int binaryLength = getPrecision(column);
        return binaryLength == 0 ? DEFAULT_COL_DISPLAY_SIZE_FOR_VARIABLE_LENGTH_COLS : binaryLength;
      case Types.REAL:
        return 7;
      case Types.FLOAT:
      case Types.DOUBLE:
      case Types.NUMERIC:
        return 14;
      case Types.TIMESTAMP:
        return 16;
      default:
        return 10;
    }
  }

  @Override
  public String getColumnLabel(int column) throws SQLException {
    return getColumnName(column);
  }

  @Override
  public String getColumnName(int column) throws SQLException {
    return columns.get(column - 1).name();
  }

  @Override
  public String getSchemaName(int column) throws SQLException {
    return "";
  }

  @Override
  public int getPrecision(int column) throws SQLException {
    int colType = getColumnType(column);
    switch (colType) {
      case Types.BOOLEAN:
        return 1;
      case Types.DATE:
      case Types.BIGINT:
      case Types.INTEGER:
        return 10;
      case Types.REAL:
        return 7;
      case Types.FLOAT:
      case Types.DOUBLE:
      case Types.NUMERIC:
        return 14;
      case Types.TIMESTAMP:
        return 24;
      default:
        // For column types with variable size, such as text columns, we should return the length
        // in characters. We could try to fetch it from INFORMATION_SCHEMA, but that would mean
        // parsing the SQL statement client side in order to figure out which column it actually
        // is. For now we just return the default column display size.
        return DEFAULT_COL_DISPLAY_SIZE_FOR_VARIABLE_LENGTH_COLS;
    }
  }

  @Override
  public int getScale(int column) throws SQLException {
    int colType = getColumnType(column);
    if (colType == Types.REAL) {
      return 7;
    }
    if (colType == Types.DOUBLE || colType == Types.NUMERIC) {
      return 15;
    }
    return 0;
  }

  @Override
  public String getTableName(int column) throws SQLException {
    return "";
  }

  @Override
  public String getCatalogName(int column) throws SQLException {
    return "";
  }

  @Override
  public int getColumnType(int column) throws SQLException {
    BigtableColumnType type = BigtableColumnType.fromSqlType(columns.get(column - 1).type());
    return type.getSqlTypeCode();
  }

  @Override
  public String getColumnTypeName(int column) throws SQLException {
    return columns.get(column - 1).type().toString();
  }

  @Override
  public boolean isReadOnly(int column) throws SQLException {
    return true;
  }

  @Override
  public boolean isWritable(int column) throws SQLException {
    return false;
  }

  @Override
  public boolean isDefinitelyWritable(int column) throws SQLException {
    return false;
  }

  @Override
  public String getColumnClassName(int column) throws SQLException {
    BigtableColumnType type = BigtableColumnType.fromSqlType(columns.get(column - 1).type());
    return type.getJavaClassName();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException("unwrap is not supported");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException("isWrapperFor is not supported");
  }
}
