package ua.danit.jpa.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Types;
import javax.persistence.GenerationType;

import org.junit.Ignore;
import org.junit.Test;
import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * Test for JPA parsing entries.
 *
 * @author Andrey Minov
 */
@Ignore
public class EntityMetaParserTest {

  @Test
  public void parseSimpleTable() throws NoSuchMethodException {
    EntityMeta entityMeta = null; /**Parse entity for class A*/
    assertNotNull(entityMeta);
    assertEquals("notA", entityMeta.getTableName());
    assertEquals("", entityMeta.getSchema());
    assertNotNull(entityMeta.getId());
    assertEquals(1, entityMeta.getId().getColumns().size());
    ColumnMeta id = entityMeta.getId().getColumns().get(0);
    assertNotNull(id);
    assertEquals(GenerationType.IDENTITY, entityMeta.getId().getGenerationType());
    assertEquals("", entityMeta.getId().getStrategy());

    assertEquals("id", id.getName());
    assertEquals(Types.INTEGER, id.getSqlType());
    assertNotNull(entityMeta.getColumns());
    assertEquals(2, entityMeta.getColumns().size());
    ColumnMeta name = entityMeta.getColumns().get(0);
    assertEquals("full_name", name.getName());
    assertEquals(Types.VARCHAR, name.getSqlType());
    ColumnMeta value = entityMeta.getColumns().get(1);
    assertEquals("value", value.getName());
    assertEquals(Types.VARCHAR, value.getSqlType());

  }

}