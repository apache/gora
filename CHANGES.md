 CHANGES.txt
 ===========
 
 # Apache Gora 0.9 Release - 12/08/19 (dd/mm/yyyy)
 Release report - https://s.apache.org/0.9GoraReleaseNotes
  
 Bug
  
     [GORA-208] - Implement consistent use of DataStoreFactory across Gora modules
     [GORA-225] - Various Issues with MemStore
     [GORA-373] - Failing TestQueryBase with JDK8
     [GORA-421] - PersistentBase#setDirty() does not set dirty
     [GORA-476] - Nutch 2.X GeneratorJob creates NullPointerException when using DataFileAvroStore
     [GORA-500] - Bug in org.apache.gora.solr.store.SolrStore#getDatumWriter & #getDatumReader
     [GORA-503] - Field index 0 is always considered as clean even if it is dirty
     [GORA-508] - Website title cut off issue
     [GORA-526] - Potential null dereference in AvroSerializer#analyzePersistent
     [GORA-545] - Boxing/unboxing to parse a primitive is suboptimal
     [GORA-560] - Fix build failure due java docs
     [GORA-611] - Fix intermittent test failures with HBase module
     [GORA-612] - Fix Flink word count tests after merging PR for GORA-565
     [GORA-617] - Fix Inception Year at Gora Pig Module
     [GORA-619] - Remove duplicated properties in gora-tutorial gora.properties
     [GORA-620] - Migrate HBase auto flush parameter name in Pig Module
     [GORA-621] - Remove log4j 2 transitive dependencies inheriting from solr-core dependency
     [GORA-622] - maven-assemly-plugin complains that group id is too big and fails build on macOS Mojave 10.14.6 (18G87)
   
 New Feature
 
     [GORA-109] - Pig Adapter for Gora
     [GORA-266] - Lucene datastore for Gora
     [GORA-411] - Add exists(key) to DataStore interface
     [GORA-444] - Add #size() to Result API
     [GORA-481] - Using Docker For Unit Testing
     [GORA-535] - Add a data store for Apache Ignite
     [GORA-540] - Update Gora documentation for the Ignite backend
     [GORA-548] - Introduce Apache Flink Execution Engine for Gora
  
 Improvement
 
     [GORA-393] - bin/compile-examples.sh should do a touch on source files before compiling
     [GORA-456] - No help when GoraCompilerCli is invoked without parameters
     [GORA-528] - Add Support for Spark 2.2.1
     [GORA-529] - Remove org/apache/gora/avro/mapreduce/FsInput.java
     [GORA-530] - Reinstate exception throwing at Query#execute()
     [GORA-531] - Upgrade HBase to 1.2.6
     [GORA-534] - Prepare gora-hbase for HBase 2.0 release
     [GORA-547] - Upgrade Aerospike datastore to client 4.2.2
     [GORA-554] - Upgrade Solr dependency to latest
     [GORA-555] - Improve Lucene query implementation with NumericRangeQuery
     [GORA-564] - Remove deprecated method usages of HBase module after upgrading to 2
     [GORA-565] - Enable Spark in Unit Tests
     [GORA-613] - Remove deprecated method usages Flink Module
     [GORA-614] - JCache datastore should be able to work with any JCache provider available in classpath
     [GORA-615] - Update gora-tutorial pom to include mongodb
     [GORA-616] - Multiple slf4j conflict issue
     [GORA-618] - Fix gpg-maven-plugin and checksum-maven-plugin issues for the release
  
 Task
 
     [GORA-536] - Avoid calling Class#newInstance
     [GORA-543] - Upgrade Apache parent POM to version 21
     [GORA-549] - Remove PersistentBase extending java.io.Externalizable
     [GORA-550] - Update POM for the move to gitbox
     [GORA-610] - Upgrade Apache Avro from 1.8.1-->1.8.2

# Apache Gora 0.8 Release - 15/09/17 (dd/mm/yyyy)
Release report - https://s.apache.org/3YdY

