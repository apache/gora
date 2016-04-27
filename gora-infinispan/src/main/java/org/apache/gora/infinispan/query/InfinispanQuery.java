/*
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
package org.apache.gora.infinispan.query;

import org.apache.avro.util.Utf8;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.gora.filter.MapFieldValueFilter;
import org.apache.gora.filter.SingleFieldValueFilter;
import org.apache.gora.infinispan.store.InfinispanStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.impl.QueryBase;
import org.apache.hadoop.io.WritableUtils;
import org.infinispan.avro.client.Support;
import org.infinispan.avro.hotrod.QueryBuilder;
import org.infinispan.avro.hotrod.RemoteQuery;
import org.infinispan.query.dsl.FilterConditionContext;
import org.infinispan.query.dsl.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/*
 * * @author Pierre Sutra
 */
public class InfinispanQuery<K,T extends PersistentBase>
extends QueryBase<K,T>
implements PartitionQuery<K,T>, Cloneable{

  public static final Logger LOG = LoggerFactory.getLogger(InfinispanQuery.class);
  private static final String ADDR_DELIMITATOR = ";";

  private RemoteQuery q;
  private InetSocketAddress location;
  private long offset;

  protected String sortingField;
  protected boolean isAscendant;

  public InfinispanQuery(){
    super(null);
    this.localFilterEnabled = false;
    this.setOffset(-1);
    this.isAscendant = true;
    this.sortingField = "";
  }

  public InfinispanQuery(InfinispanStore<K, T> dataStore) {
    super(dataStore);
    this.localFilterEnabled = false;
    this.setOffset(-1);
    this.isAscendant = true;
    this.sortingField = "";
  }

  public boolean isBuilt() {
    LOG.debug("isBuilt()");
    return q!=null;
  }

  public void rebuild(){
    LOG.debug("rebuild()");
    q=null;
    build();
  }

  public void build(){
    LOG.debug("build()");

    FilterConditionContext context = null;

    if(q!=null) {
      LOG.trace("Query already built; ignoring.");
      return;
    }

    QueryBuilder qb = ((InfinispanStore<K,T>)dataStore).getClient().getQueryBuilder();

    if (filter instanceof  MapFieldValueFilter){
      MapFieldValueFilter mfilter = (MapFieldValueFilter) filter;
      if (!(mfilter.getMapKey() instanceof Utf8))
        throw new IllegalAccessError("Invalid map key, must be a string.");
      if (mfilter.getOperands().size()>1)
        throw new IllegalAccessError("MapFieldValueFilter operand not supported.");
      if (!(mfilter.getOperands().get(0) instanceof String))
        throw new IllegalAccessError("Invalid operand, must be a string.");
      String value = mfilter.getMapKey()+ Support.DELIMITER+mfilter.getOperands().get(0).toString();
      switch (mfilter.getFilterOp()) {
      case EQUALS:
        if (value.equals("*")) {
          context = qb.having(mfilter.getFieldName()).like(value);
        } else {
          context = qb.having(mfilter.getFieldName()).eq(value);
        }
        if (!((MapFieldValueFilter) filter).isFilterIfMissing()) {
          LOG.warn("Forcing isFilterMissing to true");
          ((MapFieldValueFilter) filter).setFilterIfMissing(true);
        }
        break;
      case NOT_EQUALS:
        if (value.equals("*")) {
          context = qb.not().having(mfilter.getFieldName()).like(value);
        } else {
          context = qb.not().having(mfilter.getFieldName()).eq(value);
        }
        if (!((MapFieldValueFilter) filter).isFilterIfMissing()) {
          LOG.warn("Forcing isFilterMissing to false");
          ((MapFieldValueFilter) filter).setFilterIfMissing(false);
        }
        break;
      default:
        throw new IllegalAccessError("FilterOp not supported..");
      }

    } else if (filter instanceof  SingleFieldValueFilter){
      SingleFieldValueFilter sfilter = (SingleFieldValueFilter) filter;
      if (sfilter.getOperands().size()>1)
        throw new IllegalAccessError("SingleFieldValueFilter operand not supported.");
      Object value = sfilter.getOperands().get(0);
      switch (sfilter.getFilterOp()) {
      case EQUALS:
        if (value.equals("*")) {
          context = qb.having(sfilter.getFieldName()).like((String)value);
        } else {
          context = qb.having(sfilter.getFieldName()).eq(value);
        }
        break;
      case NOT_EQUALS:
        if (value.equals("*")) {
          context = qb.not().having(sfilter.getFieldName()).like((String)value);
        } else {
          context = qb.not().having(sfilter.getFieldName()).eq(value);
        }
        break;
      case LESS:
        context = qb.having(sfilter.getFieldName()).lt(value);
        break;
      case LESS_OR_EQUAL:
        context = qb.having(sfilter.getFieldName()).lte(value);
        break;
      case GREATER:
        context = qb.having(sfilter.getFieldName()).gt(value);
        break;
      case GREATER_OR_EQUAL:
        context = qb.having(sfilter.getFieldName()).gte(value);
        break;
      default:
        throw new IllegalAccessError("FilterOp not supported..");
      }

    } else if (filter!=null) {
      throw new IllegalAccessError("Filter not supported.");
    }

    if (this.startKey==this.endKey && this.startKey != null ){
      (context == null ? qb : context.and()).having(getPrimaryFieldName()).eq(this.startKey);
    }else{
      if (this.startKey!=null && this.endKey!=null)
        context = (context == null ? qb : context.and()).having(getPrimaryFieldName()).between(this.startKey,this.endKey);
      else if (this.startKey!=null)
        context = (context == null ? qb : context.and()).having(getPrimaryFieldName()).between(this.startKey,null);
      else if (this.endKey!=null)
        (context == null ? qb : context.and()).having(getPrimaryFieldName()).between(null,this.endKey);
    }

    // if projection enabled, keep the primary field.
    if (fields!=null && fields.length > 0) {
      String[] fieldsWithPrimary;
      List<String> fieldsList = new ArrayList<>(Arrays.asList(fields));
      if (!fieldsList.contains(getPrimaryFieldName())) {
        fieldsWithPrimary = Arrays.copyOf(fields, fields.length + 1);
        fieldsWithPrimary[fields.length] = getPrimaryFieldName();
      }else{
        fieldsWithPrimary = fieldsList.toArray(new String[]{});
      }
      qb.setProjection(fieldsWithPrimary);
    }

    qb.orderBy(
        (getSortingField().equals("")) ? getPrimaryFieldName() : getSortingField(),
            isAscendant ? SortOrder.ASC : SortOrder.DESC);

    if (this.getOffset()>=0)
      qb.startOffset(this.getOffset());

    if (this.getLimit()>0)
      qb.maxResults((int) this.getLimit());

    q = (RemoteQuery) qb.build();

    if (location!=null)
      q.setLocation(location);
  }

  public List<T> list(){
    LOG.debug("list()");
    if (!isBuilt()) build();
    return q.list();

  }

  public List<PartitionQuery<K,T>> split() {
    LOG.debug("split()");
    if(!isBuilt()) build();
    List<PartitionQuery<K,T>> splits = new ArrayList<>();
    QueryBuilder qb = ((InfinispanStore<K,T>)dataStore).getClient().getQueryBuilder();
    Collection<RemoteQuery> Queries = qb.split(this.q);
    for (RemoteQuery Query : Queries) {
      InfinispanQuery<K,T> split = (InfinispanQuery<K, T>) this.clone();
      split.q = Query;
      split.location = Query.getLocation();
      splits.add(split);
    }
    LOG.trace(splits.toString());
    return splits;
  }

  public int getResultSize(){
    return q.getResultSize();
  }

  public String getPrimaryFieldName(){
    return ((InfinispanStore)dataStore).getPrimaryFieldName();
  }

  @Override
  public Object clone() {
    InfinispanQuery<K,T> query = null;
    try {
      query = (InfinispanQuery<K, T>) super.clone();
    } catch (CloneNotSupportedException e) {
      // not reachable.
    }
    query.setDataStore(this.getDataStore());
    query.setFilter(this.getFilter());
    query.setFields(this.getFields());
    query.setKeyRange(this.getStartKey(), this.getEndKey());
    query.setConf(this.getConf());
    query.setStartTime(this.getStartTime());
    query.setEndTime(this.getEndTime());
    query.setLocalFilterEnabled(this.isLocalFilterEnabled());
    query.setLimit(this.getLimit());
    query.setOffset(this.getOffset());
    query.setQueryString(this.getQueryString());
    query.setSortingField(this.getSortingField());
    query.setSortingOrder(this.getSortingOrder());
    query.q = this.q;
    query.location = this.location;
    return query;
  }

  @Override
  public String[] getLocations() {
    if (location==null)
      return new String[0];
    String[] result = new String[1];
    result[0] = location.getHostString();
    return result;
  }

  public InetSocketAddress getLocation(){
    return location;
  }

  // FIXME use the write non-null fields function.

  @Override
  @SuppressWarnings("unchecked")
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    sortingField = WritableUtils.readString(in);
    isAscendant = in.readBoolean();
    String locationString = WritableUtils.readString(in);
    if (!locationString.equals(""))
      location = new InetSocketAddress(
          locationString.split(ADDR_DELIMITATOR)[0],
          Integer.valueOf(locationString.split(ADDR_DELIMITATOR)[1]));
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    WritableUtils.writeString(out, getSortingField());
    out.writeBoolean(isAscendant);
    if (location!=null)
      WritableUtils.writeString(out,location.getHostName()+ADDR_DELIMITATOR+location.getPort());
    else
      WritableUtils.writeString(out,"");
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("dataStore", dataStore);
    builder.append("location", location==null ? null :location.toString());
    builder.append("fields", fields);
    builder.append("startKey", startKey);
    builder.append("endKey", endKey);
    builder.append("filter", filter);
    builder.append("limit", limit);
    builder.append("offset", offset);
    builder.append("localFilterEnabled", localFilterEnabled);
    return builder.toString();
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public String getQueryString() {
    return queryString;
  }

  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  public String getSortingField() {
    return sortingField;
  }

  public void setSortingField(String field) {
    sortingField = field;
  }

  public boolean getSortingOrder() {
    return isAscendant;
  }

  public void setSortingOrder(boolean isAscendant) {
    this.isAscendant = isAscendant;
  }

}
