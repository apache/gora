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
package org.apache.gora.redis.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.gora.persistency.impl.PersistentBase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Mapping builder for Redis
 */
public class RedisMappingBuilder<K, T extends PersistentBase> {

  private final RedisStore<K, T> dataStore;

  public RedisMappingBuilder(RedisStore<K, T> dataStore) {
    this.dataStore = dataStore;
  }

  public RedisMapping readMapping(InputStream inputStream) throws IOException {
    try {
      RedisMapping redisMapping = new RedisMapping();
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document dom = db.parse(inputStream);
      Element root = dom.getDocumentElement();
      NodeList classesNodes = root.getElementsByTagName("class");
      for (int indexClasses = 0; indexClasses < classesNodes.getLength(); indexClasses++) {
        Element classElement = (Element) classesNodes.item(indexClasses);
        if (classElement.getAttribute("keyClass").equals(dataStore.getKeyClass().getCanonicalName())
            && classElement.getAttribute("name").equals(dataStore.getPersistentClass().getCanonicalName())) {
          redisMapping.setDatabase(Integer.parseInt(classElement.getAttribute("database")));
          redisMapping.setPrefix(classElement.getAttribute("prefix"));
          NodeList elementsByTagName = classElement.getElementsByTagName("field");
          Map<String, String> mapFields = new HashMap<>();
          Map<String, RedisType> mapTypes = new HashMap<>();
          for (int indexFields = 0; indexFields < elementsByTagName.getLength(); indexFields++) {
            Element item = (Element) elementsByTagName.item(indexFields);
            String name = item.getAttribute("name");
            String column = item.getAttribute("column");
            String type = item.getAttribute("type");
            mapFields.put(name, column);
            mapTypes.put(name, RedisType.valueOf(type));
          }
          redisMapping.setTypes(mapTypes);
          redisMapping.setFields(mapFields);
        }
      }
      return redisMapping;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

}
