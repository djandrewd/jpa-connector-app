package ua.danit.jpa.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;
import ua.danit.jpa.parsing.EntityMetaParser;

/**
 * Native JDBC query with limited functionalities.
 *
 * @author Andrey Minov
 */
public class NativeQuery<T> implements TypedQuery<T> {

  private static final Pattern PARAM_REGEXP = Pattern.compile(":\\w+");

  private final PreparedStatement statement;
  private final Map<String, Integer> parameters;
  private final EntityMeta entityMeta;
  private final Map<Integer, Parameter<?>> boundedParameters;
  private final Map<Integer, Object> parameterValues;
  private final FlushModeType flushMode;

  private int maxResult;
  private int startPosition;

  /**
   * Instantiates a new Native query.
   *
   * @param connection the JDBC connection
   * @param query      the query for selected
   * @param entityMeta the entity meta used in execution result.
   * @param flushMode  the entity manager flush model.
   */
  public NativeQuery(Connection connection, String query, EntityMeta entityMeta,
                     FlushModeType flushMode) {
    this.flushMode = flushMode;
    this.maxResult = Integer.MAX_VALUE;
    this.entityMeta = entityMeta;
    this.startPosition = 0;
    this.boundedParameters = new HashMap<>();
    this.parameterValues = new HashMap<>();

    if (query == null || query.isEmpty()) {
      throw new IllegalArgumentException("Query SQL cannot be empty!");
    }

    parameters = new HashMap<>();
    Matcher matcher = PARAM_REGEXP.matcher(query);
    boolean found = matcher.find();
    if (!found) {
      try {
        this.statement = connection.prepareStatement(query);
      } catch (SQLException e) {
        throw new IllegalArgumentException("Unable to create statement query!", e);
      }
      return;
    }

    StringBuilder stringBuilder = new StringBuilder();
    int start;
    int end = 0;
    int counter = 1;
    do {
      start = matcher.start();
      stringBuilder.append(query, end, start);
      end = matcher.end();

      parameters.put(query.substring(start + 1, end), counter++);
      stringBuilder.append("?");

      found = matcher.find(end);
    } while (found);
    if (end > 0) {
      stringBuilder.append(query, end, query.length());
    }

    try {
      this.statement = connection.prepareStatement(stringBuilder.toString());
    } catch (SQLException e) {
      throw new IllegalArgumentException("Unable to create statement query!", e);
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public List<T> getResultList() {
    if (entityMeta == null) {
      throw new IllegalStateException("Query is not supported select operations!");
    }
    try {
      int idx = 0;
      int count = 0;
      List<T> result = new ArrayList<>();
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          if (idx++ < startPosition) {
            continue;
          }
          if (++count > maxResult) {
            break;
          }
          result.add(createEntity(resultSet, entityMeta));
        }
      }
      return result;
    } catch (Exception e) {
      throw new PersistenceException("Unable to execute query!", e);
    } finally {
      try {
        statement.close();
      } catch (SQLException e) {
        Logger.getGlobal().log(Level.SEVERE, e, () -> "Unable to execute query!");
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public T getSingleResult() {
    if (entityMeta == null) {
      throw new IllegalStateException("Query is not supported select operations!");
    }
    try {
      T entity = null;
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          if (entity != null) {
            throw new NonUniqueResultException();
          }
          entity = createEntity(resultSet, entityMeta);
        }
      }
      return entity;
    } catch (Exception e) {
      throw new PersistenceException("Unable to execute query!", e);
    } finally {
      try {
        statement.close();
      } catch (SQLException e) {
        Logger.getGlobal().log(Level.SEVERE, e, () -> "Unable to execute query!");
      }
    }
  }

  @Override
  public int executeUpdate() {
    try {
      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new PersistenceException("Unable to execute query!", e);
    } finally {
      try {
        statement.close();
      } catch (SQLException e) {
        Logger.getGlobal().log(Level.SEVERE, e, () -> "Unable to execute query!");
      }
    }
  }

  @Override
  public TypedQuery<T> setMaxResults(int maxResult) {
    this.maxResult = maxResult;
    return this;
  }

  @Override
  public int getMaxResults() {
    return maxResult;
  }

  @Override
  public TypedQuery<T> setFirstResult(int startPosition) {
    this.startPosition = startPosition;
    return this;
  }

  @Override
  public int getFirstResult() {
    return startPosition;
  }

  @Override
  public TypedQuery<T> setHint(String hintName, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getHints() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <X> TypedQuery<T> setParameter(Parameter<X> param, X value) {
    try {
      statement.setObject(param.getPosition(), value, EntityMetaParser
          .getSqlType(param.getParameterType()));
      boundedParameters.put(param.getPosition(), param);
      parameterValues.put(param.getPosition(), value);
    } catch (Exception e) {
      throw new PersistenceException("Unable to set parameter!", e);
    }
    return this;
  }


  @SuppressWarnings("unchecked")
  @Override
  public TypedQuery<T> setParameter(String name, Object value) {
    if (!parameters.containsKey(name)) {
      throw new IllegalArgumentException("Parameter " + name + " is not existed in mapping!");
    }
    int position = parameters.get(name);
    return setParameter(new QueryParameter<>(name, position, (Class<Object>) value
        .getClass()), value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public TypedQuery<T> setParameter(int position, Object value) {
    return setParameter(new QueryParameter<>(null, position, (Class<Object>) value
        .getClass()), value);
  }

  @Override
  public TypedQuery<T> setParameter(Parameter<Calendar> param, Calendar value,
                                    TemporalType temporalType) {

    return setParameter(new QueryParameter<>(param.getName(), param
        .getPosition(), Date.class), value.getTime(), temporalType);
  }

  @Override
  public TypedQuery<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
    try {
      switch (temporalType) {
        case DATE:
          statement.setDate(param.getPosition(), new java.sql.Date(value.getTime()));
          parameterValues.put(param.getPosition(), new java.sql.Date(value.getTime()));
          break;
        case TIME:
          statement.setTime(param.getPosition(), new java.sql.Time(value.getTime()));
          parameterValues.put(param.getPosition(), new java.sql.Time(value.getTime()));
          break;
        case TIMESTAMP:
          statement.setTimestamp(param.getPosition(), new java.sql.Timestamp(value.getTime()));
          parameterValues.put(param.getPosition(), new java.sql.Timestamp(value.getTime()));
          break;
        default:
          break;
      }
      boundedParameters.put(param.getPosition(), param);
      return this;
    } catch (Exception e) {
      throw new PersistenceException("Unable to set parameter!", e);
    }
  }

  @Override
  public TypedQuery<T> setParameter(String name, Calendar value, TemporalType temporalType) {
    return setParameter(name, value.getTime(), temporalType);
  }

  @Override
  public TypedQuery<T> setParameter(String name, Date value, TemporalType temporalType) {
    if (!parameters.containsKey(name)) {
      throw new IllegalArgumentException("Parameter " + name + " is not existed in mapping!");
    }
    int position = parameters.get(name);
    return setParameter(position, value, temporalType);
  }


  @Override
  public TypedQuery<T> setParameter(int position, Calendar value, TemporalType temporalType) {
    return setParameter(new QueryParameter<>(null, position, Calendar.class), value, temporalType);
  }

  @Override
  public TypedQuery<T> setParameter(int position, Date value, TemporalType temporalType) {
    return setParameter(new QueryParameter<>(null, position, Date.class), value, temporalType);
  }

  @Override
  public Set<Parameter<?>> getParameters() {
    return new HashSet<>(boundedParameters.values());
  }

  @Override
  public Parameter<?> getParameter(String name) {
    if (!parameters.containsKey(name)) {
      throw new IllegalArgumentException("Parameter " + name + " is not existed in mapping!");
    }
    int position = parameters.get(name);
    return boundedParameters.get(position);
  }

  @Override
  public <X> Parameter<X> getParameter(String name, Class<X> type) {
    throw new UnsupportedOperationException("Only supported in criteria queries!");
  }

  @Override
  public Parameter<?> getParameter(int position) {
    return boundedParameters.get(position);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Parameter<T> getParameter(int position, Class<T> type) {
    Parameter<?> parameter = boundedParameters.get(position);
    return parameter.getParameterType().equals(type) ? (Parameter<T>) parameter : null;
  }

  @Override
  public boolean isBound(Parameter<?> param) {
    return boundedParameters.containsKey(param.getPosition());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <X> X getParameterValue(Parameter<X> param) {
    return (X) parameterValues.get(param.getPosition());
  }

  @Override
  public Object getParameterValue(String name) {
    if (!parameters.containsKey(name)) {
      throw new IllegalArgumentException("Parameter " + name + " is not existed in mapping!");
    }
    int position = parameters.get(name);
    return parameterValues.get(position);
  }

  @Override
  public Object getParameterValue(int position) {
    return parameterValues.get(position);
  }

  @Override
  public TypedQuery<T> setFlushMode(FlushModeType flushMode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlushModeType getFlushMode() {
    return flushMode;
  }

  @Override
  public TypedQuery<T> setLockMode(LockModeType lockMode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public LockModeType getLockMode() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <X> X unwrap(Class<X> cls) {
    if (cls.isAssignableFrom(PreparedStatement.class)) {
      return (X) statement;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private T createEntity(ResultSet resultSet, EntityMeta entityMeta) throws Exception {
    T entity = (T) entityMeta.getConstructor().newInstance();
    List<ColumnMeta> columns = new ArrayList<>();
    columns.addAll(entityMeta.getColumns());
    columns.addAll(entityMeta.getId().getColumns());
    for (ColumnMeta columnMeta : columns) {
      try {
        if (resultSet.findColumn(columnMeta.getName()) > 0) {
          Object value = resultSet.getObject(columnMeta.getName());
          columnMeta.getSetter().invoke(entity, value);
        }
      } catch (SQLException e) {
        // unable to locate column. Assign to null.
        Logger.getGlobal()
              .log(Level.FINEST, e, () -> "Column " + columnMeta.getName() + " not existed!");
      }
    }
    return entity;
  }

  private static class QueryParameter<T> implements Parameter<T> {
    private String name;
    private Integer position;
    private Class<T> clazz;

    QueryParameter(String name, Integer position, Class<T> clazz) {
      this.name = name;
      this.position = position;
      this.clazz = clazz;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Integer getPosition() {
      return position;
    }

    @Override
    public Class<T> getParameterType() {
      return clazz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      QueryParameter<?> that = (QueryParameter<?>) o;

      if (name != null ? !name.equals(that.name) : that.name != null) {
        return false;
      }
      if (position != null ? !position.equals(that.position) : that.position != null) {
        return false;
      }
      return clazz != null ? clazz.equals(that.clazz) : that.clazz == null;
    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (position != null ? position.hashCode() : 0);
      result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
      return result;
    }
  }
}
