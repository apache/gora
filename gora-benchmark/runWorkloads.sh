#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#Check if number of commanline arguments is right
threadcount=15
if [ $# -ne 3 ]
then
    echo "Error: Usage: $0 hbase|mongodb gora|ycsb workloadfile"
    exit;
fi
#Log output file
outputfile="$3-$2-$1.log"
insertfactor=5000000
if [ $2 = "ycsb" ]
then
   table="usertable"
elif [ $2 == "gora" ]
then
   table="users"
fi

if [ $1 = "hbase" ] 
then
   #Read operation
   echo -e "Disabling Users Table" | tee -a $outputfile
   echo -e "disable '$table'" | hbase shell -n
   echo -e "Dropping Users Table" | tee -a $outputfile
   echo -e "drop '$table'" | hbase shell -n
   echo -e "Creating Users table with 200 regions: 200 Regions is recommended by YCSB" | tee -a $outputfile
   echo -e "create '$table', 'info', {SPLITS => (1..200).map {|i| \"user#{1000+i*(9999-1000)/200}\"}}" | hbase shell -n
for i in {100000..5000000..100000}
do
   #Insert only workload, uncomment this block of code. I will improve this later.
   #echo -e "Disabling Users Table" | tee -a $outputfile
   #echo -e "disable '$table'" | hbase shell -n
   #echo -e "Dropping Users Table" | tee -a $outputfile
   #echo -e "drop '$table'" | hbase shell -n
   #echo -e "Creating Users table with 200 regions: 200 Regions is recommended by YCSB" | tee -a $outputfile
   #echo -e "create '$table', 'info', {SPLITS => (1..200).map {|i| \"user#{1000+i*(9999-1000)/200}\"}}" | hbase shell -n
   
   echo -e "Table Setup Completed ===================== Now Running Benchmark for $i records and operations" | tee -a $outputfile
   echo -e "Record Count $i" | tee -a $outputfile
   echo -e "Operation Count $i" | tee -a $outputfile
   echo -e "Thread Count $threadcount" | tee -a $outputfile
   echo -e ""
   if [ $2 = "gora" ]
   then
     ./gora-bench.sh load -threads 15 -s -p fieldcount=20 -p recordcount=$insertfactor -p operationcount=$i -P workloads/workloada
     ./gora-bench.sh run -threads 15 -s -p readallfields=true -p measurementtype=timeseries -p timeseries.granularity=2000 -p operationcount=$i -P workloads/$3 | tee -a $outputfile
   elif [ $2 = "ycsb" ]
   then
      ../../ycsb/bin/ycsb.sh load hbase20 -p columnfamily=info -threads 15 -s -p fieldcount=20 -p recordcount=$insertfactor -p operationcount=$i -P workloads/workloada
      ../../ycsb/bin/ycsb.sh run hbase20 -s -p columnfamily=info -p measurementtype=timeseries -p timeseries.granularity=2000 -threads 15 -p readallfields=true -p operationcount=$i -P workloads/$3 | tee -a $outputfile
   fi
   echo "====================================End of Benchmark====================================" | tee -a $outputfile
done
elif [ $1 = "mongodb" ]
then
      echo -e "Dropping $table from MongoDB" | tee -a $outputfile
      if [ $2 = "ycsb" ]
      then
          mongo < setupycsbmongo.js
	  ../../ycsb/bin/ycsb.sh load mongodb -threads 15 -s -p fieldcount=20 -p recordcount=$insertfactor -p operationcount=$insertfactor -P workloads/workloada
      fi

      if [ $2 = "gora" ]
      then
          mongo < setupgoramongo.js
	  ./gora-bench.sh load -threads 15 -s -p fieldcount=20 -p recordcount=$insertfactor -p operationcount=$insertfactor -P workloads/workloada
      fi
      echo -e "Table Setup Completed ===================== Now Running Benchmark for $i records and operations" | tee -a $outputfile
for i in {100000..5000000..100000}
do
   #echo -e "Dropping $table from MongoDB" | tee -a $outputfile
   #mongo < setupmongo.js
   #echo -e "Table Setup Completed ===================== Now Running Benchmark for $i records and operations" | tee -a $outputfile
   echo -e "Record Count $i" | tee -a $outputfile
   echo -e "Operation Count $i" | tee -a $outputfile
   echo -e "Thread Count $threadcount" | tee -a $outputfile
   echo -e ""
   if [ $2 = "gora" ]
   then
     #./gora-bench.sh load -threads 15 -s -p fieldcount=20 -p recordcount=$insertfactor -p operationcount=$i -P workloads/workloada
     ./gora-bench.sh run -threads 15 -s -p readallfields=true -p operationcount=$i -p measurementtype=timeseries -p timeseries.granularity=2000 -P workloads/$3 | tee -a $outputfile
   elif [ $2 = "ycsb" ]
   then
      #../../ycsb/bin/ycsb.sh load mongodb -threads 15 -s -p fieldcount=20 -p recordcount=$insertfactor -p operationcount=$i -P workloads/workloada
      ../../ycsb/bin/ycsb.sh run mongodb-async -threads 15 -s -p readallfields=true -p operationcount=$i -p measurementtype=timeseries -p timeseries.granularity=2000 -P workloads/$3 | tee -a $outputfile
   fi
   echo "====================================End of Benchmark====================================" | tee -a $outputfile
done
fi
#extract operation and runtime from log
echo -e "Extracting number of operation and runtime from log file"
cat $outputfile | grep -E "Operation Count"  > a; cat $outputfile | grep -E "RunTime" > b;
paste a b > $outputfile.csv
sed -i 's/\[OVERALL\], RunTime(ms), //g' $outputfile.csv
sed -i 's/Operation Count //g' $outputfile.csv
rm a; rm b
