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
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.TransactionRequiredException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for EntitySession implementation.
 *
 * @author Andrey Minov
 */
@Ignore
public class JPASessionTest {
  private EntityManager entityManager;
  private Connection connection;

  @Before
  public void setUp() throws Exception {
    connection = mock(Connection.class);
    entityManager = /**Get entity manager from somewhere*/null;
  }

  @Test
  public void persist() throws Exception {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);

    ResultSet set = mock(ResultSet.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.getGeneratedKeys()).thenReturn(set);
    when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
        .thenReturn(statement);

    // Persist entry
    entityManager.persist(car);

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    verify(set, times(1)).next();
  }

  @Test
  public void mergeNotExisted() throws Exception {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);

    ResultSet set = mock(ResultSet.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.getGeneratedKeys()).thenReturn(set);
    when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
        .thenReturn(statement);

    // Persist entry
    entityManager.merge(car);

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    verify(set, times(1)).next();
  }

  @Test
  public void mergeExisted() throws Exception {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);

    ResultSet set = mock(ResultSet.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.getGeneratedKeys()).thenReturn(set);
    when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
        .thenReturn(statement);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    entityManager.setFlushMode(FlushModeType.COMMIT);
    // Persist entry
    entityManager.persist(car);
    // And then merge changes.
    car.setName("fast new car!");
    entityManager.merge(car);
    entityManager.flush();

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    // And updated.
    verify(connection, times(1)).prepareStatement("UPDATE car SET name=? WHERE id=?");
    verify(set, times(1)).next();
  }


  @Test
  public void remove() throws Exception {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);

    PreparedStatement statement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    // Persist entry
    entityManager.remove(car);
    // Verify object is removed
    verify(connection, times(1)).prepareStatement("DELETE FROM car WHERE id=?");
  }

  @Test(expected = EntityNotFoundException.class)
  public void refreshNotExisted() throws Exception {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);

    PreparedStatement statement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    // Persist entry
    entityManager.refresh(car);
  }

  @Test
  public void refresh() throws Exception {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);

    ResultSet set = mock(ResultSet.class);
    when(set.getObject("id")).thenReturn(1);
    when(set.getObject("name")).thenReturn("new name");
    when(set.next()).thenReturn(true);

    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.getGeneratedKeys()).thenReturn(mock(ResultSet.class));
    when(statement.executeQuery()).thenReturn(set);
    when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
        .thenReturn(statement);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    entityManager.setFlushMode(FlushModeType.COMMIT);
    // Persist entry
    entityManager.persist(car);
    // And then refresh changes.
    entityManager.refresh(car);
    entityManager.flush();

    // Verify object is saved.
    verify(connection, times(1))
        .prepareStatement("INSERT INTO car (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    // And updated.
    verify(connection, times(1)).prepareStatement("SELECT id,name FROM car WHERE id=?");
    verify(set, times(1)).next();

    // Assert selected values.
    assertEquals(1, car.getId());
    assertEquals("new name", car.getName());
  }


  @Test
  public void find() throws Exception {
    ResultSet set = mock(ResultSet.class);
    when(set.getObject("id")).thenReturn(1);
    when(set.getObject("name")).thenReturn("new name");
    when(set.next()).thenReturn(true);

    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.getGeneratedKeys()).thenReturn(mock(ResultSet.class));
    when(statement.executeQuery()).thenReturn(set);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    Car car = entityManager.find(Car.class, 1);

    // Verify object is selected.
    verify(connection, times(1)).prepareStatement("SELECT id,name FROM car WHERE id=?");
    verify(set, times(1)).next();

    // Assert selected values.
    assertEquals(1, car.getId());
    assertEquals("new name", car.getName());
  }

  @Test
  public void flush() throws Exception {
    Car car = new Car();
    car.setName("fast car!");
    car.setId(1);

    when(connection.prepareStatement(anyString(), anyInt()))
        .thenReturn(mock(PreparedStatement.class));
    when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));

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
  public void getTransactionRollback() throws Exception {
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityTransaction.rollback();

    verify(connection, times(1)).setAutoCommit(false);
    verify(connection, times(1)).setAutoCommit(true);
    verify(connection, times(1)).rollback();
  }

  @Test(expected = IllegalStateException.class)
  public void getTransactionRollbackOnly() throws Exception {
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityTransaction.setRollbackOnly();
    entityTransaction.commit();
  }

  @Test(expected = TransactionRequiredException.class)
  public void getTransactionNotStarted() throws Exception {
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.commit();
  }

}