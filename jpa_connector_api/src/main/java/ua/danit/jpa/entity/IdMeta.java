package ua.danit.jpa.entity;

import static ua.danit.jpa.utils.Utils.checkNonEmpty;

import java.util.List;
import javax.persistence.GenerationType;

/**
 * Meta information about unique identity value of entity.
 *
 * @author Andrey Minov
 */
public class IdMeta {
  private List<ColumnMeta> columns;
  private GenerationType generationType;
  private String strategy;

  /**
   * Instantiates a new Identity metadata with generation type AUTO.
   *
   * @param columns the columns used in identity key.
   * @throws IllegalArgumentException when columns list is empty.
   */
  public IdMeta(List<ColumnMeta> columns) {
    this(columns, GenerationType.AUTO, "");
  }

  /**
   * Instantiates a new Identity metadata.
   *
   * @param columns        the columns used in generation of column
   * @param generationType the generation type for identity field.
   * @param strategy       the strategy for identity field generation.
   * @throws IllegalArgumentException when columns list is empty
   */
  public IdMeta(List<ColumnMeta> columns, GenerationType generationType, String strategy) {
    checkNonEmpty("columns", columns);

    this.columns = columns;
    this.generationType = generationType;
    this.strategy = strategy;
  }

  public GenerationType getGenerationType() {
    return generationType;
  }

  public String getStrategy() {
    return strategy;
  }

  public List<ColumnMeta> getColumns() {
    return columns;
  }

  @Override
  public String toString() {
    return "IdMeta{" + "columns=" + columns + ", generationType=" + generationType + ", strategy='"
           + strategy + '\'' + '}';
  }
}
