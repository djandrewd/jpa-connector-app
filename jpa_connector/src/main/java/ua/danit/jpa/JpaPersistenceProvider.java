package ua.danit.jpa;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import ua.danit.jpa.configuration.PersistedUnit;
import ua.danit.jpa.configuration.PersistenceRoot;
import ua.danit.jpa.configuration.Property;
import ua.danit.jpa.sessions.JpaEntityManagerFactory;

/**
 * Basic persistence provider implementation.
 *
 * @author Andrey Minov
 */
public class JpaPersistenceProvider implements PersistenceProvider {
  @Override
  public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
    if (emName == null || emName.isEmpty()) {
      throw new IllegalArgumentException("Persistence unit must not be null!");
    }
    try {
      JAXBContext context = JAXBContext.newInstance(PersistenceRoot.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();

      URL url = getClass().getResource("/META-INF/persistence.xml");
      if (url == null) {
        throw new IllegalArgumentException("Unable to locate persistence.xml!");
      }
      PersistenceRoot providers = (PersistenceRoot) unmarshaller.unmarshal(url);
      PersistedUnit unit = providers.getUnits().stream()
                                    .filter(u -> emName.equals(u.getName())).findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(
                                               "PersistedUnit with name " + emName
                                               + " is not find in persistence.xml!"));

      Map<String, String> properties = Collections.emptyMap();
      if (unit.getProperties() != null && unit.getProperties().getValues() != null) {
        properties = unit.getProperties().getValues().stream().collect(Collectors
            .toMap(Property::getName, Property::getValue));
      }
      return new JpaEntityManagerFactory(properties, unit.getClasses());
    } catch (JAXBException e) {
      throw new RuntimeException(
          "Persistence cannot" + " be created due to persistence.xml parse error!", e);
    }

  }

  @Override
  public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info,
                                                                  Map map) {
    return new JpaEntityManagerFactory(info.getJtaDataSource());
  }

  @Override
  public void generateSchema(PersistenceUnitInfo info, Map map) {
    throw new UnsupportedOperationException("Schema generation is not yet supported!");
  }

  @Override
  public boolean generateSchema(String persistenceUnitName, Map map) {
    throw new UnsupportedOperationException("Schema generation is not yet supported!");
  }

  @Override
  public ProviderUtil getProviderUtil() {
    throw new UnsupportedOperationException("Provider utils implementation is not yet supported!");
  }
}
