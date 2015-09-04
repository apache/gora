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
package org.apache.gora.hbase.store;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.RowMutations;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.coprocessor.Batch.Call;
import org.apache.hadoop.hbase.client.coprocessor.Batch.Callback;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcChannel;
import org.apache.hadoop.hbase.util.Pair;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;

/**
 * Thread safe implementation to connect to a HBase table.
 *
 */
public class HBaseTableConnection implements HTableInterface {
  /*
   * The current implementation uses ThreadLocal HTable instances. It keeps
   * track of the floating instances in order to correctly flush and close
   * the connection when it is closed. HBase itself provides a utility called
   * HTablePool for maintaining a pool of tables, but there are still some
   * drawbacks that are only solved in later releases.
   * 
   */
  
  private final Configuration conf;
  private final ThreadLocal<HTable> tables;
  private final BlockingQueue<HTable> pool = new LinkedBlockingQueue<HTable>();
  private final boolean autoFlush;
  private final TableName tableName;
  
  /**
   * Instantiate new connection.
   * 
   * @param conf
   * @param tableName
   * @param autoflush
   * @throws IOException
   */
  public HBaseTableConnection(Configuration conf, String tableName, boolean autoflush)
      throws IOException {
    this.conf = conf;
    this.tables = new ThreadLocal<HTable>();
    this.tableName = TableName.valueOf(tableName);
    this.autoFlush = autoflush;
  }
  
  private HTable getTable() throws IOException {
    HTable table = tables.get();
    if (table == null) {
      table = new HTable(conf, tableName) {
        @Override
        public synchronized void flushCommits() throws RetriesExhaustedWithDetailsException, InterruptedIOException {
          super.flushCommits();
        }
      };
      table.setAutoFlushTo(autoFlush);
      pool.add(table); //keep track
      tables.set(table);
    }
    return table;
  }
  
  @Override
  public void close() throws IOException {
    // Flush and close all instances.
    // (As an extra safeguard one might employ a shared variable i.e. 'closed'
    //  in order to prevent further table creation but for now we assume that
    //  once close() is called, clients are no longer using it).
    for (HTable table : pool) {
      table.flushCommits();
      table.close();
    }
  }

  @Override
  public byte[] getTableName() {
    return tableName.getName();
  }

  @Override
  public Configuration getConfiguration() {
    return conf;
  }

  @Override
  public boolean isAutoFlush() {
    return autoFlush;
  }

  /**
   * getStartEndKeys provided by {@link HTable} but not {@link HTableInterface}.
   * @see HTable#getStartEndKeys()
   */
  public Pair<byte[][], byte[][]> getStartEndKeys() throws IOException {
    return getTable().getStartEndKeys();
  }
  /**
   * getRegionLocation provided by {@link HTable} but not 
   * {@link HTableInterface}.
   * @see HTable#getRegionLocation(byte[])
   */
  public HRegionLocation getRegionLocation(final byte[] bs) throws IOException {
    return getTable().getRegionLocation(bs);
  }

  @Override
  public HTableDescriptor getTableDescriptor() throws IOException {
    return getTable().getTableDescriptor();
  }

  @Override
  public boolean exists(Get get) throws IOException {
    return getTable().exists(get);
  }

  @Override
  public Result get(Get get) throws IOException {
    return getTable().get(get);
  }

  @Override
  public Result[] get(List<Get> gets) throws IOException {
    return getTable().get(gets);
  }

  @Override
  public Result getRowOrBefore(byte[] row, byte[] family) throws IOException {
    return getTable().getRowOrBefore(row, family);
  }

  @Override
  public ResultScanner getScanner(Scan scan) throws IOException {
    return getTable().getScanner(scan);
  }

  @Override
  public ResultScanner getScanner(byte[] family) throws IOException {
    return getTable().getScanner(family);
  }

  @Override
  public ResultScanner getScanner(byte[] family, byte[] qualifier)
      throws IOException {
    return getTable().getScanner(family, qualifier);
  }

  @Override
  public void put(Put put) throws IOException {
    getTable().put(put);
  }

  @Override
  public void put(List<Put> puts) throws IOException {
    getTable().put(puts);
  }

  @Override
  public boolean checkAndPut(byte[] row, byte[] family, byte[] qualifier,
      byte[] value, Put put) throws IOException {
    return getTable().checkAndPut(row, family, qualifier, value, put);
  }

  @Override
  public void delete(Delete delete) throws IOException {
    getTable().delete(delete);
  }

