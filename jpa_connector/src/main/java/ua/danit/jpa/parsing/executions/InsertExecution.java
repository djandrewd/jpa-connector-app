package ua.danit.jpa.parsing.executions;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.GenerationType;
import javax.persistence.PersistenceException;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * JPA executions for insert statement.
 *
 * @author Andrey Minov
 */
public class InsertExecution implements JpaExecution<Integer> {
  private static final String SQL_FORMAT = "INSERT INTO %s (%s) VALUES (%s)";

  private String sql;
  private List<ColumnMeta> columnMetas;
  private Method idSetter;
  private boolean generatedId;

  private InsertExecution(String sql, List<ColumnMeta> columnMetas, Method idSetter,
                          boolean generatedId) {
    this.sql = sql;
    this.columnMetas = columnMetas;
    this.idSetter = idSetter;
    this.generatedId = generatedId;
  }

  /**
   * Create new insert statement execution from metadata.
   *
   * @param meta the meta for entity to persist
   * @return insert statement execution from metadata.
   */
  public static JpaExecution<Integer> fromMeta(EntityMeta meta) {
    String tableName =
        meta.getSchema() != null && !meta.getSchema().isEmpty() ? meta.getSchema() + "." + meta
            .getTableName() : meta.getTableName();

    List<ColumnMeta> columnMetas = new ArrayList<>(meta.getColumns().size());
    List<String> names = new ArrayList<>(meta.getColumns().size());

    for (ColumnMeta columnMeta : meta.getColumns()) {
      columnMetas.add(columnMeta);
      names.add(columnMeta.getName());
    }
    ColumnMeta idColumn = meta.getId().getColumns().get(0);
    boolean generatedId = meta.getId().getGenerationType() == GenerationType.IDENTITY;
    if (!generatedId) {
      columnMetas.add(idColumn);
      names.add(idColumn.getName());
    }
    String sql = String
        .format(SQL_FORMAT, tableName, String.join(",", names), Stream.generate(() -> "?")
                                                                      .limit(names.size())
                                                                      .collect(Collectors
                                                                          .joining(",")));
    return new InsertExecution(sql, columnMetas, idColumn.getSetter(), generatedId);
  }

  @Override
  public Integer execute(Connection connection, Object entity) {
    try (PreparedStatement statement = generatedId ? connection
        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : connection
        .prepareStatement(sql)) {

      for (int i = 0; i < columnMetas.size(); i++) {
        ColumnMeta meta = columnMetas.get(i);
        statement.setObject(i + 1, meta.getGetter().invoke(entity), meta.getSqlType());
      }
      int result = statement.executeUpdate();
      if (generatedId) {
        try (ResultSet set = statement.getGeneratedKeys()) {
          if (set.next()) {
            idSetter.invoke(entity, set.getObject(1));
          }
        }
      }
      return result;
    } catch (Exception e) {
      throw new PersistenceException("Error execute statement : " + sql, e);
    }
  }
}