Bug

    [GORA-209] - Specify query timeout for Hector usage in gora-cassandra
    [GORA-269] - Add parameter for setting port used in the Cassandra Module
    [GORA-310] - testDeleteByQueryFields is not sutiable for Solr DataStore
    [GORA-311] - Gora Solr deleteByQuery method is not suitable for testDeleteByQueryFields
    [GORA-327] - Upgrade to Cassandra 2.0.7
    [GORA-394] - Erroneous 'Error: KeyClass in gora-hbase-mapping is not the same as the one in the databean.' log when reading HBase mapping
    [GORA-395] - NPE occurs in o.a.g.cassandra.query.CassandraResult when accessing cc (=null) and setting the unionField (title)
    [GORA-399] - Gora-Cassandra should handle Strings inside Union types as CharSequences
    [GORA-416] - Error when populating data into Cassandra super column - InvalidRequestException(why:supercolumn parameter is not optional for super CF sc
    [GORA-510] - Conform Query.setEndKey to inclusive query for HBaseStore
    [GORA-511] - Eclipse shows the error 'The type package-info is already defined'
    [GORA-522] - Fix the issue in PUT method of Aerospike
    [GORA-525] - Fix java-doc issues in Cassandra Datastore

Improvement

    [GORA-107] - Fully annotate Javadoc's for methods in org.apache.gora.cassandra.store.*
    [GORA-205] - Dedup CassandraMapping and CassandraMappingManager
    [GORA-214] - use batched Mutations available in Hector Mutator
    [GORA-215] - expose Hector's CassandraHostConfigurator possibilities
    [GORA-255] - Remove deprecated methods from DataStoreTestBase
    [GORA-267] - Cassandra composite primary key support
    [GORA-299] - o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));
    [GORA-324] - Port work done on old GoraCompiler to new GoraCompiler
    [GORA-497] - Migrate CassandraThrift to CQL
    [GORA-509] - Upgrade solr module into solr 6
    [GORA-512] - Fail at the time of error logging if KeyClass in gora-hbase-mapping is not the same as the one in the data bean
    [GORA-515] - Update the Aerospike client
    [GORA-518] - Upgrade the supported policies to the new client in Aerospike Store
    [GORA-520] - Add support to get partitions in Aerospike module
    [GORA-523] - Add map reduce based test cases to aerospike module

New Feature

    [GORA-502] - Implement Aerospike Datastore
    [GORA-513] - Implement OrientDB Datastore.
    [GORA-519] - Add support to access authenticated servers in Aerospike

Task

    [GORA-308] - deleteByQuery() method is not implemented in CassandraStore
    [GORA-516] - Document Aerospike Datastore at Apache Gora site.
    [GORA-517] - Upgrade Mongodb Java Driver to version 3.5.0
    [GORA-521] - Document Cassandra Datastore at Apache Gora site.

# Apache Gora 0.7 Release - 16/03/17 (dd/mm/yyyy)
Release report - https://s.apache.org/YrmC

Sub-task

    [GORA-55] - Fix TestHBaseStore
    [GORA-98] - Support CQL through Gora Hector API usage in Gora
    [GORA-99] - Implement Speed4j within Hector API
    [GORA-100] - Implement Kerberos security within gora-cassandra
    [GORA-227] - Failing assertions when putting and getting Values using MemStore#execute
    [GORA-316] - Upgrade to Avro 1.8.x in gora-dynamodb
    [GORA-409] - JCache Datastore
    [GORA-446] - java.util.NoSuchElementException thrown by accessing java.util.concurrent.ConcurrentSkipListMap.firstKey in MemStore
    [GORA-447] - Fix NPE within MemStoreTest.testMemStoreDeleteByQueryFields and failed assertion within MemStoreTest.testGetWithFields
    [GORA-489] - Adding CouchDB Datastore Documentation

