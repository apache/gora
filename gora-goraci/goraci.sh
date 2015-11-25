#!/bin/sh
#
# The Goraci command script
#
# Environment Variables
#
#   GORACI_JAVA_HOME The java implementation to use.  Overrides JAVA_HOME.
#
#   GORACI_HEAPSIZE  The maximum amount of heap to use, in MB. 
#                   Default is 1000.
#
#   GORACI_OPTS      Extra Java runtime options.
#

# resolve links - $0 may be a softlink
THIS="$0"
while [ -h "$THIS" ]; do
  ls=`ls -ld "$THIS"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    THIS="$link"
  else
    THIS=`dirname "$THIS"`/"$link"
  fi
done

# if no args specified, show usage
if [ $# = 0 ]; then
  echo "Usage: run COMMAND [COMMAND options]"
  echo "where COMMAND is one of:"
  echo "  Generator                  A map only job that generates data."
  echo "  Verify                     A map reduce job that looks for holes.  
                             Look at the counts after running.  
                             REFERENCED and UNREFERENCED are ok, 
                             any UNDEFINED counts are bad. Do not 
                             run at the same time as the Generator."
  echo "  Walker                     A standalone program that starts 
                             following a linked list and emits 
                             timing info."
  echo "  Print                      A standalone program that prints nodes 
                             in the linked list."
  echo "  Delete                     A standalone program that deletes a 
                             single node."
  echo "  Loop                       A program to Loop through Generator and
                             Verify steps"
  echo " or"
  echo "  CLASSNAME                  run the class named CLASSNAME"
  echo "Most commands print help when invoked w/o parameters."
  exit 1
fi

# get arguments
COMMAND=$1
shift
  
# some directories
THIS_DIR=`dirname "$THIS"`
GORACI_HOME=`cd "$THIS_DIR" ; pwd`

# cath when JAVA_HOME is not set
if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi
# so that filenames w/ spaces are handled correctly in loops below
IFS=

# restore ordinary behaviour
unset IFS

# figure out which class to run
if [ "$COMMAND" = "Generator" ] ; then
  CLASS=org.apache.gora.goraci.Generator
elif [ "$COMMAND" = "Verify" ] ; then
  CLASS=org.apache.gora.goraci.Verify
elif [ "$COMMAND" = "Walker" ] ; then
  CLASS=org.apache.gora.goraci.Walker
elif [ "$COMMAND" = "Print" ] ; then
  CLASS=org.apache.gora.goraci.Print
elif [ "$COMMAND" = "Delete" ] ; then
  CLASS=org.apache.gora.goraci.Delete
elif [ "$COMMAND" = "Loop" ] ; then
  CLASS=org.apache.gora.goraci.Loop
else
  CLASS=$1
  shift
fi

# initial CLASSPATH 
CLASSPATH=""

# add libs to CLASSPATH
SEP=""
for f in $GORACI_HOME/lib/*.jar; do
  CLASSPATH=${CLASSPATH}$SEP$f;
  SEP=":"
done

#run it
export HADOOP_CLASSPATH="$CLASSPATH"
LIBJARS=`echo $HADOOP_CLASSPATH | tr : ,`
hadoop jar "$GORACI_HOME/lib/gora-goraci-0.7-SNAPSHOT.jar" $CLASS -libjars "$LIBJARS" "$@"


