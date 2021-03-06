package ua.danit.jpa.entity;

import static ua.danit.jpa.utils.Utils.checkNonEmpty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ua.danit.jpa.utils.Utils;

/**
 * Meta information about column in relational structure.
 *
 * @author Andrey Minov
 */
public class ColumnMeta {
  private String name;
  private int sqlType;
  private Class<?> type;

  private Field field;
  private Method getter;
  private Method setter;
  /**
   * (Optional) Whether the database column is nullable.
   */
  private boolean nullable;

  /**
   * (Optional) Whether the column is included in SQL INSERT
   * statements generated by the persistence provider.
   */
  private boolean insertable;

  /**
   * (Optional) Whether the column is included in SQL UPDATE
   * statements generated by the persistence provider.
   */
  private boolean updatable;

  /**
   * (Optional) The name of the table that contains the column.
   * If absent the column is assumed to be in the primary table.
   */
  private String table;

  /**
   * (Optional) The column length. (Applies only if a
   * string-valued column is used.)
   */
  private int length;

  /**
   * (Optional) The scale for a decimal (exact numeric) column.
   * (Applies only if a decimal column is used.)
   */
  private int scale;

  /**
   * Instantiates a new Column meta.
   *
   * @param name    the name of column
   * @param table   the name of the table
   * @param sqlType the sql type one of {@link java.sql.Types}
   * @param type    the class type for fields
   * @param getter  the getter for field corresponding to column
   * @param setter  the setter for field corresponding to column
   * @param field   corresponding to this column
   * @throws IllegalArgumentException when name of the column is empty.
   */
  public ColumnMeta(String name, String table, int sqlType, Class<?> type, Method getter,
                    Method setter, Field field) {
    this(name, table, sqlType, type, field, getter, setter, true, true, true, 256, 0);
  }

  /**
   * Instantiates a new Column meta.
   *
   * @param name       the name of column
   * @param table      the name of the table
   * @param sqlType    the sql type one of {@link java.sql.Types}
   * @param type       the class type for fields
   * @param field      field corresponding to this column
   * @param getter     the getter for field corresponding to column
   * @param setter     the setter for field corresponding to column
   * @param nullable   whether the database column is nullable.
   * @param insertable whether the column is included in SQL INSERT
   *                   statements generated by the persistence provider.
   * @param updatable  whether the column is included in SQL UPDATE
   *                   statements generated by the persistence provider.
   * @param length     the column length. (Applies only if a
   *                   string-valued column is used.)
   * @param scale      the scale for a decimal (exact numeric) column.
   *                   (Applies only if a decimal column is used.)
   * @throws IllegalArgumentException when name of column is empty.
   */
  public ColumnMeta(String name, String table, int sqlType, Class<?> type, Field field,
                    Method getter, Method setter, boolean nullable, boolean insertable,
                    boolean updatable, int length, int scale) {
    checkNonEmpty("name", name);

    this.field = field;
    this.name = name;
    this.sqlType = sqlType;
    this.type = type;
    this.getter = getter;
    this.setter = setter;
    this.nullable = nullable;
    this.insertable = insertable;
    this.updatable = updatable;
    this.table = table;
    this.length = length;
    this.scale = scale;
  }

  public String getName() {
    return name;
  }

  public int getSqlType() {
    return sqlType;
  }

  public Method getGetter() {
    return getter;
  }

  public Method getSetter() {
    return setter;
  }

  public boolean isNullable() {
    return nullable;
  }

  public boolean isInsertable() {
    return insertable;
  }

  public boolean isUpdatable() {
    return updatable;
  }

  public String getTable() {
    return table;
  }

  public int getLength() {
    return length;
  }

  public int getScale() {
    return scale;
  }

  public Class<?> getType() {
    return type;
  }

  public Field getField() {
    return field;
  }

  @Override
  public String toString() {
    return "ColumnMeta{" + "name='" + name + '\'' + ", sqlType=" + sqlType + ", type=" + type
           + ", field=" + field + ", getter=" + getter + ", setter=" + setter + ", nullable="
           + nullable + ", insertable=" + insertable + ", updatable=" + updatable + ", table='"
           + table + '\'' + ", length=" + length + ", scale=" + scale + '}';
  }
}
