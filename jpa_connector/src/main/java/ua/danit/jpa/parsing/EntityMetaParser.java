package ua.danit.jpa.parsing;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;
import ua.danit.jpa.entity.IdMeta;

/**
 * Parser for parsing information about entities.
 *
 * @author Andrey Minov
 */
public class EntityMetaParser {

  private static final Map<Class<?>, Integer> TYPES_MAP;

  static {
    TYPES_MAP = new HashMap<>();
    TYPES_MAP.put(Integer.class, Types.INTEGER);
    TYPES_MAP.put(int.class, Types.INTEGER);
    TYPES_MAP.put(Long.class, Types.BIGINT);
    TYPES_MAP.put(long.class, Types.BIGINT);
    TYPES_MAP.put(Double.class, Types.DOUBLE);
    TYPES_MAP.put(double.class, Types.DOUBLE);
    TYPES_MAP.put(char.class, Types.CHAR);
    TYPES_MAP.put(BigDecimal.class, Types.DECIMAL);
    TYPES_MAP.put(String.class, Types.VARCHAR);
    TYPES_MAP.put(Date.class, Types.TIMESTAMP);
    TYPES_MAP.put(Timestamp.class, Types.TIMESTAMP);
    TYPES_MAP.put(java.sql.Date.class, Types.DATE);
    TYPES_MAP.put(Time.class, Types.TIME);
  }

  /**
   * Parse new persisted entity metadata from class using reflection.
   *
   * @param entityClazz the entity clazz
   * @return the metainformation about persisted entity.
   * @throws NoSuchMethodException when exists no getter or setter existed for some of field.
   */
  public static EntityMeta parseEntity(Class<?> entityClazz) throws NoSuchMethodException {
    if (!entityClazz.isAnnotationPresent(Entity.class)) {
      return null;
    }
    String tableName = entityClazz.getSimpleName();
    String schema = null;
    if (entityClazz.isAnnotationPresent(Table.class)) {
      Table table = entityClazz.getAnnotation(Table.class);
      if (!table.name().isEmpty()) {
        tableName = table.name();
      }
      schema = table.schema();
      //Both indexes and uniques are used in DDL generation and do not supported at this phase.
    }
    Constructor<?> constructor = entityClazz.getConstructor();
    List<ColumnMeta> idColumns = new ArrayList<>();
    List<ColumnMeta> columns = new ArrayList<>();
    GenerationType generationType = GenerationType.AUTO;
    String strategy = "";
    for (Field field : entityClazz.getDeclaredFields()) {
      if (!TYPES_MAP.containsKey(field.getType())) {
        throw new IllegalArgumentException("Not supported class for mapping:" + field.getType());
      }
      String name = field.getName();
      int type = TYPES_MAP.get(field.getType());
      Method getter = getGetter(entityClazz, field);
      Method setter = getSetter(entityClazz, field);
      ColumnMeta columnMeta;
      if (field.isAnnotationPresent(Column.class)) {
        Column column = field.getDeclaredAnnotation(Column.class);
        if (!column.name().isEmpty()) {
          name = column.name();
        }
        columnMeta = new ColumnMeta(name, tableName, type, field
            .getType(), field, getter, setter, column.nullable(), column.insertable(), column
            .updatable(), column.length(), column.scale());
      } else {
        columnMeta = new ColumnMeta(name, tableName, type, field.getType(), getter, setter, field);
      }
      if (!field.isAnnotationPresent(Id.class)) {
        columns.add(columnMeta);
      } else {
        idColumns.add(columnMeta);
      }

      if (field.isAnnotationPresent(GeneratedValue.class)) {
        GeneratedValue gv = field.getDeclaredAnnotation(GeneratedValue.class);
        generationType = gv.strategy();
        strategy = gv.generator();
      }
    }
    if (idColumns.isEmpty()) {
      throw new IllegalArgumentException("Table entity class must have @Id field!");
    }
    IdMeta id = new IdMeta(idColumns, generationType, strategy);
    return new EntityMeta(tableName, schema, null, constructor, id, columns);
  }

  /**
   * Gets sql type one of {@link Types} by class name.
   *
   * @param clazz the clazz type for column.
   * @return one of {@link Types} mapped on class provided.
   * @throws IllegalArgumentException when type has not SQL mapping.
   */
  public static int getSqlType(Class<?> clazz) {
    return Optional.ofNullable(TYPES_MAP.get(clazz)).orElseThrow(() -> new IllegalArgumentException(
        "Class " + clazz + " is not supported as SQL type!"));
  }

  private static Method getGetter(Class<?> entityClazz, Field field) throws NoSuchMethodException {
    String name =
        "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
    return entityClazz.getMethod(name);
  }

  private static Method getSetter(Class<?> entityClazz, Field field) throws NoSuchMethodException {
    String name =
        "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
    return entityClazz.getMethod(name, field.getType());
  }
}
