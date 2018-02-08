package ua.danit.jpa.parsing.executions;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.persistence.PersistenceException;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * Execution for delete operation.
 *
 * @author Andrey Minov
 */
public class DeleteExecution implements JpaExecution<Integer> {
  private static final String SQL_FORMAT = "DELETE FROM %s WHERE %s=?";

  private String sql;
  private int idType;
  private Method idGetter;

  private DeleteExecution(String sql, int idType, Method idGetter) {
    this.sql = sql;
    this.idType = idType;
    this.idGetter = idGetter;
  }

  /**
   * Create new delete statement execution from metadata.
   *
   * @param meta the meta for entity to persist
   * @return delete statement execution from metadata.
   */
  public static JpaExecution<Integer> fromMeta(EntityMeta meta) {
    String tableName =
        meta.getSchema() != null && !meta.getSchema().isEmpty() ? meta.getSchema() + "." + meta
            .getTableName() : meta.getTableName();


    ColumnMeta idColumn = meta.getId().getColumns().get(0);

    String sql = String.format(SQL_FORMAT, tableName, idColumn.getName());
    return new DeleteExecution(sql, idColumn.getSqlType(), idColumn.getGetter());
  }

  @Override
  public Integer execute(Connection connection, Object entity) {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Object id = idGetter.invoke(entity);
      statement.setObject(1, id, idType);
      return statement.executeUpdate();
    } catch (Exception e) {
      throw new PersistenceException("Unable to execute statement:" + sql, e);
    }
  }
}
