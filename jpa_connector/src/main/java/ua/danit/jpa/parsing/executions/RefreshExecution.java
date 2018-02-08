package ua.danit.jpa.parsing.executions;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * Execution for entry refreshing.
 *
 * @author Andrey Minov
 */
public class RefreshExecution implements JpaExecution<Void> {

  private JpaExecution<Object> selectExecution;
  private ColumnMeta idColumn;
  private List<ColumnMeta> columnMetas;

  private RefreshExecution(JpaExecution<Object> selectExecution, ColumnMeta idColumn,
                           List<ColumnMeta> columnMetas) {
    this.selectExecution = selectExecution;
    this.idColumn = idColumn;
    this.columnMetas = columnMetas;
  }

  /**
   * Create new refresh execution which select entry from database and when update existed entity.
   *
   * @param meta the meta data for selected entity
   * @return new refresh execution which select entry from database and when update existed entity.
   */
  public static JpaExecution<Void> fromMeta(EntityMeta meta) {
    JpaExecution<Object> selectExecution = SelectExecution.fromMeta(meta);
    ColumnMeta idColumn = meta.getId().getColumns().get(0);
    List<ColumnMeta> columnMetas = new ArrayList<>();
    columnMetas.add(idColumn);
    columnMetas.addAll(meta.getColumns());
    return new RefreshExecution(selectExecution, idColumn, columnMetas);
  }

  @Override
  public Void execute(Connection connection, Object entity) {
    Object primaryKey;
    try {
      primaryKey = idColumn.getGetter().invoke(entity);
    } catch (Exception e) {
      throw new RuntimeException("Unable to get primary key object!", e);
    }
    Object newObject = selectExecution.execute(connection, primaryKey);
    try {
      for (ColumnMeta meta : columnMetas) {
        Object value = meta.getGetter().invoke(newObject);
        meta.getSetter().invoke(entity, value);
      }
      return null;
    } catch (Exception e) {
      throw new PersistenceException("Unable to run refresh statement!", e);
    }
  }
}
