package ua.danit.jpa.sessions;

import static ua.danit.jpa.parsing.executions.SelectExecution.fromMeta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;
import ua.danit.jpa.parsing.JpaPersistenceMetaContext;
import ua.danit.jpa.parsing.executions.DeleteExecution;
import ua.danit.jpa.parsing.executions.InsertExecution;
import ua.danit.jpa.parsing.executions.JpaExecution;
import ua.danit.jpa.parsing.executions.RefreshExecution;
import ua.danit.jpa.parsing.executions.UpdateExecution;
import ua.danit.jpa.query.NativeQuery;

/**
 * Basic implementation for JPA entity manager.
 *
 * @author Andrey Minov
 */
public class JpaSession implements EntityManager {
  private Connection connection;
  private JpaPersistenceMetaContext metaContext;
  private Map<Object, Object> context;
  private Queue<ExecutionEntry> pendingExecution;
  private FlushModeType flushModeType;
  private boolean open;
  private EntityManagerFactory entityManagerFactory;

  /**
   * Instantiates a new Jpa session.
   *
   * @param connection           the JDBC connection
   * @param metaContext          the meta context for entries
   * @param entityManagerFactory the entity manager factory created this session.
   */
  public JpaSession(Connection connection, JpaPersistenceMetaContext metaContext,
                    EntityManagerFactory entityManagerFactory) {
    this.connection = connection;
    this.metaContext = metaContext;
    this.entityManagerFactory = entityManagerFactory;
    this.pendingExecution = new LinkedList<>();
    this.context = new HashMap<>();
    this.flushModeType = FlushModeType.AUTO;
    this.open = true;
  }

  @Override
  public void persist(Object entity) {
    checkOpen();
    EntityMeta meta = metaContext.get(entity.getClass());
    Object key = getPrimaryKey(meta, entity);
    if (context.containsKey(key)) {
      throw new EntityExistsException("Entity already exists in persistence context!");
    }
    context.put(key, entity);
    pendingExecution.offer(ExecutionEntry.create(InsertExecution.fromMeta(meta), entity));
    if (flushModeType == FlushModeType.AUTO) {
      flush();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T merge(T entity) {
    checkOpen();
    EntityMeta meta = metaContext.get(entity.getClass());
    T prev = (T) context.put(getPrimaryKey(meta, entity), entity);
    if (prev == null) {
      pendingExecution.offer(ExecutionEntry.create(InsertExecution.fromMeta(meta), entity));
    } else {
      pendingExecution.offer(ExecutionEntry.create(UpdateExecution.fromMeta(meta), entity));
    }
    if (flushModeType == FlushModeType.AUTO) {
      flush();
    }
    return prev;
  }

  @Override
  public void remove(Object entity) {
    checkOpen();
    EntityMeta meta = metaContext.get(entity.getClass());
    context.remove(getPrimaryKey(meta, entity));
    pendingExecution.offer(ExecutionEntry.create(DeleteExecution.fromMeta(meta), entity));
    if (flushModeType == FlushModeType.AUTO) {
      flush();
    }
  }

  @Override
  public void refresh(Object entity) {
    checkOpen();
    EntityMeta meta = metaContext.get(entity.getClass());
    if (!context.containsKey(getPrimaryKey(meta, entity))) {
      throw new EntityNotFoundException(
          "Entity " + entity + " is not found in persistence context!");
    }
    pendingExecution.offer(ExecutionEntry.create(RefreshExecution.fromMeta(meta), entity));
    if (flushModeType == FlushModeType.AUTO) {
      flush();
    }
  }

  @Override
  public void refresh(Object entity, Map<String, Object> properties) {
    refresh(entity);
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey) {
    checkOpen();
    EntityMeta meta = metaContext.get(entityClass);
    return (T) context.computeIfAbsent(primaryKey, pk -> fromMeta(meta).execute(connection, pk));
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
    return find(entityClass, primaryKey);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode,
                    Map<String, Object> properties) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public <T> T getReference(Class<T> entityClass, Object primaryKey) {
    return null;
  }

  @Override
  public void flush() {
    checkOpen();
    if (connection != null) {
      ExecutionEntry entry;
      while ((entry = pendingExecution.poll()) != null) {
        entry.getExecution().execute(connection, entry.getEntry());
      }
    }
    context.clear();
  }

  @Override
  public FlushModeType getFlushMode() {
    return flushModeType;
  }

  @Override
  public void setFlushMode(FlushModeType flushMode) {
    this.flushModeType = flushMode;
  }

  @Override
  public void lock(Object entity, LockModeType lockMode) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public void clear() {
    context.clear();
    pendingExecution.clear();
  }

  @Override
  public void detach(Object entity) {
    checkOpen();
    EntityMeta meta = metaContext.get(entity.getClass());
    context.remove(getPrimaryKey(meta, entity));
  }

  @Override
  public boolean contains(Object entity) {
    checkOpen();
    EntityMeta meta = metaContext.get(entity.getClass());
    return context.containsKey(getPrimaryKey(meta, entity));
  }

  @Override
  public LockModeType getLockMode(Object entity) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public void setProperty(String propertyName, Object value) {

  }

  @Override
  public Map<String, Object> getProperties() {
    return Collections.emptyMap();
  }

  @Override
  public Query createQuery(String qlString) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public Query createQuery(CriteriaUpdate updateQuery) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public Query createQuery(CriteriaDelete deleteQuery) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public Query createNamedQuery(String name) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public Query createNativeQuery(String sqlString) {
    checkOpen();
    return new NativeQuery(connection, sqlString, null, flushModeType);
  }

  @Override
  public Query createNativeQuery(String sqlString, Class resultClass) {
    checkOpen();
    return new NativeQuery(connection, sqlString, metaContext.get(resultClass), flushModeType);
  }

  @Override
  public Query createNativeQuery(String sqlString, String resultSetMapping) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName,
                                                         Class[] resultClasses) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName,
                                                         String... resultSetMappings) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public void joinTransaction() {
    throw new UnsupportedOperationException("This connector is not available to use with JTA.");
  }

