Prerequisite

Install and setup the required database to test. Currently, the benchmark is tested on MongoDB, HBase and CouchDB.

Install maven

Set up

1. Edit the gora.properties file located at src/main/resources/gora.properties and set configurations accordingly. 

   a. To test MongoDB set gora.datastore.default=org.apache.gora.mongodb.store.MongoStore

   b. To test HBase set gora.datastore.default=org.apache.gora.hbase.store.HBaseStore

   c. To test CouchDB set gora.datastore.default=org.apache.gora.couchdb.store.CouchDBStore

2. As of writing this guide, the YCSB project does not publish its jars in maven central. The simplest way to solve this problem is to do a local maven install. First download YCSB source and execute mvn clean install in the root directory. This may take some time to complete. You can add the -DskipTests switch to skip all test.

3. From the module directory i.e. gora-benchmark, run mvn clean install 

5. Now run the benchmark using the following command

6. Load the database

./bin/gora-bench.sh load -threads 15 -s -p fieldcount=20 -p recordcount=1000 -p operationcount=1000 -P workloads/workloada

7. Run the workload

./bin/gora-bench.sh run -threads 15 -s -p readallfields=true -p measurementtype=timeseries -p timeseries.granularity=2000 -p operationcount=1000 -P workloads/workloadb


More details about the parameters and their usage can be found in the links below.  

https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload

https://github.com/brianfrankcooper/YCSB/wiki/Core-Properties