Bug

    [GORA-291] - Gora-core, unable to wire up packages not known in advance.
    [GORA-293] - getDatastore use Class.forName() when use String parameters for dataStoreClass
    [GORA-367] - testCountQuery(org.apache.gora.avro.mapreduce.TestDataFileAvroStoreMapReduce) failing in new environments
    [GORA-398] - Create and initialize Gora master Jenkins job which uses Hadoop 2.X shims
    [GORA-401] - Serialization and deserialization of Persistent does not hold the entity dirty state from Map to Reduce
    [GORA-438] - Erroneous Exception Message at IOUtils.java
    [GORA-440] - Fix several instances of 'Method may fail to close stream' within gora-accumulo BinaryEncoder
    [GORA-441] - Update build.gradle within gora-gradle-plugin to reference canonical Apache links
    [GORA-453] - Implement bootstraping of GoraCI nodes with Chef and the JClouds ComputeService
    [GORA-458] - DataOutputStream is not closed in BinaryEncoder#encodeXXX() methods
    [GORA-459] - Resource leak due to unclosed Table
    [GORA-460] - InputStream is not closed in DataStoreFactory#getMappingFile()
    [GORA-462] - Potential null dereference of serializer in CassandraSubColumn#getFieldValue()
    [GORA-463] - Missing assignment in CassandraSubColumn#getUnionSchema()
    [GORA-467] - Flushing DataOutputStream before calling toByteArray on the underlying ByteArrayOutputStream
    [GORA-474] - bug in org.apache.gora.solr.store.SolrStore#getDatumWriter & #getDatumReader
    [GORA-478] - Address Sonar issues within gora-accumulo Encoder classes
    [GORA-479] - Various compilation errors in gora-infinispan module
    [GORA-480] - Compilation issues with gora-dynamodb
    [GORA-491] - JCache test fail under JDK 1.8
    [GORA-492] - javadoc:jar build error
    [GORA-493] - Unclosed DataInputBuffer in IOUtils#deserialize()
    [GORA-494] - Lack of synchronization accessing cacheManager in InfinispanClient#createSchema()
    [GORA-495] - Gora compiler generates uncompilable databeans
    [GORA-496] - gora commands doesn't output results to console
    [GORA-499] - Potential NPE issue
    [GORA-503] - Field index 0 is always considered as clean even if it is dirty
    [GORA-505] - TestContainers should be moved to test scope

Improvement

    [GORA-49] - Fix failing JUint tests in Gora modules
    [GORA-78] - Umbrella parent issue for Hector usage
    [GORA-125] - Clean up logging libraries
    [GORA-135] - Add gora-env.sh and other Hadoop related configuration to /conf
    [GORA-240] - Tests for MemStore
    [GORA-274] - Extend the LogManager to support -get <lineNum> <field list>
    [GORA-277] - Create gora-maven-plugin
    [GORA-315] - Define <scope> for * dependencies in pom.xml
    [GORA-359] - Add integration to other distributed system frameworks to allow Gora as data ingest
    [GORA-362] - Refactor gora-dynamodb to support avro serialization
    [GORA-368] - Address flagged Sonar error in Gora package design
    [GORA-412] - Consider location of @SuppressWarnings("all") in compiled classes
    [GORA-414] - Upgrade to Accumulo 1.6.4
    [GORA-439] - Remove Unused Method Parameters
    [GORA-443] - Upgrade HBase to 1.2.3
    [GORA-448] - Upgrade MongoDB Java Driver Version
    [GORA-452] - Upgrade goraci prior to Chef and the ComputeService implementations
    [GORA-466] - Upgrade to Avro 1.8.X
    [GORA-469] - Gora doesn't build under JDK 1.8
    [GORA-477] - Add support for Solr 5.x
    [GORA-482] - Move into supporting only Hadoop2
    [GORA-483] - CI build for Gora
    [GORA-487] - Using IOUtils methods instead of duplicate code lines
    [GORA-488] - setProperties method contains no code
    [GORA-490] - Adding package-info.java for gora datastore packages
    [GORA-498] - Adding MongoDB Authentications
    [GORA-504] - Switched Accumulo Dependency to 1.7.1 and ported AccumuloStore Class to work with accumulo 1.7.1

New Feature

    [GORA-102] - Implement a datastore for Hypertable
    [GORA-224] - Pluggable client archiecture for gora-cassandra
    [GORA-295] - Dynamic support for Cache Providers using JCache
    [GORA-343] - Update Gora to support HBase version >= 0.96
    [GORA-437] - Implement CouchDB Datastore
    [GORA-445] - Review extent of transative dependencies post GORA-386
    [GORA-471] - Datastore for Infinispan

Task

    [GORA-294] - Setup Pre-Commit build for gora-trunk Jenkins job
    [GORA-442] - Create documentation for GoraSparkEngine
    [GORA-484] - Add documentation to JCache Datastore
    [GORA-501] - Fix Javadoc for JDK1.8 compliance

Test

    [GORA-370] - Implement MapReduce 2.X tests
    [GORA-454] - Implement FilterList tests
    [GORA-465] - Remove @Deprecated logic for hbase-mapping.xml
    [GORA-506] - Investigate timing of HBase tests

# Apache Gora 0.6.1 Release - 05/09/2015 (dd/mm/yyyy)
Release Report - http://s.apache.org/l69

