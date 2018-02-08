package ua.danit.jpa.sessions;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.TransactionRequiredException;

import org.junit.Before;
import org.junit.Test;
import ua.danit.jpa.parsing.JpaPersistenceMetaContext;

/**
 * Tests for EntitySession implementation.
 *
 * @author Andrey Minov
 */
public class JPASessionTest {
  private EntityManager entityManager;
  private Connection connection;
  private ResultSet set;
  private PreparedStatement statement;
  private Car car;

  @Before
  public void setUp() throws Exception {
    connection = mock(Connection.class);
    set = mock(ResultSet.class);
    statement = mock(PreparedStatement.class);

    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
        .thenReturn(statement);
    when(statement.getGeneratedKeys()).thenReturn(set);
    when(statement.executeQuery()).thenReturn(set);
    when(set.getObject(1)).thenReturn(1L);

    JpaPersistenceMetaContext context = new JpaPersistenceMetaContext();
    context.register(Car.class);
    entityManager = new JpaSession(connection, context, mock(EntityManagerFactory.class));
    car = createCar();
  }

  @Test
  public void testPersistEntity() throws Exception {
    // Persist entry
    entityManager.persist(car);

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    verify(set, times(1)).next();
    // Assert id is generated
    assertEquals("Id value is not correct!", 1, car.getId());
  }


  @Test
  public void testRemoveEntity() throws Exception {
    entityManager.remove(car);
    // Verify object is removed
    verify(connection, times(1)).prepareStatement("DELETE FROM car WHERE id=?");
  }

  @Test
  public void testMergeNotExistedEntity() throws Exception {
    // Persist entry
    entityManager.merge(car);

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    verify(set, times(1)).next();
    // Assert id is generated
    assertEquals("Id value is not correct!", 1, car.getId());
  }


  @Test
  public void testMergeExistedEntity() throws Exception {
    entityManager.setFlushMode(FlushModeType.COMMIT);
    entityManager.persist(car);
    entityManager.merge(car);
    entityManager.flush();

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    // And updated.
    verify(connection, times(1)).prepareStatement("UPDATE car SET name=? WHERE id=?");
    verify(set, times(1)).next();
  }


  @Test(expected = EntityNotFoundException.class)
  public void testRefreshNotExisted() throws Exception {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    entityManager.refresh(car);
  }

  @Test
  public void testRefreshEntity() throws Exception {
    when(set.getObject("id")).thenReturn(1);
    when(set.getObject("name")).thenReturn("new name");
    when(set.next()).thenReturn(true);

    entityManager.setFlushMode(FlushModeType.COMMIT);
    entityManager.persist(car);
    entityManager.refresh(car);
    entityManager.flush();

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    // And updated.
    verify(connection, times(1)).prepareStatement("SELECT id,name FROM car WHERE id=?");
    verify(set, times(2)).next();

    // Assert selected values.
    assertEquals("Incorrect selected car id!", 1, car.getId());
    assertEquals("Incorrect selected car name", "new name", car.getName());
  }


  @Test
  public void testFindEntityInDatabase() throws Exception {
    when(set.getObject("id")).thenReturn(1);
    when(set.getObject("name")).thenReturn("new name");
    when(set.next()).thenReturn(true);

    Car car = entityManager.find(Car.class, 1);

    // Verify object is selected.
    verify(connection, times(1)).prepareStatement("SELECT id,name FROM car WHERE id=?");
    verify(set, times(1)).next();

    // Assert selected values.
    assertEquals("Incorrect selected car id!", 1, car.getId());
    assertEquals("Incorrect selected car name", "new name", car.getName());
  }

  @Test
  public void testNeverFlushChanged() throws Exception {
    entityManager.setFlushMode(FlushModeType.COMMIT);
    entityManager.persist(car);
    entityManager.merge(car);
    entityManager.refresh(car);
    entityManager.remove(car);

    // verify nothing is done until flush is set.
    verify(connection, never()).prepareStatement(anyString());
    verify(connection, never()).prepareStatement(anyString(), anyInt());
  }

  @Test
  public void testTransactionCommit() throws Exception {
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityTransaction.commit();

    verify(connection, times(1)).setAutoCommit(false);
    verify(connection, times(1)).commit();
    verify(connection, times(1)).setAutoCommit(true);
  }

  @Test
  public void testTransactionRollback() throws Exception {
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityTransaction.rollback();

    verify(connection, times(1)).setAutoCommit(false);
    verify(connection, times(1)).rollback();
    verify(connection, times(1)).setAutoCommit(true);
  }

  @Test(expected = IllegalStateException.class)
  public void testTransactionRollbackOnly() throws Exception {
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityTransaction.setRollbackOnly();
    entityTransaction.commit();
  }

  @Test(expected = TransactionRequiredException.class)
  public void testTransactionNotStarted() throws Exception {
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.commit();
  }

  private Car createCar() {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);
    return car;
  }

}