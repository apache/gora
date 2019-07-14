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

package org.apache.gora.avro.mapreduce;

import static org.apache.gora.avro.store.TestAvroStore.WEBPAGE_OUTPUT;

import java.io.IOException;

import org.apache.gora.avro.store.DataFileAvroStore;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mapreduce.DataStoreMapReduceTestBase;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;

/**
 * Mapreduce tests for {@link DataFileAvroStore}.
 */
public class TestDataFileAvroStoreMapReduce extends DataStoreMapReduceTestBase {

  public TestDataFileAvroStoreMapReduce() throws IOException {
    super();
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore() 
    throws IOException {
    DataFileAvroStore<String,WebPage> webPageStore = new DataFileAvroStore<>();
    webPageStore.initialize(String.class, WebPage.class, DataStoreFactory.createProps());
    webPageStore.setOutputPath(WEBPAGE_OUTPUT);
    webPageStore.setInputPath(WEBPAGE_OUTPUT);
    
    return webPageStore;
  }

}
