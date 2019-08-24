<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Background 
This guide will take you through compiling and running the gora-benchmark module on a target database. 

# Prerequisite
1. Install and setup the required database to test. Currently, the benchmark is tested on 

   1. MongoDB
   2. HBase
   3. CouchDB.

2. Install maven

# Setup the datasource to benchmark

1. Edit the gora.properties file located at src/main/resources/gora.properties and set configurations accordingly. 

     a. To test MongoDB set gora.datastore.default=org.apache.gora.mongodb.store.MongoStore

     b. To test HBase set gora.datastore.default=org.apache.gora.hbase.store.HBaseStore

     c. To test CouchDB set gora.datastore.default=org.apache.gora.couchdb.store.CouchDBStore

2. As of writing this guide, the YCSB project does not publish its jars in maven central. The simplest way to solve this problem is to do a local maven install. First download YCSB source and execute mvn clean install in the root directory. This may take some time to complete. You can add the -DskipTests switch to skip all test.

3. From the module directory i.e. gora-benchmark, run mvn clean install 

4. Now run the benchmark using the following command

5. Load the database

     ./bin/gora-bench.sh load -threads 15 -s -p fieldcount=20 -p recordcount=1000 -p operationcount=1000 -P workloads/workloada

6. Run the workload

     ./bin/gora-bench.sh run -threads 15 -s -p readallfields=true -p measurementtype=timeseries -p timeseries.granularity=2000 -p operationcount=1000 -P workloads/workloadb


More details about the parameters and their usage can be found in the links below.  

https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload

https://github.com/brianfrankcooper/YCSB/wiki/Core-Properties
