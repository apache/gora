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

package org.apache.gora.orientdb.store;

import org.apache.gora.persistency.impl.PersistentBase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility builder for create OrientDB mapping from gora-orientdb-mapping.xml.
 */
public class OrientDBMappingBuilder<K, T extends PersistentBase> {

  public static final String ATT_NAME = "name";
  public static final String ATT_TYPE = "type";
  public static final String TAG_CLASS = "class";
  public static final String ATT_KEYCLASS = "keyClass";
  public static final String ATT_DOCUMENT = "document";
  public static final String TAG_FIELD = "field";
  public static final String ATT_FIELD = "docfield";
  public static final Logger log = LoggerFactory.getLogger(OrientDBMapping.class);

  private final OrientDBStore<K, T> dataStore;

  private OrientDBMapping mapping;

  public OrientDBMappingBuilder(final OrientDBStore<K, T> store) {
    this.dataStore = store;
    this.mapping = new OrientDBMapping();
  }

  /**
   * Build OrientDB dataStore mapping from gora-orientdb-mapping.xml given from class path
   * or file system location.
   */
  public OrientDBMapping build() {
    if (mapping.getDocumentClass() == null)
      throw new IllegalStateException("Document Class is not specified.");
    return mapping;
  }

  protected OrientDBMappingBuilder fromFile(String uri) throws IOException {
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      InputStream is = getClass().getResourceAsStream(uri);
      if (is == null) {
        String msg = "Unable to load the mapping from classpath resource '" + uri
                + "' Re-trying local from local file system location.";
        log.warn(msg);
        is = new FileInputStream(uri);
      }
      Document doc = saxBuilder.build(is);
      Element root = doc.getRootElement();
      List<Element> classElements = root.getChildren(TAG_CLASS);
      for (Element classElement : classElements) {
        final Class<T> persistentClass = dataStore.getPersistentClass();
        final Class<K> keyClass = dataStore.getKeyClass();
        if (matchesKeyClassWithMapping(keyClass, classElement)
                && matchesPersistentClassWithMapping(persistentClass, classElement)) {
          loadPersistentClass(classElement, persistentClass);
          break;
        }
      }
    } catch (IOException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    return this;
  }

  private boolean matchesPersistentClassWithMapping(final Class<T> persistentClass,
                                                    final Element classElement) {
    return classElement.getAttributeValue(ATT_NAME).equals(persistentClass.getName());
  }

  private boolean matchesKeyClassWithMapping(final Class<K> keyClass,
                                             final Element classElement) {
    return classElement.getAttributeValue(ATT_KEYCLASS).equals(keyClass.getName());
  }

  private void loadPersistentClass(Element classElement,
                                   Class<T> pPersistentClass) {

    String docClassFromMapping = classElement.getAttributeValue(ATT_DOCUMENT);
    String resolvedDocClass = dataStore.getSchemaName(docClassFromMapping,
            pPersistentClass);
    mapping.setDocumentClass(resolvedDocClass);

    List<Element> fields = classElement.getChildren(TAG_FIELD);
    for (Element field : fields) {
      mapping.registerClassField(field.getAttributeValue(ATT_NAME),
              field.getAttributeValue(ATT_FIELD),
              field.getAttributeValue(ATT_TYPE));
    }
  }

}