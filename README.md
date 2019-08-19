# Apache Gora Project

<img src="http://gora.apache.org/resources/img/powered-by-gora.png" align="right" width="300" />

[![license](https://img.shields.io/github/license/apache/gora.svg?maxAge=2592000?style=plastic)](http://www.apache.org/licenses/LICENSE-2.0)
[![Jenkins](https://img.shields.io/jenkins/s/https/builds.apache.org/gora-trunk.svg?maxAge=2592000?style=plastic)](https://builds.apache.org/job/gora-trunk/)
[![Jenkins tests](https://img.shields.io/jenkins/t/https/builds.apache.org/gora-trunk.svg?maxAge=2592000?style=plastic)](https://builds.apache.org/job/gora-trunk)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.gora/gora.svg?maxAge=2592000?style=plastic)](http://search.maven.org/#search|ga|1|g%3A%22org.apache.gora%22)
[![Twitter URL](https://img.shields.io/twitter/url/http/apachegora.svg?style=social&maxAge=2592000?style=plastic)](https://twitter.com/apachegora)
 
The Apache Gora open source framework provides an in-memory data model 
and persistence for big data. Gora supports persisting to column stores, 
key value stores, document stores and RDBMSs, and analyzing the data 
with extensive Apache Hadoop MapReduce, Apache Spark, Apache Flink 
and Apache Pig support.  

## Why Gora?

Although there are various excellent ORM frameworks for relational
databases, data modeling in NoSQL data stores differ profoundly
from their relational cousins. Moreover, data-model agnostic
frameworks such as JDO are not sufficient for use cases, where one
needs to use the full power of the data models in column stores.
Gora fills this gap by giving the user an easy-to-use ORM framework
with data store specific mappings and built in Apache Hadoop support.

The overall goal for Gora is to become the standard data representation
and persistence framework for big data. The roadmap of Gora can be
grouped as follows.

* Data Persistence : Persisting objects to Column stores such as
  HBase, Cassandra, Hypertable; key-value stores such as Voldermort,
  Redis, etc; SQL databases, such as MySQL, HSQLDB, flat files in local
  file system or Hadoop HDFS.

* Data Access : An easy to use Java-friendly common API for accessing
  the data regardless of its location.

* Indexing : Persisting objects to Lucene and Solr indexes,
  accessing/querying the data with Gora API.

* Analysis : Accesing the data and making analysis through adapters for
  Apache Pig, Apache Hive and Cascading

* MapReduce support : Out-of-the-box and extensive MapReduce (Apache
  Hadoop) support for data in the data store.

# Background

ORM stands for Object Relation Mapping. It is a technology which
abstacts the persistency layer (mostly Relational Databases) so
that plain domain level objects can be used, without the cumbersome
effort to save/load the data to and from the database. Gora differs
from current solutions in that:

* Gora is specially focussed at NoSQL data stores, but also has limited
  support for SQL databases.

* The main use case for Gora is to access/analyze big data using Hadoop.

* Gora uses Avro for bean definition, not byte code enhancement or annotations.

* Object-to-data store mappings are backend specific, so that full data
  model can be utilized.

* Gora is simple since it ignores complex SQL mappings.

* Gora will support persistence, indexing and anaysis of data, using Pig,
  Lucene, Hive, etc.


 For the latest information about Gora, please visit our website at:
 
   http://gora.apache.org
 
# License

Gora is provided under Apache License version 2.0. See LICENSE.txt for more details.
