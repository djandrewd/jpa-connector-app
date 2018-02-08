package ua.danit.jpa.entity;

import static ua.danit.jpa.utils.Utils.checkNonNull;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Meta information entity describing mapping to relational structure.
 *
 * @author Andrey Minov
 */
public class EntityMeta {
  private String tableName;
  private String schema;
  private String catalog;
  private Constructor<?> constructor;
  private IdMeta id;
  private List<ColumnMeta> columns;

  /**
   * Instantiates a new metadata for table entity.
   *
   * @param tableName   the table name
   * @param schema      the schema name
   * @param catalog     the catalog name
   * @param constructor the constructor for entity
   * @param id          the identity column for this table.
   * @param columns     the list of columns for this table.
   */
  public EntityMeta(String tableName, String schema, String catalog, Constructor<?> constructor,
                    IdMeta id, List<ColumnMeta> columns) {
    checkNonNull("constructor", constructor);
    checkNonNull("id", id);
    checkNonNull("tableName", tableName);

    this.constructor = constructor;
    this.tableName = tableName;
    this.schema = schema;
    this.catalog = catalog;
    this.id = id;
    this.columns = columns;
  }

  public Constructor<?> getConstructor() {
    return constructor;
  }

  public String getCatalog() {
    return catalog;
  }

  public String getTableName() {
    return tableName;
  }

  public String getSchema() {
    return schema;
  }

  public IdMeta getId() {
    return id;
  }

  public List<ColumnMeta> getColumns() {
    return columns;
  }

  @Override
  public String toString() {
    return "EntityMeta{" + "tableName='" + tableName + '\'' + ", schema='" + schema + '\''
           + ", catalog='" + catalog + '\'' + ", id=" + id + ", columns=" + columns + '}';
  }
}
