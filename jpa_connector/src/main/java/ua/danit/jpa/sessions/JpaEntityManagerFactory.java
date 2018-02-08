package ua.danit.jpa.sessions;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import ua.danit.jpa.configuration.JpaProviderProperties;
import ua.danit.jpa.parsing.JpaPersistenceMetaContext;

/**
 * Basic entity manager factory with DataSource inside.
 *
 * @author Andrey Minov
 */
public class JpaEntityManagerFactory implements EntityManagerFactory {

  private JpaPersistenceMetaContext metaContext;
  private DataSource dataSource;
  private boolean open;
  private Map<String, String> properties;

  /**
   * Instantiates a new entity manager factory.
   *
   * @param properties the properties for datasource connection.
   * @param classes    the classes that must be added to persistence context.
   */
  public JpaEntityManagerFactory(Map<String, String> properties, List<String> classes) {
    String connectionUrl = properties.get(JpaProviderProperties.CONNECTION_URL);
    String username = properties.get(JpaProviderProperties.USERNAME);
    String password = properties.get(JpaProviderProperties.PASSWORD);
    String driverName = properties.get(JpaProviderProperties.DRIVER);
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl(connectionUrl);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setDriverClassName(driverName);
    this.dataSource = dataSource;
    this.metaContext = new JpaPersistenceMetaContext();
    this.open = true;
    this.properties = properties;
    if (classes != null) {
      for (String clazz : classes) {
        try {
          metaContext.register(Class.forName(clazz));
        } catch (ClassNotFoundException e) {
          throw new PersistenceException(e);
        }
      }
    }
  }

  /**
   * Instantiates a new Jpa entity manager factory.
   *
   * @param dataSource the data source
   */
  public JpaEntityManagerFactory(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public EntityManager createEntityManager() {
    return createEntityManager(Collections.emptyMap());
  }

  @Override
  public EntityManager createEntityManager(Map map) {
    try {
      return new JpaSession(dataSource.getConnection(), metaContext, this);
    } catch (SQLException e) {
      throw new PersistenceException("Unable to get connection from pool!", e);
    }
  }

  @Override
  public EntityManager createEntityManager(SynchronizationType synchronizationType) {
    throw new UnsupportedOperationException("Current provider is not ready to work with JTA!");
  }

  @Override
  public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
    throw new UnsupportedOperationException("Current provider is not ready to work with JTA!");
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder() {
    throw new UnsupportedOperationException("Method is not yet supported");
  }

  @Override
  public Metamodel getMetamodel() {
    return metaContext;
  }

  @Override
  public boolean isOpen() {
    return open;
  }

  @Override
  public void close() {
    try {
      open = false;
      ((BasicDataSource) dataSource).close();
    } catch (SQLException e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public Map<String, Object> getProperties() {
    return new HashMap<>(properties);
  }

  @Override
  public Cache getCache() {
    throw new UnsupportedOperationException("Method is not yet supported");
  }

  @Override
  public PersistenceUnitUtil getPersistenceUnitUtil() {
    throw new UnsupportedOperationException("Method is not yet supported");
  }

  @Override
  public void addNamedQuery(String name, Query query) {
    throw new UnsupportedOperationException("Method is not yet supported");
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    return null;
  }

  @Override
  public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {

  }
}
