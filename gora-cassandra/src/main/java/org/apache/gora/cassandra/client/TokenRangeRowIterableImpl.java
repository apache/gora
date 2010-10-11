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
import java.util.Iterator;
import java.util.List;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.util.StringUtils;

class TokenRangeRowIterableImpl implements RowIterable {

  private static final Log LOG = LogFactory.getLog(RowIterable.class);

  private final SimpleCassandraClient client;

  private String startToken;

  private String endToken;

  private final int batchCount;

  private Select select;

  private List<Row> rows;

  private int rowIndex;

  TokenRangeRowIterableImpl(SimpleCassandraClient client,
      String startToken, String endToken, int batchCount,
      Select select) {
    this.client = client;
    this.startToken = startToken;
    this.endToken = endToken;
    this.batchCount = batchCount;
    this.select = select;
    this.rows = null;
    this.rowIndex = 0;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void maybeInit() throws IOException {
    if (rows != null && rowIndex >= rows.size()) {
      rows = null;
    }

    if (rows != null) {
      return;
    }

    rows = client.getTokenRangeIntl(startToken, endToken, batchCount, select);

    if (rows.isEmpty()) {
      rows = null;
      return;
    }

    rowIndex = 0;
    Row lastRow = rows.get(rows.size() - 1);
    IPartitioner p = DatabaseDescriptor.getPartitioner();
    startToken = p.getTokenFactory().toString(p.getToken(lastRow.getKey()));
  }

  @Override
  public Iterator<Row> iterator() {
    return new Iterator<Row>() {
      @Override
      public boolean hasNext() {
        try {
          maybeInit();
        } catch (IOException e) {
          LOG.warn(StringUtils.stringifyException(e));
          return false;
        }
        return rows != null;
      }

      @Override
      public Row next() {
        return rows.get(rowIndex++);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

}
