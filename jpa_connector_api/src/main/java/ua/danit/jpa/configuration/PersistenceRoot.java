package ua.danit.jpa.configuration;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Root element for persisted units configuration from persistence.xml.
 *
 * @author Andrey Minov
 */
@XmlRootElement(name = "persistence", namespace = "http://java.sun.com/xml/ns/persistence")
@XmlAccessorType(XmlAccessType.FIELD)
public class PersistenceRoot {
  @XmlElement(name = "persistence-unit", namespace = "http://java.sun.com/xml/ns/persistence")
  private List<PersistedUnit> units;

  public List<PersistedUnit> getUnits() {
    return units;
  }

  @Override
  public String toString() {
    return "PersistenceRoot{" + "units=" + units + '}';
  }

  @XmlType(name = "properties", namespace = "http://java.sun.com/xml/ns/persistence")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Properties {
    @XmlElement(name = "property", namespace = "http://java.sun.com/xml/ns/persistence")
    private List<Property> values;


    public List<Property> getValues() {
      return values;
    }

    @Override
    public String toString() {
      return "Properties{" + "values=" + values + '}';
    }
  }

}
