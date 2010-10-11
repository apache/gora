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
package org.apache.gora.sql.statement;

import java.util.ArrayList;

import org.apache.gora.util.StringUtils;

/** A SQL SELECT statement */
public class SelectStatement {
  
  private String selectStatement;
  private ArrayList<String> selectList;
  private String from;
  private Where where;
  private String groupBy;
  private String having;
  private String orderBy;
  private boolean orderByAsc = true; //whether ascending or descending
  private long offset = -1;
  private long limit = -1 ;
  private boolean semicolon = true;
  
  public SelectStatement() {
    this.selectList = new ArrayList<String>();
  }
  
  public SelectStatement(String from) {
    this();
    this.from = from;
  }
  
  public SelectStatement(String selectList, String from, String where,
      String orderBy) {
    this.selectStatement = selectList;
    this.from = from;
    setWhere(where);
    this.orderBy = orderBy;
  }
  
  public SelectStatement(String selectList, String from, Where where,
      String groupBy, String having, String orderBy, boolean orderByAsc,
      int offset, int limit, boolean semicolon) {
    super();
    this.selectStatement = selectList;
    this.from = from;
    this.where = where;
    this.groupBy = groupBy;
    this.having = having;
    this.orderBy = orderBy;
    this.orderByAsc = orderByAsc;
    this.offset = offset;
    this.limit = limit;
    this.semicolon = semicolon;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("SELECT ");
    if(selectStatement != null)
      builder.append(selectStatement);
    else
      StringUtils.join(builder, selectList);
    append(builder, "FROM", from);
    append(builder, "WHERE", where);
    append(builder, "GROUP BY", groupBy);
    append(builder, "HAVING", having);
    append(builder, "ORDER BY", orderBy);
    if(orderBy != null)
      builder.append(" ").append(orderByAsc?" ASC ":" DESC ");
    if(limit > 0)
      builder.append(" LIMIT ").append(limit);
    if(offset >= 0)
      builder.append(" OFFSET ").append(offset);
    if(semicolon)
      builder.append(";");
    return builder.toString();
  }
  
  /** Adds a part to the Where clause connected with AND */
  public void addWhere(String part) {
    if(where == null)
      where = new Where();
    where.addPart(part);
  }
  
  /** Appends the clause if not null */
  static void append(StringBuilder builder, String sqlClause, Object clause ) {
    if(clause != null && !clause.toString().equals("")) {
      builder.append(" ").append(sqlClause).append(" ").append(clause.toString());
    }
  }

  public void setSelectStatement(String selectStatement) {
    this.selectStatement = selectStatement;
  }
  
  public String getSelectStatement() {
    return selectStatement;
  }

  public ArrayList<String> getSelectList() {
    return selectList;
  }
  
  public void setSelectList(ArrayList<String> selectList) {
    this.selectList = selectList;
  }
  
  public void addToSelectList(String selectField) {
    selectList.add(selectField);
  }
  
  /**
   * @return the from
   */
  public String getFrom() {
    return from;
  }

  /**
   * @param from the from to set
   */
  public void setFrom(String from) {
    this.from = from;
  }

  /**
   * @return the where
   */
  public Where getWhere() {
    return where;
  }

  /**
   * @param where the where to set
   */
  public void setWhere(Where where) {
    this.where = where;
  }
  
  /**
   * @param where the where to set
   */
  public void setWhere(String where) {
    this.where = new Where(where);
  }

  /**
   * @return the groupBy
   */
  public String getGroupBy() {
    return groupBy;
  }

  /**
   * @param groupBy the groupBy to set
   */
  public void setGroupBy(String groupBy) {
    this.groupBy = groupBy;
  }

  /**
   * @return the having
   */
  public String getHaving() {
    return having;
  }

  /**
   * @param having the having to set
   */
  public void setHaving(String having) {
    this.having = having;
  }

  /**
   * @return the orderBy
   */
  public String getOrderBy() {
    return orderBy;
  }

  /**
   * @param orderBy the orderBy to set
   */
  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }

  /**
   * @return the orderByAsc
   */
  public boolean isOrderByAsc() {
    return orderByAsc;
  }

  /**
   * @param orderByAsc the orderByAsc to set
   */
  public void setOrderByAsc(boolean orderByAsc) {
    this.orderByAsc = orderByAsc;
  }

  /**
   * @return the offset
   */
  public long getOffset() {
    return offset;
  }

  /**
   * @param offset the offset to set
   */
  public void setOffset(long offset) {
    this.offset = offset;
  }

  /**
   * @return the limit
   */
  public long getLimit() {
    return limit;
  }

  /**
   * @param limit the limit to set
   */
  public void setLimit(long limit) {
    this.limit = limit;
  }

  /**
   * @return the semicolon
   */
  public boolean isSemicolon() {
    return semicolon;
  }

  /**
   * @param semicolon the semicolon to set
   */
  public void setSemicolon(boolean semicolon) {
    this.semicolon = semicolon;
  }
  
}