* GORA-436 Improve Source Code as Java 7 Compatible (Furkan KAMACI via lewismc)

* GORA-435 Clean Up Code and Fix Potential Bugs (Furkan KAMACI via lewismc)

* GORA-433 Non-Final Public Static Fields Should Be Final (Furkan KAMACI via lewismc)

* GORA-314 Implement TestDriver for AccumuloStoreTest e.g. TODO in class (lewismc)

* GORA-417 Deploy Hadoop-1 compatible binaries for Apache Gora (lewismc)

* GORA-432 Simplify if Statements (Furkan KAMACI via lewismc)

* GORA-386 Gora Spark Backend Support (Furkan KAMACI, talat, lewismc)

* GORA-429 Implement Maven forbidden-apis plugin in Gora (lewismc, rmarroquin)

* GORA-228 java.util.ConcurrentModificationException when using MemStore for concurrent tests (Yasin Kılınç, cihad güzel, lewismc)

* GORA-419: AccumuloStore.put deletes entire row when updating map/array field (gerhardgossen via lewismc)

* GORA-420: AccumuloStore.createSchema fails when table already exists (gerhardgossen via lewismc)

* GORA-427 Configure MongoDB ReadPreference and WriteConcern (drazzib)

* GORA-426 MongoDB cursor timeout on long running parse job (Alexander Yastrebov via drazzib)

* GORA-424 Cache cursor size to improve performance (Alexander Yastrebov via drazzib)

* GORA-423 BSONDecorator returns empty string for null field value (Alexander Yastrebov via drazzib)

* GORA-262 Add support for HTTPClient authentication in gora-solr (Furkan KAMACI via lewismc)

* GORA-384 Provide documentation on Gora Shims layer (lewismc)

* GORA-415 hadoop-client dependency should be optional in gora-core (hsaputra via lewismc)

* GORA-410 Change logging behavior to pass exception object to LOG methods (Gerhard Gossen via lewismc)

* GORA-330 Import Gora Gradle plugin (drazzib)


# Apache Gora 0.6 Release - 12/02/2015 (dd/mm/yyyy)
Release Report - http://s.apache.org/gora-0.6

* GORA-406 Upgrade Solr dependencies to 4.10.3 (lewismc)

* GORA-407 Upgrade restlet dependencies to 2.3.1 for gora-solr (lewismc)

* GORA-388 MongoStore: Fix handling of Utf8 in filters (drazzib)

* GORA-375 Upgrade HBase to 0.98 (Talat UYARER via lewismc)

* GORA-389 MongoStore: Document or List mapping change cause NPE in clearDirty() (drazzib)

* GORA-390 Rework generated isXXXDirty methods. (drazzib)
 
* GORA-381 Fix Guava dependency mismatch post GoraCI (lewismc)

* GORA-374 Implement Rackspace Cloud Orchestration in GoraCI (lewismc)

* GORA-371 Exception setXIncludeAware UnsupportedOperationException (Viacheslav Dobromyslov (dobromyslov) via lewismc)

* GORA-378 Log error trace as well as error message in GoraRecordWriter (lewismc)

* GORA-376 Gora Cassandra doesn't accept user credentials for connection (Viju Kothuvatiparambil via lewismc) 

* GORA-372: Fixed runtime error with the slf4j-api version conflict. (Viacheslav Dobromyslov (dobromyslov) via hsaputra)

# Apache Gora 0.5 Release - 15/09/14
Release Report - http://s.apache.org/0.5report

* GORA-369 Obtain consistent formatting of all pom.xml's (lewismc)

* GORA-346 Create shim layer to support multiple hadoop versions (Moritz Hoffmann, rmarroquin, hsaputra, Mikhail Bernadsky via lewismc)

* GORA-73 Merge goraci testing suite with master branch (kturner, enis via lewismc)

* GORA-353 Accumulo authentication token serialized incorrectly (Chin Huang via lewismc)

* GORA-167 forward port of Make Cassandra keyspace consistency configurable within gora.properties (rmarroquin via lewismc)

* GORA-364 MemStore.get fails with NPE when key is not set (Gerhard Gossen via lewismc)

* GORA-361 AvroUtils.deepClonePersistent needs to flush BinaryEncoder (Gerhard Gossen via hsaputra)

* GORA-351 Multiple Slf4j logging implementations in parent pom.xml (lewismc)

