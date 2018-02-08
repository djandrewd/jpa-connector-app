package ua.danit.jpa.parsing.executions;

import java.sql.Connection;
import javax.persistence.PersistenceException;

/**
 * General execution for SQL JPA operation.
 *
 * @param <R> the result execution parameter
 * @author Andrey Minov
 */
public interface JpaExecution<R> {
  /**
   * Execute JPA action with connection and provided entity.
   *
   * @param connection the JDBC connection used to execute operation
   * @param entity     the entity used on JPA action
   * @return the entity execution result.
   * @throws PersistenceException in case error occured during execution.
   */
  R execute(Connection connection, Object entity);
}
