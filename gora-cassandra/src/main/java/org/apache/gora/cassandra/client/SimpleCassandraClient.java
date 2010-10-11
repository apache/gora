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
package org.apache.gora.cassandra.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.TokenRange;
import org.apache.commons.lang.NotImplementedException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class SimpleCassandraClient implements CassandraClient {

  public Cassandra.Client client;

  private TTransport transport;

  private String keySpace;

  private ConsistencyLevel consistencyLevel;

  private static ExecutorService SERVICE =
    Executors.newSingleThreadScheduledExecutor();

  private static int NUM_CLIENTS = 0;

  static {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        synchronized (SERVICE) {
          if (!SERVICE.isShutdown()) {
            SERVICE.shutdown();
          }
        }
      }
    });
  }

  public SimpleCassandraClient(String host, int port, String keySpace)
  throws TTransportException {
    this(host, port, keySpace, ConsistencyLevel.ONE);
  }

  public SimpleCassandraClient(String host, int port,
      String keySpace, ConsistencyLevel consistencyLevel)
  throws TTransportException {
    this.transport = new TSocket(host, port);
    this.transport.open();
    this.client = new Cassandra.Client(new TBinaryProtocol(transport));
    setKeySpace(keySpace);
    setConsistencyLevel(consistencyLevel);
    synchronized (SERVICE) {
      NUM_CLIENTS++;
      if (SERVICE.isShutdown()) {
        SERVICE = Executors.newSingleThreadScheduledExecutor();
      }
    }
  }

  public Cassandra.Client getClient() {
    return client;
  }

  @Override
  public Map<String, Map<String, String>> describeKeySpace()
  throws IOException {
    try {
      return client.describe_keyspace(keySpace);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Row get(String key, Select select) throws IOException {
    List<SliceGet> sliceGets =
      new ArrayList<SliceGet>();
    Map<ColumnParent, SlicePredicate> predicateMap = select.getPredicateMap();

    for (Entry<ColumnParent, SlicePredicate> e : predicateMap.entrySet()) {
      sliceGets.add(new SliceGet(client, keySpace, key,
          e.getKey(), e.getValue(), consistencyLevel));
    }
    List<Future<Pair<ColumnParent, List<ColumnOrSuperColumn>>>> results;
    try {
      results = SERVICE.invokeAll(sliceGets);
    } catch (InterruptedException e1) {
      throw new IOException(e1);
    }
    Row row = new Row(key);
    for (Future<Pair<ColumnParent, List<ColumnOrSuperColumn>>> f : results) {
      Pair<ColumnParent, List<ColumnOrSuperColumn>> pair;
      try {
        pair = f.get();
      } catch (Exception e1) {
        throw new IOException(e1);
      }
      for (ColumnOrSuperColumn csc : pair.getSecond()) {
        ColumnParent parent = pair.getFirst();
        row.addColumnOrSuperColumn(parent.column_family, parent.super_column, csc);
      }
    }
    return row;
  }

  public List<Row> getRangeIntl(String startKey, String endKey, int rowCount, Select select)
  throws IOException {
    KeyRange range = new KeyRange(rowCount).setStart_key(startKey).setEnd_key(endKey);
    return getKeyRange(range, select);
  }

  public List<Row> getTokenRangeIntl(String startToken, String endToken, int rowCount,
      Select select) throws IOException {
    KeyRange range =
      new KeyRange(rowCount).setStart_token(startToken).setEnd_token(endToken);
    return getKeyRange(range, select);
  }

  @Override
  public RowIterable getRange(String startKey, String endKey, int rowCount,
      Select select) throws IOException {
    // TODO: Not yet implemented!!!
    throw new NotImplementedException("Not yet implemented!");
  }

  @Override
  public RowIterable getTokenRange(String startToken, String endToken,
      int rowCount, Select select) throws IOException {
    return new TokenRangeRowIterableImpl(this, startToken, endToken, rowCount, select);
  }

  private List<Row> getKeyRange(KeyRange keyRange, Select select)
  throws IOException {
    List<RangeSliceGet> rangeSliceGets =
      new ArrayList<RangeSliceGet>();
    Map<ColumnParent, SlicePredicate> predicateMap = select.getPredicateMap();

    for (Entry<ColumnParent, SlicePredicate> e : predicateMap.entrySet()) {
      rangeSliceGets.add(new RangeSliceGet(client, keySpace, keyRange,
          e.getKey(), e.getValue(), consistencyLevel));
    }
    List<Future<Pair<ColumnParent, List<KeySlice>>>> results;
    try {
      results = SERVICE.invokeAll(rangeSliceGets);
    } catch (InterruptedException e1) {
      throw new IOException(e1);
    }

    Map<String, Row> rowMap = new HashMap<String, Row>();

    for (Future<Pair<ColumnParent, List<KeySlice>>> keySlicesTask : results) {
      Pair<ColumnParent, List<KeySlice>> keySlices;
      try {
        keySlices = keySlicesTask.get();
      } catch (Exception e) {
        throw new IOException(e);
      }
      for (KeySlice keySlice : keySlices.getSecond()) {
        addKeySliceToRowMap(rowMap, keySlices.getFirst(), keySlice);
      }
    }

    return new ArrayList<Row>(rowMap.values());
  }

  private void addKeySliceToRowMap(Map<String, Row> rowMap, ColumnParent parent,
      KeySlice keySlice) {
    Row row = rowMap.get(keySlice.key);
    if (row == null) {
      row = new Row(keySlice.key);
      rowMap.put(row.getKey(), row);
    }
    for (ColumnOrSuperColumn csc : keySlice.columns) {
      row.addColumnOrSuperColumn(parent.column_family, parent.super_column, csc);
    }
  }

  @Override
  public void mutate(String key, Mutate mutation) throws IOException {
    Map<String, Map<String, List<Mutation>>> rowMutations =
      new HashMap<String, Map<String,List<Mutation>>>();
    rowMutations.put(key, mutation.getMutationMap());
    try {
      client.batch_mutate(keySpace, rowMutations, consistencyLevel);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void setKeySpace(String keySpace) {
    this.keySpace = keySpace;
  }

  @Override
  public void setConsistencyLevel(ConsistencyLevel level) {
    this.consistencyLevel = level;
  }

  @Override
  public void close() {
    transport.close();
    synchronized (SERVICE) {
      NUM_CLIENTS--;
      if (NUM_CLIENTS == 0 && !SERVICE.isShutdown()) {
        SERVICE.shutdown();
      }
    }
  }

  @Override
  public List<TokenRange> describeRing() throws IOException {
    try {
      return client.describe_ring(keySpace);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<String> describeSplits(String startToken, String endToken,
      int size) throws IOException {
    try {
      return client.describe_splits(startToken, endToken, size);
    } catch (TException e) {
      throw new IOException(e);
    }
  }
}