* GORA-234 Javadoc and java annotations in gora-core base (abstract) classes needs to be corrected (tpalsulich via lewismc)

* GORA-349 Update Datastore specific mapping documentation with new default root element (MJJoyce) via lewismc

* GORA-241 Properly document WebServiceBackedDataStore Interface (Tyler Palsulich)

* GORA-347 Column attributes should be obtained using the column name and not the field name. (kaveh minooie, rmarroquin)

* GORA-348 - Using query for Avro Unions in Cassandra doesn't fully work (rmarroquin)

* GORA-XX Attemtpt to resolve javax.servlet classloading issue within gora-solr (lewismc)

* GORA-339 Upgrade solr dependencies to 4.8.1 (lewismc)

* GORA-341 - Switch gora-orm references to gora-otd in tutorial (MJJoyce via lewismc)

* GORA-332 fix record.vm template to generate deepCopyToReadOnlyBuffer method (drazzib aka Damien Raude-Morvan via hsaputra)

* GORA-331 Gora 0.4 compiler crash with "enum" type (drazzib aka Damien Raude-Morvan via hsaputra)

* GORA-336 MongoFilterUtil: missing ref link when creating new instance of factory (drazzib via lewismc)

* GORA-260 Make Solrj solr server impl configurable from within gora.properties (lewismc)

* GORA-199 MongoDB support for Gora ( Damien Raude-Morvan)

* GORA-333 Move README to markdown for better rendering on Github (lewismc)

* GORA-329 Update all SCM links in * pom.xml's (lewismc)

* GORA-325 Add Gora 0.4 Javadoc to site (lewismc)

# Gora 0.4 release: (14/04/2014)
Release Report: http://s.apache.org/4lx

* GORA-201 Upgrade HBase to 0.94.13 (various)

* GORA-292 Upgrade dependency to Accumulo 1.5 (Akber Choudhry via lewismc)

* GORA-245 Upgrade to Avro 1.7.X in gora-cassandra (lewismc, Talat, Uyarer, rmarroquin)

* GORA-94 Upgrade to Apache Avro 1.7.x (Ed Kohlwey, lewismc, ferdy, yasin tamer, alparslanavci, Talat Uyarer, rmarroquin)

* GORA-106 Migrate Gora website documentation to Apache CMS (lewismc)

* GORA-296 Improve 'Keyclass and nameclass match' logging in HBaseStore (rmarroquin via lewismc)

* GORA-246 Upgrade to Avro 1.7.X in gora-hbase (Alparslan Avcı, rmarroquin, lewismc via lewismc)

* GORA-154 delete() method is not implemented at CassandraStore, and always returns false or 0 (rmarroquin via Kazuomi Kashii)

* GORA-204 Don't store empty arrays in CassandraClient#addGenericArray(), addStatefulHashMap() and CassandraStore#addOrUpdateField(rmarroquin via lewismc)

* GORA-303 Upgrade to Avro 1.7.X in gora-solr (Talat UYARER)

* GORA-253 Add Facebook, Linkedin, Google+, Twitter, etc plugins to website (lewismc)

* GORA-244 Upgrade to Avro 1.7.X in gora-accumulo (Akber Choudhry via lewismc)

* GORA-306 Ssn field is not nullable in Employee's Avro Schema (Talat UYARER via lewismc)

* GORA-171 Implement Daily Rolling File Appender for localised Gora logging (lewismc)

* GORA-119 implement a filter enabled scan in gora (ferdy, kturner, enis, Tien Nguyen Manh via lewismc)

* GORA-231 Provide better error handling in AccumuloStore.readMapping when file does not exist. (apgiannakidis via lewismc)

* GORA-283 Specify field name for types not being considered in gora-cassandra (lewismc)

* GORA-285 Change logging at o.a.g.mapreduce.GoraRecordWriter from INFO to WARN (lewismc)

* GORA-117 gora hbase does not have a mechanism to set the caching on a scanner, which makes for poor performance on map/reduce jobs (alfonsonishikawa)

* GORA-281 More flexible file locations for cassandra's config and log4j (Nate McCall via hsaputra)

* GORA-275 Update Gora stores to pass conf when creating instance of PartitionQueryImpl (Damien Raude-Morvan via hsaputra)

* GORA-270 IOUtils static SerializationFactory field (Damien Raude-Morvan via hsaputra)

