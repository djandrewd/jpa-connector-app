package ua.danit.jpa.configuration;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Configuration for persisted unit entry.
 *
 * @author Andrey Minov
 */
@XmlType(name = "persistence-unit", namespace = "http://java.sun.com/xml/ns/persistence")
@XmlAccessorType(XmlAccessType.FIELD)
public class PersistedUnit {
  @XmlAttribute
  private String name;
  @XmlElement(name = "provider", namespace = "http://java.sun.com/xml/ns/persistence")
  private String provider;

  @XmlElement(name = "properties", namespace = "http://java.sun.com/xml/ns/persistence")
  private PersistenceRoot.Properties properties;

  @XmlElement(name = "class", namespace = "http://java.sun.com/xml/ns/persistence")
  private List<String> classes;

  public String getName() {
    return name;
  }

  public PersistenceRoot.Properties getProperties() {
    return properties;
  }

  public List<String> getClasses() {
    return classes;
  }

  public String getProvider() {
    return provider;
  }

  @Override
  public String toString() {
    return "PersistedUnit{" + "name='" + name + '\'' + ", provider='" + provider + '\''
           + ", properties=" + properties + ", classes=" + classes + '}';
  }
}
