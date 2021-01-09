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
package org.apache.gora.neo4j.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.io.IOUtils;
import org.apache.gora.neo4j.store.Neo4jStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Mapping builder for Neo4j
 */
public class Neo4jMappingBuilder<K, T extends PersistentBase> {

  protected static final String XSD_MAPPING_FILE = "gora-neo4j.xsd";
  private final Neo4jStore<K, T> dataStore;

  public Neo4jMappingBuilder(Neo4jStore<K, T> dataStore) {
    this.dataStore = dataStore;
  }

  private String getKeyClassCanonicalName() {
    return dataStore.getKeyClass().getCanonicalName();
  }

  private String getPersistentClassCanonicalName() {
    return dataStore.getPersistentClass().getCanonicalName();
  }

  public Neo4jMapping readMapping(InputStream inputStream, boolean xsdValidation) throws IOException {
    try {
      String mappingstream = IOUtils.toString(inputStream, Charset.defaultCharset());
      Neo4jMapping neo4jmapping = new Neo4jMapping();
      if (xsdValidation) {
        javax.xml.validation.Schema newSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(new StreamSource(getClass().getClassLoader().getResourceAsStream(XSD_MAPPING_FILE)));
        newSchema.newValidator().validate(new StreamSource(IOUtils.toInputStream(mappingstream, Charset.defaultCharset())));
      }
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document dom = db.parse(IOUtils.toInputStream(mappingstream, Charset.defaultCharset()));
      Element root = dom.getDocumentElement();
      NodeList classesNodes = root.getElementsByTagName("class");
      for (int indexClasses = 0; indexClasses < classesNodes.getLength(); indexClasses++) {
        Element classElement = (Element) classesNodes.item(indexClasses);
        if (classElement.getAttribute("keyClass").equals(getKeyClassCanonicalName())
                && classElement.getAttribute("name").equals(getPersistentClassCanonicalName())) {
          neo4jmapping.setLabel(classElement.getAttribute("label"));
          NodeList elementsByTagName = classElement.getElementsByTagName("field");
          Map<String, Property> mapFields = new HashMap<>();
          for (int indexFields = 0; indexFields < elementsByTagName.getLength(); indexFields++) {
            Element item = (Element) elementsByTagName.item(indexFields);
            String name = item.getAttribute("name");
            String column = item.getAttribute("property");
            String type = item.getAttribute("type");
            mapFields.put(name, new Property(column, PropertyTypes.valueOf(type)));
          }
          neo4jmapping.setProperties(mapFields);
          Element nodekey = (Element) classElement.getElementsByTagName("nodeKey").item(0);
          neo4jmapping.setNodeKey(new Property(nodekey.getAttribute("property"), PropertyTypes.valueOf(nodekey.getAttribute("type"))));
        }
      }
      return neo4jmapping;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

}