* GORA-268 Make GoraCompiler the main manifest attribute in gora-core (Apostolos Giannakidis via lewismc)

* GORA-265 Support for dynamic file extensions when traversing a directory (Apostolos Giannakidis via lewismc)

* GORA-264 Make generated data beans more java doc friendly (Apostolos Giannakidis via lewismc)

* GORA-222 upgrade jackson version to 1.6.9 (rherget via lewismc)

* GORA-232 DataStoreTestBase should delegate all functionality to DataStoreTestUtil (Apostolos Giannakidis via lewismc)

* GORA-259 Removal of the main methods from the test case classes (Apostolos via hsaputra)

* GORA-229 Use @Ignore for unimplemented functionality to identify absent tests (Apostolos Giannakidis via lewismc)

* GORA-258 writeCapacUnits gets value from wrong attribute (Apostolos Giannakidis via lewismc)

* GORA-256 Add Solr store to gora-tutorial (Scott Stults via lewismc)

* GORA-9 Implement a Solr-based store (ab, lewismc, Scott Stults)

* GORA-237 Gora Compiler usage message for LPGL v3 mentions v2.1 (David Medinets via lewismc)

* GORA-230 Change logging behavior in AccumuloStore to pass exception object to LOG.error method. (David Medinets via lewismc)

* GORA-185 Remove ANT scripts and IVY confs (lewismc)

* GORA-243 Properly escaping spaces of GORA_HOME in bin/gora (Apostolos Giannakidis via lewismc)

* GORA-174 GORA compiler does not handle ["string", "null"] unions in the AVRO schema (alfonsonishikawa, rmarroquin, kturner via lewismc)
  incl. GORA-206, 207 and 216.

* GORA-239 Add null checks and better message in AccumuloStore (David Medinets via hsaputra)

# 0.3 release: 05/03/2013 (mm/dd/yyyy)
Release Report: https://issues.apache.org/jira/secure/ReleaseNote.jspa?projectId=12311172&version=12317954Gora Change Log

* GORA-191 Support multiple Avro Schemas within GoraCompiler (Udesh Liyanaarachchi, rmarroquin, lewismc) 

* GORA-159 gora-hbase MR tests should use HBaseTestingUtility instead of deprecated HBaseClusterTestCase via GORA-89 

* GORA-89 Avoid HBase MiniCluster restarts to shorten gora-hbase tests (hsaputra, alfonso, Ioan eugen Stan)

* GORA-203 Bug in setting column field attribute "qualifier" in CassandraMapping (rmarroquin, lewismc, kazk)

* GORA-221 fix Cassandra configuration in gora-tutorial (lewismc)

* GORA-211 thread safety: fix java.lang.NullPointerException - synchronize on mutator (rherget)

* GORA-210 thread safety: fix java.util.ConcurrentModificationException (rherget)

* GORA-190 Add "version" switch to bin/gora script (lewismc)

* GORA-169 Implement correct logging for KeySpaces and attributes in CassandraMappingManager (lewismc)

* GORA-27 Optionally add license headers to generated files (lewismc + rmarroquin)

* GORA-181 Replace tab characters with correct Gora coding style (rmarroquin)

* GORA-197 gora-cassandra requires BytesType for Cassandra column family validator (kazk)

* GORA-196 OSX JDK7 failed to load snappy native library from snappy-java-1.0.4.1.jar (kazk)

* GORA-182 Nutch 2.1 does not work with gora-cassandra 0.2.1 (kazk)

* GORA-193 Make sure gora-core test dependency is always generated when packaging (lewismc)

* GORA-186 Show better errors when a field is missing in HBase mapping (Alfonso Nishikawa via hsaputra)

* GORA-179 Modify the Query interface to be Query<K, T extends Persistent> to be more precise (hsaputra)

* GORA-178 HBase fix ivy.xml to use the correct antconfig mapping (ferdy) 

* GORA-172 java.lang.ClassNotFoundException: org.apache.gora.memory.store.MemStore.MemQuery (yumeng via lewismc)

* GORA-103 Datastore for gora-dynamodb (rmarroquin via lewismc)

* GORA-160 Gora Fails to Import Into Recent Versions of Eclipse (Ed Kohlwey via lewismc)

* GORA-85 Implement "Usage" messages for SpecificCompiler and LogAnalytics (lewismc)

* GORA-164 Use <type> instead of <classifier> configuration for test dependencies (lewismc)

