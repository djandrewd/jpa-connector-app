package ua.danit.jpa.parsing.executions;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceException;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * JPA execution for select entry.
 *
 * @author Andrey Minov
 */
public class SelectExecution implements JpaExecution<Object> {
  private static final String SQL_FORMAT = "SELECT %s FROM %s WHERE %s=?";

  private String sql;
  private int idType;
  private Constructor<?> constructor;
  private Map<String, ColumnMeta> columns;

  private SelectExecution(String sql, int idType, Constructor<?> constructor,
                          Map<String, ColumnMeta> columns) {
    this.sql = sql;
    this.idType = idType;
    this.constructor = constructor;
    this.columns = columns;
  }

  /**
   * Create new select execution which select data from database, create new entry and return.
   *
   * @param meta the meta data for selected entity
   * @return new select execution which select data from database, create new entry and return.
   */
  public static JpaExecution<Object> fromMeta(EntityMeta meta) {

    List<String> names = new ArrayList<>(meta.getColumns().size());
    Map<String, ColumnMeta> columns = new HashMap<>();
    ColumnMeta idColumn = meta.getId().getColumns().get(0);
    columns.put(idColumn.getName(), idColumn);
    names.add(idColumn.getName());

    for (ColumnMeta columnMeta : meta.getColumns()) {
      names.add(columnMeta.getName());
      columns.put(columnMeta.getName(), columnMeta);
    }

    String tableName =
        meta.getSchema() != null && !meta.getSchema().isEmpty() ? meta.getSchema() + "." + meta
            .getTableName() : meta.getTableName();

    String sql = String.format(SQL_FORMAT, String.join(",", names), tableName, idColumn.getName());
    return new SelectExecution(sql, idColumn.getSqlType(), meta.getConstructor(), columns);
  }

  @Override
  public Object execute(Connection connection, Object id) {
    Object entity;
    try {
      entity = constructor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Unable to create new object entity!");
    }
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, id, idType);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        for (Map.Entry<String, ColumnMeta> entry : columns.entrySet()) {
          ColumnMeta columnMeta = entry.getValue();
          Object o = resultSet.getObject(entry.getKey());
          columnMeta.getSetter().invoke(entity, o);
        }
      }
      return entity;
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }
}