  @Override
  public boolean isJoinedToTransaction() {
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrap(Class<T> cls) {
    if (cls.isAssignableFrom(Connection.class)) {
      return (T) connection;
    }
    return null;
  }

  @Override
  public Object getDelegate() {
    return null;
  }

  @Override
  public void close() {
    flush();

    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        throw new PersistenceException("Unable to close connection:" + e, e);
      }
    }
    this.open = false;
  }

  @Override
  public boolean isOpen() {
    return open;
  }

  @Override
  public EntityTransaction getTransaction() {
    return new ConnectionTransaction();
  }

  @Override
  public EntityManagerFactory getEntityManagerFactory() {
    return entityManagerFactory;
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder() {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public Metamodel getMetamodel() {
    return metaContext;
  }

  @Override
  public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public EntityGraph<?> createEntityGraph(String graphName) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public EntityGraph<?> getEntityGraph(String graphName) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  @Override
  public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
    throw new UnsupportedOperationException("This method is not supported!");
  }

  private Object getPrimaryKey(EntityMeta meta, Object entity) {
    ColumnMeta idColumn = meta.getId().getColumns().get(0);
    try {
      return idColumn.getGetter().invoke(entity);
    } catch (Exception e) {
      throw new PersistenceException("Unable to get entity id!", e);
    }
  }

  private void checkOpen() {
    if (!open) {
      throw new PersistenceException("Entity manager is closed!");
    }
  }

  private static class ExecutionEntry {
    private JpaExecution<?> execution;
    private Object entry;

    /**
     * Instantiates a new Execution entry.
     *
     * @param execution the execution
     * @param entry     the entry
     */
    ExecutionEntry(JpaExecution<?> execution, Object entry) {
      this.execution = execution;
      this.entry = entry;
    }

    /**
     * Create execution entry.
     *
     * @param execution the execution
     * @param entry     the entry
     * @return the execution entry
     */
    static ExecutionEntry create(JpaExecution<?> execution, Object entry) {
      return new ExecutionEntry(execution, entry);
    }

    /**
     * Gets execution.
     *
     * @return the execution
     */
    JpaExecution<?> getExecution() {
      return execution;
    }

    /**
     * Gets entry.
     *
     * @return the entry
     */
    Object getEntry() {
      return entry;
    }
  }

  private class ConnectionTransaction implements EntityTransaction {
    private boolean rollbackOnly;
    private boolean isActive;

    @Override
    public void begin() {
      try {
        connection.setAutoCommit(true);
        isActive = true;
      } catch (SQLException e) {
        throw new PersistenceException("Incorrect transaction action!", e);
      }
    }

    @Override
    public void commit() {
      try {
        if (!isActive) {
          throw new TransactionRequiredException();
        }
        if (rollbackOnly) {
          throw new IllegalStateException("Transaction is mark as rollback only!");
        }
        if (flushModeType == FlushModeType.COMMIT) {
          flush();
        }
        connection.commit();
      } catch (SQLException e) {
        throw new RollbackException("Incorrect transaction action!", e);
      } finally {
        try {
          isActive = false;
          connection.setAutoCommit(false);
        } catch (SQLException e) {
          Logger.getGlobal().log(Level.SEVERE, e, () -> "Incorrect transaction action!");
        }
      }
    }

    @Override
    public void rollback() {
      try {
        if (!isActive) {
          throw new TransactionRequiredException();
        }
        clear();
        connection.rollback();
      } catch (SQLException e) {
        throw new PersistenceException("Incorrect transaction action!", e);
      } finally {
        try {
          isActive = false;
          connection.setAutoCommit(false);
        } catch (SQLException e) {
          Logger.getGlobal().log(Level.SEVERE, e, () -> "Incorrect transaction action!");
        }
      }
    }

    @Override
    public void setRollbackOnly() {
      rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() {
      return rollbackOnly;
    }

    @Override
    public boolean isActive() {
      return isActive;
    }
  }
}
