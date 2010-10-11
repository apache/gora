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
package org.apache.gora.cassandra.store;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.util.Utf8;
import org.apache.cassandra.thrift.TokenRange;
import org.apache.gora.cassandra.client.CassandraClient;
import org.apache.gora.cassandra.client.Mutate;
import org.apache.gora.cassandra.client.Row;
import org.apache.gora.cassandra.client.Select;
import org.apache.gora.cassandra.client.SimpleCassandraClient;
import org.apache.gora.cassandra.query.CassandraPartitionQuery;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.query.CassandraResult;
import org.apache.gora.persistency.ListGenericArray;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.State;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.StatefulHashMap;
import org.apache.gora.persistency.StatefulMap;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.ByteUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * DataStore for Cassandra.
 *
 * <p> Note: CassandraStore is not thread-safe. </p>
 */
public class CassandraStore<K, T extends Persistent>
extends DataStoreBase<K, T> {

  private static final String ERROR_MESSAGE =
    "Cassandra does not support creating or modifying ColumnFamilies during runtime";

  private static final String DEFAULT_MAPPING_FILE = "gora-cassandra-mapping.xml";

  private static final int SPLIT_SIZE = 65536;

  private static final int BATCH_COUNT = 256;

  private CassandraClient client;

  private Map<String, CassandraColumn> columnMap;

  private CassandraMapping mapping;

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) throws IOException {
    super.initialize(keyClass, persistentClass, properties);

    String mappingFile =
      DataStoreFactory.getMappingFile(properties, this, DEFAULT_MAPPING_FILE);

    readMapping(mappingFile);
  }

  @Override
  public String getSchemaName() {
    return mapping.getKeySpace();
  }

  @Override
  public void createSchema() throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public void deleteSchema() throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean schemaExists() throws IOException {
    return true;
  }

  public CassandraClient getClientByLocation(String endPoint) {
    return client;
  }

  public Select createSelect(String[] fields) {
    Select select = new Select();
    if (fields == null) {
      fields = beanFactory.getCachedPersistent().getFields();
    }
    for (String f : fields) {
      CassandraColumn col = columnMap.get(f);
      Schema fieldSchema = fieldMap.get(f).schema();
      switch (fieldSchema.getType()) {
        case MAP:
        case ARRAY:
          if (col.isSuperColumn()) {
            select.addAllColumnsForSuperColumn(col.family, col.superColumn);
          } else {
            select.addColumnAll(col.family);
          }
          break;
        default:
          if (col.isSuperColumn()) {
            select.addColumnName(col.family, col.superColumn, col.column);
          } else {
            select.addColumnName(col.family, col.column);
          }
          break;
      }
    }
    return select;
  }

  @Override
  public T get(K key, String[] fields) throws IOException {
    if (fields == null) {
      fields = beanFactory.getCachedPersistent().getFields();
    }
    Select select = createSelect(fields);
    try {
      Row result = client.get(key.toString(), select);
      return newInstance(result, fields);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("rawtypes")
  private void setField(T persistent, Field field, StatefulMap map) {
    persistent.put(field.pos(), map);
  }

  private void setField(T persistent, Field field, byte[] val)
  throws IOException {
    persistent.put(field.pos()
        , ByteUtils.fromBytes(val, field.schema(), datumReader, persistent.get(field.pos())));
  }

  @SuppressWarnings("rawtypes")
  private void setField(T persistent, Field field, GenericArray list) {
    persistent.put(field.pos(), list);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public T newInstance(Row result, String[] fields)
  throws IOException {
    if(result == null)
      return null;

    T persistent = newPersistent();
    StateManager stateManager = persistent.getStateManager();
    for (String f : fields) {
      CassandraColumn col = columnMap.get(f);
      Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();
      Map<String, byte[]> qualMap;
      switch(fieldSchema.getType()) {
        case MAP:
          if (col.isSuperColumn()) {
            qualMap = result.getSuperColumn(col.family, col.superColumn);
          } else {
            qualMap = result.getColumn(col.family);
          }
          if (qualMap == null) {
            continue;
          }
          Schema valueSchema = fieldSchema.getValueType();
          StatefulMap map = new StatefulHashMap();
          for (Entry<String, byte[]> e : qualMap.entrySet()) {
            Utf8 mapKey = new Utf8(e.getKey());
            map.put(mapKey, ByteUtils.fromBytes(e.getValue(), valueSchema, datumReader, null));
            map.putState(mapKey, State.CLEAN);
          }
          setField(persistent, field, map);
          break;
        case ARRAY:
          if (col.isSuperColumn()) {
            qualMap = result.getSuperColumn(col.family, col.superColumn);
          } else {
            qualMap = result.getColumn(col.family);
          }
          if (qualMap == null) {
            continue;
          }
          valueSchema = fieldSchema.getElementType();
          ArrayList arrayList = new ArrayList();
          for (Entry<String, byte[]> e : qualMap.entrySet()) {
            arrayList.add(ByteUtils.fromBytes(e.getValue(), valueSchema, datumReader, null));
          }
          ListGenericArray arr = new ListGenericArray(fieldSchema, arrayList);
          setField(persistent, field, arr);
          break;
        default:
          byte[] val;
          if (col.isSuperColumn()) {
            val = result.get(col.family, col.superColumn, col.column);
          } else {
            val = result.get(col.family, col.column);
          }
          if (val == null) {
            continue;
          }
          setField(persistent, field, val);
          break;
      }
    }
    stateManager.clearDirty(persistent);
    return persistent;
  }

  @Override
  public void put(K key, T obj) throws IOException {
    Mutate mutate = new Mutate();
    Schema schema = obj.getSchema();
    StateManager stateManager = obj.getStateManager();
    List<Field> fields = schema.getFields();
    String qual;
    byte[] value;
    for (int i = 0; i < fields.size(); i++) {
      if (!stateManager.isDirty(obj, i)) {
        continue;
      }
      Field field = fields.get(i);
      Type type = field.schema().getType();
      Object o = obj.get(i);
      CassandraColumn col = columnMap.get(field.name());

      switch(type) {
      case MAP:
        if(o instanceof StatefulMap) {
          @SuppressWarnings("unchecked")
          StatefulMap<Utf8, ?> map = (StatefulMap<Utf8, ?>) o;
          for (Entry<Utf8, State> e : map.states().entrySet()) {
            Utf8 mapKey = e.getKey();
            switch (e.getValue()) {
            case DIRTY:
              qual = mapKey.toString();
              value = ByteUtils.toBytes(map.get(mapKey), field.schema().getValueType(), datumWriter);
              if (col.isSuperColumn()) {
                mutate.put(col.family, col.superColumn, qual, value);
              } else {
                mutate.put(col.family, qual, value);
              }
              break;
            case DELETED:
              qual = mapKey.toString();
              if (col.isSuperColumn()) {
                mutate.delete(col.family, col.superColumn, qual);
              } else {
                mutate.delete(col.family, qual);
              }
              break;
            }
          }
        } else {
          @SuppressWarnings({ "rawtypes", "unchecked" })
          Set<Map.Entry> set = ((Map)o).entrySet();
          for(@SuppressWarnings("rawtypes") Entry entry: set) {
            qual = entry.getKey().toString();
            value = ByteUtils.toBytes(entry.getValue().toString());
            if (col.isSuperColumn()) {
              mutate.put(col.family, col.superColumn, qual, value);
            } else {
              mutate.put(col.family, qual, value);
            }
          }
        }
        break;
      case ARRAY:
        if(o instanceof GenericArray) {
          @SuppressWarnings("rawtypes")
          GenericArray arr = (GenericArray) o;
          int j=0;
          for(Object item : arr) {
            value = ByteUtils.toBytes(item.toString());
            if (col.isSuperColumn()) {
              mutate.put(col.family, col.superColumn, Integer.toString(j), value);
            } else {
              mutate.put(col.family, Integer.toString(j), value);
            }
            j++;
          }
        }
        break;
      default:
        value = ByteUtils.toBytes(o, field.schema(), datumWriter);
        if (col.isSuperColumn()) {
          mutate.put(col.family, col.superColumn, col.column, value);
        } else {
          mutate.put(col.family, col.column, value);
        }
        break;
      }
    }

    if(!mutate.isEmpty())
      client.mutate(key.toString(), mutate);
  }

  @Override
  public boolean delete(K key) throws IOException {
    Mutate mutate = new Mutate();
    for (String family : mapping.getColumnFamilies()) {
      mutate.deleteAll(family);
    }

    client.mutate(key.toString(), mutate);
    return true;
  }

  @Override
  public void flush() throws IOException { }

  @Override
  public void close() throws IOException {
    client.close();
  }

  @Override
  public Query<K, T> newQuery() {
    return new CassandraQuery<K, T>(this);
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws IOException {
    return new CassandraResult<K, T>(this, query, BATCH_COUNT);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
  throws IOException {
    List<PartitionQuery<K,T>> partitions = new ArrayList<PartitionQuery<K,T>>();

    List<TokenRange> rangeList = client.describeRing();
    for (TokenRange range : rangeList) {
      List<String> tokens =
        client.describeSplits(range.start_token, range.end_token, SPLIT_SIZE);
      // turn the sub-ranges into InputSplits
      String[] endpoints = range.endpoints.toArray(new String[range.endpoints.size()]);
      // hadoop needs hostname, not ip
      for (int i = 0; i < endpoints.length; i++) {
          endpoints[i] = InetAddress.getByName(endpoints[i]).getHostName();
      }

      for (int i = 1; i < tokens.size(); i++) {
        CassandraPartitionQuery<K, T> partitionQuery =
          new CassandraPartitionQuery<K, T>(query, tokens.get(i - 1), tokens.get(i), endpoints, SPLIT_SIZE);
        partitions.add(partitionQuery);
      }
    }
    return partitions;
  }

  private CassandraClient createClient() throws IOException {
    String serverStr =
      DataStoreFactory.findPropertyOrDie(properties, this, "servers");
    String[] server1Parts = serverStr.split(",")[0].split(":");
    try {
      return new SimpleCassandraClient(server1Parts[0],
          Integer.parseInt(server1Parts[1]), mapping.getKeySpace());
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  protected void readMapping(String filename) throws IOException {

    mapping = new CassandraMapping();
    columnMap = new HashMap<String, CassandraColumn>();

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader()
          .getResourceAsStream(filename));

      List<Element> classes = doc.getRootElement().getChildren("class");

      for(Element classElement: classes) {
        if(classElement.getAttributeValue("keyClass").equals(keyClass.getCanonicalName())
            && classElement.getAttributeValue("name").equals(
                persistentClass.getCanonicalName())) {

          String keySpace = classElement.getAttributeValue("keyspace");
          mapping.setKeySpace(keySpace);
          client = createClient();
          Map<String, Map<String, String>> keySpaceDesc = client.describeKeySpace();
          for (Entry<String, Map<String, String>> e : keySpaceDesc.entrySet()) {
            boolean isSuper = e.getValue().get("Type").equals("Super");
            mapping.addColumnFamily(e.getKey(), isSuper);
          }

          List<Element> fields = classElement.getChildren("field");

          for(Element field:fields) {
            String fieldName = field.getAttributeValue("name");
            String path = field.getAttributeValue("path");
            String[] parts = path.split(":");
            String columnFamily = parts[0];
            String superColumn = null;
            String column = null;

            boolean isSuper = mapping.isColumnFamilySuper(columnFamily);
            if (isSuper) {
              superColumn = parts[1];
              if (parts.length == 3) {
                column = parts[2];
              }
            } else {
              if (parts.length == 2) {
                column = parts[1];
              }
            }

            columnMap.put(fieldName,
                new CassandraColumn(columnFamily, superColumn, column));
          }

          break;
        }
      }
    } catch(Exception ex) {
      throw new IOException(ex);
    }
  }
}