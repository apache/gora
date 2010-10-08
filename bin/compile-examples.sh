#!/bin/bash

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

# some directories
THIS_DIR=`dirname "$THIS"`
GORA_HOME=`cd "$THIS_DIR/.." ; pwd`

MODULE=gora-core
DIR=$MODULE/src/examples/avro/
OUTDIR=$MODULE/src/examples/java
GORA_BIN=$GORA_HOME/bin/gora

for f in `ls $DIR` ; do
  echo "Compiling $DIR$f"
  $GORA_BIN compile $DIR$f $OUTDIR 
done

