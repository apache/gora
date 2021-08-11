/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.elasticsearch.store;

import java.util.ArrayList;
import java.util.List;

/**
 * Class with the collection info returned by ElasticsearchStoreMetadataAnalyzer with the information
 * about an ElasticsearchStore collection.
 */
public class ElasticsearchStoreCollectionMetadata {

  /**
   * Collection document keys present in a given collection at ElasticsearchStore.
   */
  private List<String> documentKeys = new ArrayList<>();

  /**
   * Collection document types present in a given collection at ElasticsearchStore.
   */
  private List<String> documentTypes = new ArrayList<>();

  public List<String> getDocumentKeys() {
    return documentKeys;
  }

  public void setDocumentKeys(List<String> documentKeys) {
    this.documentKeys = documentKeys;
  }

  public List<String> getDocumentTypes() {
    return documentTypes;
  }

  public void setDocumentTypes(List<String> documentTypes) {
    this.documentTypes = documentTypes;
  }
}