  @Override
  public void delete(List<Delete> deletes) throws IOException {
    getTable().delete(deletes);
    
  }

  @Override
  public boolean checkAndDelete(byte[] row, byte[] family, byte[] qualifier,
      byte[] value, Delete delete) throws IOException {
    return getTable().checkAndDelete(row, family, qualifier, value, delete);
  }

  @Override
  public Result increment(Increment increment) throws IOException {
    return getTable().increment(increment);
  }

  @Override
  public long incrementColumnValue(byte[] row, byte[] family, byte[] qualifier,
      long amount) throws IOException {
    return getTable().incrementColumnValue(row, family, qualifier, amount);
  }

  @Deprecated
  @Override
  public long incrementColumnValue(byte[] row, byte[] family, byte[] qualifier,
      long amount, boolean writeToWAL) throws IOException {
    return getTable().incrementColumnValue(row, family, qualifier, amount,
        writeToWAL);
  }

  @Override
  public void flushCommits() throws IOException {
    for (HTable table : pool) {
      table.flushCommits();
    }
  }

  @Override
  public void batch(List<? extends Row> actions, Object[] results)
      throws IOException, InterruptedException {
    getTable().batch(actions, results);
    
  }

  @Override
  public void mutateRow(RowMutations rm) throws IOException {
    getTable().mutateRow(rm);    
  }

  @Override
  public Result append(Append append) throws IOException {
    return getTable().append(append);
  }

  @Override
  public void setAutoFlush(boolean autoFlush) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setAutoFlush(boolean autoFlush, boolean clearBufferOnFail){
    // TODO Auto-generated method stub
  }

  @Override
  public long getWriteBufferSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setWriteBufferSize(long writeBufferSize) throws IOException {
    // TODO Auto-generated method stub
  }

  @Override
  public TableName getName() {
    return tableName;
  }

  @Override
  public Boolean[] exists(List<Get> gets) throws IOException {
    return getTable().exists(gets);
  }

  @Override
  public <R> void
      batchCallback(List<? extends Row> actions, Object[] results, Callback<R> callback)
          throws IOException, InterruptedException {
    getTable().batchCallback(actions, results, callback);
    
  }

  @Deprecated
  @Override
  public <R> Object[] batchCallback(List<? extends Row> actions, Callback<R> callback)
      throws IOException, InterruptedException {
    return getTable().batchCallback(actions, callback);
  }

  @Override
  public long incrementColumnValue(byte[] row, byte[] family, byte[] qualifier, long amount,
      Durability durability) throws IOException {
    return getTable().incrementColumnValue(row, family, qualifier, amount,durability);
  }

  @Override
  public CoprocessorRpcChannel coprocessorService(byte[] row) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T extends Service, R> Map<byte[], R> coprocessorService(Class<T> service,
      byte[] startKey, byte[] endKey, Call<T, R> callable) throws Throwable {
    return getTable().coprocessorService(service, startKey, endKey, callable);
  }

  @Override
  public <T extends Service, R> void coprocessorService(Class<T> service, byte[] startKey,
      byte[] endKey, Call<T, R> callable, Callback<R> callback) throws Throwable {
    getTable().coprocessorService(service, startKey, endKey, callable, callback);
  }

  @Override
  public void setAutoFlushTo(boolean autoFlush) {
    // TODO Auto-generated method stub    
  }

  @Override
  public <R extends Message> Map<byte[], R> batchCoprocessorService(
      MethodDescriptor methodDescriptor, Message request, byte[] startKey, byte[] endKey,
      R responsePrototype) throws Throwable {
    return getTable().batchCoprocessorService(methodDescriptor, request, startKey, endKey, responsePrototype);
  }

  @Override
  public <R extends Message> void batchCoprocessorService(MethodDescriptor methodDescriptor,
      Message request, byte[] startKey, byte[] endKey, R responsePrototype, Callback<R> callback)
      throws Throwable {
    getTable().batchCoprocessorService(methodDescriptor, request, startKey, endKey, responsePrototype, callback);
    
  }

  @Deprecated
  @Override
  public Object[] batch(List<? extends Row> actions) throws IOException, InterruptedException {
    return getTable().batch(actions);
  }

  @Override
  public boolean checkAndMutate(byte[] arg0, byte[] arg1, byte[] arg2, CompareOp arg3, byte[] arg4,
      RowMutations arg5) throws IOException {
    // TODO Auto-generated method stub
    return false;
  }
}
