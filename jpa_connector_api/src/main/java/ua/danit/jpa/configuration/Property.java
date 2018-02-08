package ua.danit.jpa.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Class for persisted provider proverty.
 *
 * @author Andrey Minov
 */
@XmlType(name = "property", namespace = "http://java.sun.com/xml/ns/persistence")
@XmlAccessorType(XmlAccessType.FIELD)
public class Property {
  @XmlAttribute
  private String name;

  @XmlAttribute
  private String value;

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "Property{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
  }
}
