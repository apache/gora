/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
This guidline outlines the process of compiling and running Gora Benchmark Module

Prerequisite
Install and setup the required database to test. Currently, the benchmark is tested on MongoDB, HBase and CouchDB.
Install maven

Set up

1. Edit the gora.properties file located at src/main/resources/gora.properties and set configurations accordingly. 

 a. To test MongoDB set gora.datastore.default=org.apache.gora.mongodb.store.MongoStore
 b. To test HBase set gora.datastore.default=org.apache.gora.hbase.store.HBaseStore
 c. To test CouchDB set gora.datastore.default=org.apache.gora.couchdb.store.CouchDBStore


2. From the module directory i.e. gora-benchmark, run mvn clean install
3. This should run some test and successfully build the module. 
	If the build fails and complains about maven not able to ycsb-core-verion.jar then download the jar manually and add it to ~/.m2/repository/com/yahoo/ycsb/core/0.1.4/

5. Now run the benchmark using the following command

6. Load the database
./gora-bench.sh load -threads 15 -s -p fieldcount=20 -p recordcount=1000 -p operationcount=1000 -P workloads/workloada

7. Run the workload
./gora-bench.sh run -threads 15 -s -p readallfields=true -p measurementtype=timeseries -p timeseries.granularity=2000 -p operationcount=1000 -P workloads/workloadb


More details about the parameters and their usage can be found in the links below.  
https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload
https://github.com/brianfrankcooper/YCSB/wiki/Core-Properties
