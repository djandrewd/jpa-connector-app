package ua.danit.jpa.parsing;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import ua.danit.jpa.entity.ColumnMeta;
import ua.danit.jpa.entity.EntityMeta;

/**
 * Context for holding parsing entries.
 *
 * @author Andrey Minov
 */
public class JpaPersistenceMetaContext implements Metamodel {
  private Map<Class<?>, EntityMeta> entityMetas = new ConcurrentHashMap<>();

  /**
   * Get entity metadata for then class.
   *
   * @param clazz the clazz
   * @return the metainformation for the entity
   * @throws IllegalArgumentException when class is not registered in persistence metacontext
   */
  public EntityMeta get(Class<?> clazz) {
    return Optional.ofNullable(entityMetas.get(clazz))
                   .orElseThrow(() -> new IllegalArgumentException(
                       "Metadata for class " + clazz + " is not found!"));
  }

  /**
   * Register class into persistence metacontext.
   *
   * @param clazz the clazz to register
   * @throws IllegalArgumentException when provided class is not marked
   *                                  as @{@link javax.persistence.Entity}
   *                                  or when getter or setter not existed
   *                                  for some of entities field.
   */
  public void register(Class<?> clazz) {
    try {
      EntityMeta meta = EntityMetaParser.parseEntity(clazz);
      if (meta == null) {
        throw new IllegalArgumentException(
            "Provided class " + clazz + " is not marked as JPA entity!");
      }
      entityMetas.put(clazz, meta);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Getter or setter for one of fields incorrect!", e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <X> EntityType<X> entity(Class<X> cls) {
    return new JpaEntityType<>(get(cls), cls);
  }

  @Override
  public <X> ManagedType<X> managedType(Class<X> cls) {
    return null;
  }

  @Override
  public <X> EmbeddableType<X> embeddable(Class<X> cls) {
    return null;
  }

  @Override
  public Set<ManagedType<?>> getManagedTypes() {
    return new HashSet<>(getEntities());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<EntityType<?>> getEntities() {
    Set<EntityType<?>> out = new HashSet<>();
    entityMetas.entrySet().stream().map(e -> new JpaEntityType(e.getValue(), e.getKey()))
               .forEach(out::add);
    return out;
  }

  @Override
  public Set<EmbeddableType<?>> getEmbeddables() {
    return Collections.emptySet();
  }

  private static class JpaEntityType<T> implements EntityType<T> {

    private EntityMeta entityMeta;
    private Class<T> clazz;

    public JpaEntityType(EntityMeta entityMeta, Class<T> clazz) {
      this.entityMeta = entityMeta;
      this.clazz = clazz;
    }

    @Override
    public String getName() {
      return entityMeta.getTableName();
    }

    @Override
    public BindableType getBindableType() {
      return BindableType.ENTITY_TYPE;
    }

    @Override
    public Class<T> getBindableJavaType() {
      return clazz;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Y> SingularAttribute<? super T, Y> getId(Class<Y> type) {
      List<ColumnMeta> ids = entityMeta.getId().getColumns();
      return ids.stream().filter(v -> v.getType().equals(type))
                .map(v -> new JpaAttribute(v, this, true)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Y> SingularAttribute<T, Y> getDeclaredId(Class<Y> type) {
      return (SingularAttribute<T, Y>) getId(type);
    }

    @Override
    public <Y> SingularAttribute<? super T, Y> getVersion(Class<Y> type) {
      throw new UnsupportedOperationException("Versions is not supported!");
    }

    @Override
    public <Y> SingularAttribute<T, Y> getDeclaredVersion(Class<Y> type) {
      throw new UnsupportedOperationException("Versions is not supported!");
    }

    @Override
    public IdentifiableType<? super T> getSupertype() {
      throw new UnsupportedOperationException("Method is not supported!");
    }

    @Override
    public boolean hasSingleIdAttribute() {
      return entityMeta.getId().getColumns().size() == 1;
    }

    @Override
    public boolean hasVersionAttribute() {
      return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<SingularAttribute<? super T, ?>> getIdClassAttributes() {
      List<ColumnMeta> ids = entityMeta.getId().getColumns();
      Set<SingularAttribute<? super T, ?>> out = new HashSet<>();
      ids.stream().map(v -> new JpaAttribute(v, this, true)).forEach(out::add);
      return out;
    }

    @Override
    public Type<?> getIdType() {
      ColumnMeta id = entityMeta.getId().getColumns().get(0);
      return new Type<Object>() {
        @Override
        public PersistenceType getPersistenceType() {
          return PersistenceType.BASIC;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Object> getJavaType() {
          return (Class<Object>) id.getType();
        }
      };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Attribute<? super T, ?>> getAttributes() {
      Set<Attribute<? super T, ?>> out = new HashSet<>();
      entityMeta.getColumns().stream().map(v -> new JpaAttribute(v, this, false)).forEach(out::add);
      return out;
    }

    @Override
    public Set<Attribute<T, ?>> getDeclaredAttributes() {
      return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Y> SingularAttribute<? super T, Y> getSingularAttribute(String name, Class<Y> type) {
      return entityMeta.getColumns().stream().filter(v -> v.getType().equals(type))
                       .map(v -> new JpaAttribute(v, this, false)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SingularAttribute<? super T, ?> getSingularAttribute(String name) {
      return entityMeta.getColumns().stream().filter(v -> v.getName().equals(name))
                       .map(v -> new JpaAttribute(v, this, false)).findFirst().orElse(null);
    }

    @Override
    public <Y> SingularAttribute<T, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
      return null;
    }

    @Override
    public SingularAttribute<T, ?> getDeclaredSingularAttribute(String name) {
      return null;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Set<SingularAttribute<? super T, ?>> getSingularAttributes() {
      Set<SingularAttribute<? super T, ?>> out = new HashSet<>();
      entityMeta.getColumns().stream().map(v -> new JpaAttribute(v, this, false)).forEach(out::add);
      return out;
    }

    @Override
    public Set<SingularAttribute<T, ?>> getDeclaredSingularAttributes() {
      return Collections.emptySet();
    }


    @Override
    public CollectionAttribute<? super T, ?> getCollection(String name) {
      return null;
    }

    @Override
    public <E> CollectionAttribute<? super T, E> getCollection(String name, Class<E> elementType) {
      return null;
    }

    @Override
    public CollectionAttribute<T, ?> getDeclaredCollection(String name) {
      return null;
    }

    @Override
    public <E> CollectionAttribute<T, E> getDeclaredCollection(String name, Class<E> elementType) {
      return null;
    }

    @Override
    public <E> SetAttribute<? super T, E> getSet(String name, Class<E> elementType) {
      return null;
    }


    @Override
    public SetAttribute<? super T, ?> getSet(String name) {
      return null;
    }

    @Override
    public <E> SetAttribute<T, E> getDeclaredSet(String name, Class<E> elementType) {
      return null;
    }

    @Override
    public SetAttribute<T, ?> getDeclaredSet(String name) {
      return null;
    }

    @Override
    public <E> ListAttribute<? super T, E> getList(String name, Class<E> elementType) {
      return null;
    }

    @Override
    public ListAttribute<? super T, ?> getList(String name) {
      return null;
    }

    @Override
    public <E> ListAttribute<T, E> getDeclaredList(String name, Class<E> elementType) {
      return null;
    }

    @Override
    public ListAttribute<T, ?> getDeclaredList(String name) {
      return null;
    }

    @Override
    public MapAttribute<? super T, ?, ?> getMap(String name) {
      return null;
    }

    @Override
    public <K, V> MapAttribute<? super T, K, V> getMap(String name, Class<K> keyType,
                                                       Class<V> valueType) {
      return null;
    }

    @Override
    public MapAttribute<T, ?, ?> getDeclaredMap(String name) {
      return null;
    }


    @Override
    public <K, V> MapAttribute<T, K, V> getDeclaredMap(String name, Class<K> keyType,
                                                       Class<V> valueType) {
      return null;
    }

    @Override
    public Set<PluralAttribute<? super T, ?, ?>> getPluralAttributes() {
      return Collections.emptySet();
    }

    @Override
    public Set<PluralAttribute<T, ?, ?>> getDeclaredPluralAttributes() {
      return Collections.emptySet();
    }

    @Override
    public Attribute<? super T, ?> getAttribute(String name) {
      return null;
    }

    @Override
    public Attribute<T, ?> getDeclaredAttribute(String name) {
      return null;
    }


    @Override
    public PersistenceType getPersistenceType() {
      return PersistenceType.ENTITY;
    }

    @Override
    public Class<T> getJavaType() {
      return clazz;
    }
  }

  private static class JpaAttribute implements SingularAttribute {

    private ColumnMeta columnMeta;
    private ManagedType declaredType;
    private boolean id;

    JpaAttribute(ColumnMeta columnMeta, ManagedType declaredType, boolean id) {
      this.columnMeta = columnMeta;
      this.declaredType = declaredType;
      this.id = id;
    }

    @Override
    public boolean isId() {
      return id;
    }

    @Override
    public boolean isVersion() {
      return false;
    }

    @Override
    public boolean isOptional() {
      return !columnMeta.isNullable();
    }

    @Override
    public Type getType() {
      return new Type() {
        @Override
        public PersistenceType getPersistenceType() {
          return PersistenceType.BASIC;
        }

        @Override
        public Class getJavaType() {
          return columnMeta.getType();
        }
      };
    }

    @Override
    public String getName() {
      return columnMeta.getName();
    }

    @Override
    public PersistentAttributeType getPersistentAttributeType() {
      return PersistentAttributeType.BASIC;
    }

    @Override
    public ManagedType getDeclaringType() {
      return declaredType;
    }

    @Override
    public Class getJavaType() {
      return columnMeta.getType();
    }

    @Override
    public Member getJavaMember() {
      return columnMeta.getField();
    }

    @Override
    public boolean isAssociation() {
      return false;
    }

    @Override
    public boolean isCollection() {
      return false;
    }

    @Override
    public BindableType getBindableType() {
      return BindableType.SINGULAR_ATTRIBUTE;
    }

    @Override
    public Class getBindableJavaType() {
      return columnMeta.getType();
    }
  }
}
