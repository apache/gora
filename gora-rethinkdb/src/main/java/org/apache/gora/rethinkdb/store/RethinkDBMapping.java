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

package org.apache.gora.rethinkdb.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;

/**
 * Maintains mapping between AVRO data bean and RethinkDB document.
 */
public class RethinkDBMapping {

  public static final Logger log = LoggerFactory.getLogger(RethinkDBMapping.class);

  private String documentClass;
  private HashMap<String, String> classToDocument = new HashMap<>();
  private HashMap<String, String> documentToClass = new HashMap<>();
  private HashMap<String, DocumentFieldType> documentFields = new HashMap<>();

  public String getDocumentClass() {
    return documentClass;
  }

  public void setDocumentClass(String documentClass) {
    this.documentClass = documentClass;
  }

  private void registerDocumentField(String name, DocumentFieldType type) {
    if (documentFields.containsKey(name) && (documentFields.get(name) != type))
      throw new IllegalStateException("The field '" + name + "' is already "
              + "registered with a different type.");
    documentFields.put(name, type);
  }

  public void registerClassField(String classFieldName,
                                 String docFieldName, String fieldType) {
    try {
      registerDocumentField(docFieldName,
              DocumentFieldType.valueOf(fieldType.toUpperCase(Locale.getDefault())));
    } catch (final IllegalArgumentException e) {
      throw new IllegalStateException("Declared '" + fieldType
              + "' for class field '" + classFieldName
              + "' is not supported by RethinkDBMapping");
    }

    if (classToDocument.containsKey(classFieldName)) {
      if (!classToDocument.get(classFieldName).equals(docFieldName)) {
        throw new IllegalStateException("The class field '" + classFieldName
                + "' is already registered in the mapping"
                + " with the document field '"
                + classToDocument.get(classFieldName)
                + " which differs from the new one '" + docFieldName + "'.");
      }
    } else {
      classToDocument.put(classFieldName, docFieldName);
      documentToClass.put(docFieldName, classFieldName);
    }
  }

  public String[] getDocumentFields() {
    return documentToClass.keySet().toArray(new String[documentToClass.keySet().size()]);
  }

  public String getDocumentField(String field) {
    return classToDocument.get(field);
  }

  protected DocumentFieldType getDocumentFieldType(String field) {
    return documentFields.get(field);
  }

  public static enum DocumentFieldType {

    BOOLEAN("boolean"),
    INTEGER("integer"),
    LONG("long"),
    FLOAT("float"),
    SHORT("short"),
    DOUBLE("double"),
    STRING("string"),
    DOCUMENT("document"),
    LIST("list"),
    MAP("map");

    private final String stringValue;

    DocumentFieldType(final String s) {
      stringValue = s;
    }

    public String toString() {
      return stringValue;
    }

  }

}
