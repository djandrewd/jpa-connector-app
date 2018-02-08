package ua.danit.jpa.users;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.hsqldb.cmdline.SqlFile;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ua.danit.users.entity.User;

/**
 * Test for JPA connector using HSQL database.
 * <p>
 * @author Andrey Minov
 */
public class QueryTest {
  private static final String DB_INIT_SCRIPT_LOCATION = "/tables_creation.sql";
  private static final int USERS_SIZE = 10;

  private static final String SELECT_ALL_USERS_SQL = "SELECT LOGIN, PASSWORD, USERNAME FROM USERS";
  private static final String SELECT_SINGLE_USER =
      "SELECT LOGIN, PASSWORD, USERNAME " + "FROM USERS WHERE LOGIN = :login";
  private static final String SELECT_WITH_USERNAME_PASS =
      "SELECT LOGIN, PASSWORD, " + "USERNAME FROM USERS WHERE USERNAME ="
      + " :username AND PASSWORD = :password";

  private static EntityManagerFactory entityManagerFactory;
  private static List<User> users;

  private EntityManager entityManager;

  @BeforeClass
  public static void initDB() throws Exception {
    entityManagerFactory = Persistence.createEntityManagerFactory("inMemory");
    users = new ArrayList<>();

    createDatabase();
    createAndPersistUsers();
  }

  @AfterClass
  public static void closeDB() throws Exception {
    if (entityManagerFactory != null) {
      entityManagerFactory.close();
    }
  }

  @Before
  public void setUp() throws Exception {
    entityManager = entityManagerFactory.createEntityManager();
  }

  @Test
  public void testSelectAllSavedUsers() {
    assertEquals("Incorrect users result!", users, entityManager
        .createNativeQuery(SELECT_ALL_USERS_SQL, User.class).getResultList());
  }

  @Test
  public void testSelectSingleUser() {
    assertEquals("Incorrect user result!", createUser(0), entityManager
        .createNativeQuery(SELECT_SINGLE_USER, User.class).setParameter("login", "test-u-0")
        .getSingleResult());
  }

  @Test
  public void testSelectTwoUsers() {
    Collection<User> subList = asList(createUser(1), createUser(2));
    assertEquals("Incorrect partial users result!", subList, entityManager
        .createNativeQuery(SELECT_ALL_USERS_SQL, User.class).setFirstResult(1).setMaxResults(2)
        .getResultList());
  }

  @Test
  public void testUserByPassAndUsername() {
    assertEquals("Incorrect selection result!", createUser(7), entityManager
        .createNativeQuery(SELECT_WITH_USERNAME_PASS, User.class)
        .setParameter("username", "name-u-7").setParameter("password", "pass-u-7")
        .getSingleResult());
  }

  @After
  public void tearDown() throws Exception {
    if (entityManager != null) {
      entityManager.close();
    }
  }


  private static User createUser(long index) {
    User user = new User();
    user.setLogin("test-u-" + index);
    user.setPassword("pass-u-" + index);
    user.setUsername("name-u-" + index);
    return user;
  }

  private static void createAndPersistUsers() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.setFlushMode(FlushModeType.COMMIT);
      for (int i = 0; i < USERS_SIZE; i++) {
        User user = createUser(i);
        users.add(user);
        entityManager.persist(user);
      }
      entityManager.flush();
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  private static void createDatabase() throws Exception {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      Path path = Paths.get(QueryTest.class.getResource(DB_INIT_SCRIPT_LOCATION).toURI());
      SqlFile file = new SqlFile(path.toFile());
      file.setConnection(entityManager.unwrap(Connection.class));
      file.execute();
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

}
