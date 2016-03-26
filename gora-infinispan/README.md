# gora-infinispan

### Description 

The Apache Gora open source framework provides an in-memory data model and persistence for big data. Gora supports persisting to column stores, key value stores, document stores and RDBMSs, and analyzing data with extensive [Apache Hadoop](https://hadoop.apache.org/) [MapReduce](https://en.wikipedia.org/wiki/MapReduce) support. 

This project provides a Gora support for the [Infinispan](http://infinispan.org) storage system, allowing it to store data for Hadoop based applications, such as Apache [Nutch](http://nutch.apache.org/) or [Giraph](http://giraph.apache.org/).

### Requirements

[infinispan-avro-8.0.0.CR1](https://github.com/leads-project/infinispan-avro)

### Installation 

This project is based upon Maven. It makes use of Infinispan 7.2.5.Final and the Avro support for Infinispan which is available [here](https://github.com/infinispan/infinispan). Below, we explain how to execute an installation.

```
# Building and installing infinispan-avro
git clone https://github.com/leads-project/infinispan-avro.git
cd infinispan-avro
mvn clean install -DskipTests

# Building and installing gora-infinispan
git clone https://github.com/leads-project/gora-infinispan.git
cd gora-infinispan
mvn clean install -DskipTests
```

### Usage

Gora allows a user application to store, retrieve and query Avro defined types. As of version 0.6, it offers [CRUD operations](http://gora.apache.org/current/api/apidocs-0.6/org/apache/gora/store/DataStore.html) and [query](http://gora.apache.org/current/api/apidocs-0.6/org/apache/gora/query/Query.html) that handle pagination, key range restriction, [filtering](http://gora.apache.org/current/api/apidocs-0.6/org/apache/gora/filter/Filter.html) and projection. 

The key interest of Gora is to offer a direct support for Hadoop to the data stores that implement its API. Under the hood, such a feature comes from a bridge between the [ImputFormat](http://gora.apache.org/current/api/apidocs-0.6/org/apache/gora/mapreduce/GoraInputFormat.html) and [OutputFormat](http://gora.apache.org/current/api/apidocs-0.6/org/apache/gora/mapreduce/GoraOutputFormat.html) classes and the [DataStore](http://gora.apache.org/current/api/apidocs-0.6/org/apache/gora/store/DataStore.html) class.

This Infinispan support for Gora pass all the unit tests of the framework. All the querying operations are handled at the server side, and splitting a query allows to execute it at each of the Infinispan server, close to the data. Thanks to this last feature, map-reduce jobs that run atop of Infinisapn are locality-aware. 

## Code Sample

In the samples below, we first duplicate a query across all the servers, then we execute two filtering operations.

```java
Utils.populateEmployeeStore(employeeStore, NEMPLOYEE);
InfinispanQuery<String,Employee> query;

// Partitioning
int retrieved = 0;
query = new InfinispanQuery<>(employeeDataStore);
query.build();
for (PartitionQuery<String,Employee> q : employeeDataStore.getPartitions(query)) {
retrieved+=((InfinispanQuery<String,Employee>) q).list().size();
}
assert retrieved==NEMPLOYEE;

// Test matching everything
query = new InfinispanQuery<>(employeeDataStore);
SingleFieldValueFilter filter = new SingleFieldValueFilter();
filter.setFieldName("name");
filter.setFilterOp(FilterOp.EQUALS);
List<Object> operaands = new ArrayList<>();
operaands.add("*");
filter.setOperands(operaands);
query.setFilter(filter);
query.build();
List<Employee> result = new ArrayList<>();
for (PartitionQuery<String,Employee> q : employeeDataStore.getPartitions(query)) {
result.addAll(((InfinispanQuery<String,Employee>)q).list());
}
assertEquals(NEMPLOYEE,result.size());
```