* GORA-XX ensure directory cleanup succeeds in gora-* (Simone Tripodi via lewismc)

# 0.2.1 release: 26/07/2012
Release Report: https://issues.apache.org/jira/secure/ReleaseNote.jspa?projectId=12311172&version=12322496

* GORA-157 gora-cassandra test failure - proposal to skip 10 test cases for a while (kazk)

* GORA-156 Properly implement getSchemaName in CassandraStore (lewismc)

* GORA-153 gora-cassandra does not correctly handle DELETED State for MAP (kazk)

* GORA-152 gora-core test incorrectly uses ByteBuffer's array() method to get its byte array (kazk)

* GORA-151 CassandraStore's schemaExists() method always returns false (kazk)

* GORA-150 Introduce Configuration property preferred.schema.name (ferdy)

* GORA-142 Creates org.apache.gora.cassandra.serializers package in order to clean the code of store and query packages and to support additional types in future. (kazk)

* GORA-148 CassandraMapping supports only (first) keyspace and class in gora-cassandra-mapping.xml (kazk)

* GORA-143 GoraCompiler needs to add "import FixedSize" statement for FIXED type (kazk)

* GORA-147 fix threading issue caused by multiple threads trying to flush (ferdy)

* GORA-146 HBaseStore does not properly set endkey (ferdy)

* GORA-140 Requires some adjustments on dependency at gora-cassandra (kazk, lewismc)

* GORA-138 gora-cassandra array type support: Double fix for GORA-81 Replace CassandraStore#addOrUpdateField with TypeInferringSerializer to take advantage of when the value is already of type ByteBuffer. (Kazuomi Kashii via lewismc)

* GORA-139  Creates Cassandra column family with BytesType for column value validator (and comparators), instead of UTF8Type (Kazuomi Kashii via lewismc)

* GORA-131 gora-cassandra should support other key types than String (Kazuomi Kashii via lewismc)

* GORA-132 Uses ByteBufferSerializer for column value to support various data types rather than StringSerializer (Kazuomi Kashii via lewismc)

* GORA-77 Replace commons logging with Slf4j (Renato Javier Marroquín Mogrovejo via lewismc)

* GORA-134 ListGenericArray's hashCode causes StackOverflowError (Kazuomi Kashii via lewismc)

* GORA-95 Catch incorrect mapping configurations and implement sufficient logging in CassandraMapping. (lewismc)

* GORA-** Commit to fix classloading for CLI execution (lewismc)

* GORA-122 gora-accumulo/lib is not cleaned after mvn clean (lewismc)

* GORA-133 & 63 GoraCompiler cannot compile array type & bin/compile-examples.sh does not work respectively (enis, Kazuomi Kashii via lewismc)

* GORA-129 redundant conf field in HBaseStore (ferdy)

* GORA-123 Append correct submodule directories to SCM paths in submodule pom's (lewismc)

* GORA-127 Result objects are not closed properly from GoraRecordReader. (enis)

0.2 Release: 20/04/2012
Jira Release Report: https://issues.apache.org/jira/secure/ReleaseNote.jspa?projectId=12311172&version=12315541
 
* GORA-120 Dirty fields are not correctly applied after serialization and map clearance (ferdy)
 
* GORA-115 Flushing HBaseStore should flush all HTable instances. (ferdy)

* Make hbase autoflush default to false and make autoflush configurable rather than hardcoded (stack via lewismc)

* GORA-76 Upgrade to Hadoop 1.0.1 (ferdy & lewismc)

* GORA-65 Initial checkin of gora accumulo datastore (kturner)

* GORA-108 Change CassandraClient#init() to CassandraClient#initialize() for consistency with other Datastores. (lewismc)

* GORA-105 DataStoreFactory does not properly support multiple stores (ferdy)

* GORA-** Update Gora parent pom to include maven release plugin targets, and update developer credentials. (lewismc)

* GORA-74 Remove sqlbuilder library (lewismc)

* GORA-101 HBaseStore should properly support multiple tables in the mapping file. (ferdy)

* GORA-82 Add missing license headers & RAT target to pom.xml (lewismc)

* GORA-88 HBaseByteInterface not thread safe (ferdy)

* GORA-93 [gora-cassandra] Add implementation of CassandraStore.get(key) (Sujit Pal via lewismc)

* GORA-58 Upgrade Gora-Cassandra to use Cassandra 1.0.2 (lewismc)

