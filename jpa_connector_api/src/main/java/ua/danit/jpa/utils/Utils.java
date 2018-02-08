package ua.danit.jpa.utils;

import java.util.Collection;

/**
 * Object related utilities.
 *
 * @author Andrey Minov
 */
public class Utils {

  /**
   * Check value to be non null.
   *
   * @param name  name of the object to check
   * @param value the value to be checked
   * @throws IllegalArgumentException when object is null
   */
  public static void checkNonNull(String name, Object value) {
    if (value == null) {
      throw new IllegalArgumentException(String.format("Value '%s' must not be null!", name));
    }
  }

  /**
   * Check value to be non null and non empty.
   *
   * @param name  name of the object to check
   * @param value the string to be checked
   * @throws IllegalArgumentException when object is null or empty
   */
  public static void checkNonEmpty(String name, String value) {
    checkNonNull(name, value);
    if (value.isEmpty()) {
      throw new IllegalArgumentException(String.format("Value '%s' must not be empty!", name));
    }
  }

  /**
   * Check value to be non null and non empty.
   *
   * @param name       name of the object to check
   * @param collection the collection to be checked
   * @throws IllegalArgumentException when object is null or empty
   */
  public static void checkNonEmpty(String name, Collection<?> collection) {
    checkNonNull(name, collection);
    if (collection.isEmpty()) {
      throw new IllegalArgumentException(String.format("Value '%s' must not be empty!", name));
    }
  }
}
