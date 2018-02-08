package ua.danit.jpa.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Types;
import javax.persistence.GenerationType;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * Test for JPA parsing entries.
 *
 * @author Andrey Minov
 */
public class EntityMetaParserTest {

  private EntityMeta entityMeta;

  @Before
  public void setUp() throws Exception {
    entityMeta = EntityMetaParser.parseEntity(User.class);
  }

  @Test
  public void testTableName() {
    assertEquals("users", entityMeta.getTableName());
  }

  @Test
  public void testEmptySchema() {
    assertEquals("", entityMeta.getSchema());
  }

  @Test
  public void testIdColumns() {
    assertNotNull("Entity id field must not be null", entityMeta.getId());
    assertEquals("Entity must have single id column", 1, entityMeta.getId().getColumns().size());
    assertEquals("Incorrect value of generation type!", GenerationType.IDENTITY, entityMeta.getId()
                                                                                           .getGenerationType());
    assertEquals("Incorrect value for generation strategy!", "", entityMeta.getId().getStrategy());

    ColumnMeta id = entityMeta.getId().getColumns().get(0);
    assertNotNull("Id column must not be empty!", id);
    assertEquals("Incorrect name of identity field!", "id", id.getName());
    assertEquals("Incorrent type of identity field!", Types.INTEGER, id.getSqlType());
  }

  @Test
  public void testTableColumns() {
    assertNotNull("Table columns value is null!", entityMeta.getColumns());
    assertEquals("Table columns size is incorrect!", 2, entityMeta.getColumns().size());

    ColumnMeta name = entityMeta.getColumns().get(0);
    assertEquals("Incorrect name for first column", "full_name", name.getName());
    assertEquals("Incorrect type for first column", Types.VARCHAR, name.getSqlType());

    ColumnMeta value = entityMeta.getColumns().get(1);
    assertEquals("Incorrect name for second column", "value", value.getName());
    assertEquals("Incorrect type for second column", Types.VARCHAR, value.getSqlType());
  }
}