* GORA-80 Implement functionality to define consistency used for Cassandra read and 
write operations. (lewismc)

* GORA-91 Ensure that Gora adheres to ASF branding requirements (lewismc)

* GORA-90 Create DOAP for Gora (lewismc)

* GORA-79 Block keyspace creation until the whole cassandra cluster converges to the new keyspace. (Patricio Echagüe via lewismc)

* GORA-83 add 'target' dirs to svn ignore (ferdy)

* GORA-66 testDeleteByQueryFields seems incorrect (Keith Turner via lewismc)

* GORA-72 Made all artifacts proper OSGi bundles. Added ClassLoadingUtils which will fallback to thread context classloader if Class.forName fails. (iocanel)

* GORA-55 Removed second assertion for schema existence, since schema is already deleted, Excluded ranged queries from the tests, since the tests assume end key to be inclussive while Hbase considers it exclusive. (iocanel)

* GORA-** Updated the maven surefire plugin version to properly set the system properties (iocanel)

* GORA-22 Upgrade cassandra backend to 0.7 (jnioche & Alexis Detreglode)

* GORA-69 Added xerces to dependencyManagement to avoid version conflicts (iocanel)

* GORA-54 Split TestHBaseStoreMapReduce to two individual tests, so that they can run properly, Added jvm args to maven surefire plugin to increase the max heap size that the tests will use to 512M. (iocanel)

* GORA-70 Upgrade deprecated expressions in pom.xml for use in building effective models, Replace reference to artifactId/version with project.artifactId/project.version (iocanel)

* GORA-68 Added test.build.data System property so that test data can be stored under target. Added port reservation for gora-sql tests. Added mapping xml to the gora-sql test classpath (iocanel)

* GORA-51 Added surefire plugin configuration to run the tests isolated (iocanel)

* GORA-64 Query should document inclusiveness (Keith Turner via lewismc) 

* GORA-52 Run JUnit tests from the command line (lewismc)

* GORA-62 Run bin/gora commands after maven build (lewismc)

* GORA-43 Forward port 0.1.1 incubating Maven poms to trunk (lewismc)

* GORA-57 HBaseStore does not correctly read family definitions (Ferdy via lewismc)

* GORA-56 HBaseStore is not thread safe. (Ferdy via lewismc)

* GORA-48. HBaseStore initialization of table without configuration in constructor will throw Exception (Ferdy via lewismc)

* GORA-47&46. fix tar ant target & Add nightly target to build.xml respectively (lewismc)

* GORA-45. Add dependency to 'clean-cache' ant target (Lewis John McGibbney via mattmann)

* GORA-44. Ant build fails (Lakshmi Narasimhan via mattmann)

* GORA-28. Merge back recent changes in 0.1-incubating to trunk (mattmann, ab)

* GORA-12. Semantics of DataStore.delete* (ab via mattmann)

* GORA-18. Configuration not found error breaks the build on trunk (ioannis via mattmann)

* GORA-26. Configurable classes should pass around Configuration consistently (ab via mattmann)

* GORA-32. Map type with long values generates non-compilable Java class (Yves Langisch)

* GORA-29. Gora maven support (Ioannis Canellos via mattmann)

* GORA-31. jersey-json dependency not in repositories currently in ivysettings.xml (Marsall Pierce via hsaputra)

# 0.1-incubating release:

*  INFRA-3038. Initial import of code.

*  GORA-1. Organize source code for Apache. (enis)

*  GORA-6. Add methods that take dataStoreClass instead of data store instance 
   to Gora{Input|Output}Format and Gora{Mapper|Reducer}. (enis)

*  GORA-7. DataStoreFactory.createDataStore() should throw exceptions on 
   failure. (enis)

*  GORA-15. Primitive types cannot be used as keys in DataStore. (enis)

*  GORA-16. Create a tutorial for Gora. (enis)

*  GORA-21. Commons-lang needs a configuration in gora-core/ivy/ivy.xml (jnioche)

*  GORA-20. Flush datastore regularly (Alexis Detreglode via jnioche)

*  GORA-23. Limit result set in store reads (Alexis Detreglode via hsaputra)

*  GORA-25. Upgrade Gora-hbase to HBase 0.90.0 (ab)

*  GORA-2. Create Gora web site (enis)

*  GORA-5. Add champions and mentors to Gora website (hsaputra)
