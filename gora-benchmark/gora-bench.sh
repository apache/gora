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
# See the License for the specific language governing permissions and
# limitations under the License.

#Adapted from YCSB's version of ycsb.sh. 
CLASSPATH="."

# Attempt to find the available JAVA, if JAVA_HOME not set
if [ -z "$JAVA_HOME" ]; then
  JAVA_PATH=$(which java 2>/dev/null)
  if [ "x$JAVA_PATH" != "x" ]; then
    JAVA_HOME=$(dirname "$(dirname "$JAVA_PATH" 2>/dev/null)")
  fi
fi

# If JAVA_HOME still not set, error
if [ -z "$JAVA_HOME" ]; then
  echo "[ERROR] Java executable not found. Exiting."
  exit 1;
fi

# Determine YCSB command argument
if [ "load" = "$1" ] ; then
  YCSB_COMMAND=-load
  YCSB_CLASS=com.yahoo.ycsb.Client
  DB_CLASS=org.apache.gora.benchmark.GoraBenchmarkClient
elif [ "run" = "$1" ] ; then
  YCSB_COMMAND=-t
  YCSB_CLASS=com.yahoo.ycsb.Client
  DB_CLASS=org.apache.gora.benchmark.GoraBenchmarkClient
else
  echo "[ERROR] Found unknown command '$1'"
  echo "[ERROR] Expected one of 'load', 'run', or 'shell'. Exiting."
  exit 1;
fi

SAVEIFS=$IFS
IFS=$(echo -en "\n\b")

# add libs to CLASSPATH
for f in target/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

for f in lib/*.jar; do
 CLASSPATH=${CLASSPATH}:$f;
done

IFS=$SAVEIFS

CLASSPATH=${CLASSPATH}:target/classes/
CLASSPATH=${CLASSPATH}:target/test-classes/

#CLASSPATH=${CLASSPATH}:conf
#CLASSPATH=${CLASSPATH}:conf

# Get the rest of the arguments
YCSB_ARGS=$(echo "$@" | cut -d' ' -f2-)

#echo $YCSB_ARGS

# Print details to standard output
echo "$JAVA_HOME/bin/java $JAVA_OPTS -classpath $CLASSPATH $YCSB_CLASS $YCSB_COMMAND -db $DB_CLASS $YCSB_ARGS"

# Run Gora Bench
"$JAVA_HOME/bin/java" $JAVA_OPTS -classpath "$CLASSPATH" $YCSB_CLASS $YCSB_COMMAND -db $DB_CLASS $YCSB_ARGS

