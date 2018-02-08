**ORM. JPA 2.1**

You task is to create your own ORM framework by implementing JSR 338: JavaTM Persistence 2.1.
You should also write test application that uses your JPA provider. 

You will be provided with next resources
<ol>
   <li>JpaProviderProperties - class containing properties needed for JPA provider.</li>
   <li>PersistenceRoot, PersistedUnit, Property - JAXB entries for persistence.xml 
   configuration.</li>
   <li>EntityMeta - parsed information about JPA entity.</li>
   <li>ColumnMeta - parsed information about entity column.</li>
   <li>IdMeta - parsed information about id entity column.</li>
   <li>tables_creation.sql - SQL create script for users and user groups.</li>
</ol>

You can use HSQLDB as in-memory SQL storage. 

Implementation should be supplied with:
<ol>
  <li>Parsing entity class files and storage for cached EntityMeta, ColumnMeta, IdMeta.</li>
  <li>Implementation for javax.persistence.spi.PersistenceProvider</li>
  <li>Implementation for javax.persistence.EntityManagerFactory/li>
  <li>Implementation for javax.persistence.EntityManager</li>
  <li>SPI registration for javax.persistence.spi.PersistenceProvider</li>
  <li>Implementation for javax.persistence.Query</li>
</ol>

EntityManager implementation should contain persistence context and should support 
operations:

<ol>
  <li>persist - insert into context or flush in SQL</li>
  <li>merge - merge into context or flush in SQL</li>
  <li>remove - remove from context or flush in SQL</li>
  <li>find - find entity in database and load it into context</li>
  <li>flush - flush modes : AUTO and COMMIT. Write possibility to flush changes immediately 
  and delay until commit operation.</li>
  <li>refresh - refresh entity from persistence context</li>
  <li>createNativeQuery - create and implement  possibility for execute native queries and return
   object results</li>
   <li>getTransaction - create new transaction entity.</li>
</ol>

Impementation should satisfy tests: EntityMetaParserTest, JPASessionTest.

Application which uses JPA provider must be console application which:
<ol>
  <li>Create some number of new users</li>
  <li>Retrieve information about user by login</li>
  <li>Remove user by login</li>
  <li>Retrieve N users from database</li>
  <li>Retrieve N users from database with name = XXX</li>          
</ol>

Example:<br> 
  entityManager<br>.createNativeQuery("SELECT LOGIN, PASSWORD, USERNAME FROM USERS WHERE 
USERNAME = :username", User.class)<br>.setParameter("username", "XXX")<br>.getResultList()

Code must follow OOP and SOLID principles and Google code checkstyle conventions.

Good luck!
