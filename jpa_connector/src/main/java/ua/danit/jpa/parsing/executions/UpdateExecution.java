package ua.danit.jpa.parsing.executions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * JPA execution for update entry.
 *
 * @author Andrey Minov
 */
public class UpdateExecution implements JpaExecution<Integer> {
  private static final String SQL_FORMAT = "UPDATE %s SET %s WHERE %s=?";
  private static final String PARAM_FORMAT = "%s=?";

  private String sql;
  private List<ColumnMeta> columnMetas;
  private ColumnMeta idColumn;

  private UpdateExecution(String sql, List<ColumnMeta> columnMetas, ColumnMeta idColumn) {
    this.sql = sql;
    this.columnMetas = columnMetas;
    this.idColumn = idColumn;
  }

  /**
   * Create new update execution from entity metadata.
   *
   * @param meta the meta data for entity for update.
   * @return new update execution from entity metadata.
   */
  public static JpaExecution<Integer> fromMeta(EntityMeta meta) {
    String tableName =
        meta.getSchema() != null && !meta.getSchema().isEmpty() ? meta.getSchema() + "." + meta
            .getTableName() : meta.getTableName();

    List<ColumnMeta> columnMetas = new ArrayList<>(meta.getColumns().size());
    List<String> names = new ArrayList<>(meta.getColumns().size());

    for (ColumnMeta columnMeta : meta.getColumns()) {
      columnMetas.add(columnMeta);
      names.add(String.format(PARAM_FORMAT, columnMeta.getName()));
    }
    ColumnMeta idColumn = meta.getId().getColumns().get(0);
    String sql = String.format(SQL_FORMAT, tableName, String.join(",", names), idColumn.getName());
    return new UpdateExecution(sql, columnMetas, idColumn);
  }

  @Override
  public Integer execute(Connection connection, Object entity) {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int i = 0;
      for (; i < columnMetas.size(); i++) {
        ColumnMeta meta = columnMetas.get(i);
        statement.setObject(i + 1, meta.getGetter().invoke(entity), meta.getSqlType());
      }
      statement.setObject(i + 1, idColumn.getGetter().invoke(entity), idColumn.getSqlType());
      return statement.executeUpdate();
    } catch (Exception e) {
      throw new PersistenceException("Enable to run script: " + sql, e);
    }
  }
}
