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

import com.orientechnologies.orient.core.metadata.schema.OType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.metadata.schema.OSchemaShared;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.avro.specific.SpecificRecord;

import java.util.HashMap;
import java.util.Locale;

/**
 * Maintains mapping between AVRO data bean and OrientDB document.
 */
public class OrientDBMapping {

  public static final Logger log = LoggerFactory.getLogger(OrientDBMapping.class);

  private String documentClass;
  private HashMap<String, String> classToDocument = new HashMap<>();
  private HashMap<String, String> documentToClass = new HashMap<>();
  private HashMap<String, DocumentFieldType> documentFields = new HashMap<>();

  /**
   * Returns main OrientDB document class which matches to persistent bean.
   *
   * @return a {@link ODocument} class name.
   */
  public String getDocumentClass() {
    return documentClass;
  }

  /**
   * Setter for main OrientDB document class which matches for persistent bean.
   *
   * @param documentClass {@link ODocument} class name.
   */
  public void setDocumentClass(String documentClass) {
    this.documentClass = documentClass;
  }

  /**
   * Register mapping {@link com.orientechnologies.orient.core.record.impl.ODocument}
   * field name to it s data type {@link OType}
   *
   * @param name {@link ODocument} class name.
   * @param type {@link DocumentFieldType} field data type.
   */
  private void registerDocumentField(String name, DocumentFieldType type) {
    if (OSchemaShared.checkClassNameIfValid(name) != null) {
      throw new IllegalArgumentException("'" + name
              + "' is an invalid field name for a OrientDB document. '"
              + OSchemaShared.checkClassNameIfValid(name) + "' is not a valid character.");
    }
    if (documentFields.containsKey(name) && (documentFields.get(name) != type))
      throw new IllegalStateException("The field '" + name + "' is already "
              + "registered with a different type.");
    documentFields.put(name, type);
  }

  /**
   * Register mapping between AVRO {@link SpecificRecord}
   * record field, ODocument field and it's type.
   *
   * @param classFieldName {@link SpecificRecord} field name.
   * @param docFieldName   {@link ODocument} field name.
   * @param fieldType      {@link DocumentFieldType} field data type as string.
   */
  public void registerClassField(String classFieldName,
                                 String docFieldName, String fieldType) {
    try {
      registerDocumentField(docFieldName,
              DocumentFieldType.valueOf(fieldType.toUpperCase(Locale.getDefault())));
    } catch (final IllegalArgumentException e) {
      throw new IllegalStateException("Declared '" + fieldType
              + "' for class field '" + classFieldName
              + "' is not supported by OrientDBMapping");
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

  /**
   * Returns all fields in AVRO {@link SpecificRecord} record.
   *
   * @return array of fields in string.
   */
  public String[] getDocumentFields() {
    return documentToClass.keySet().toArray(new String[documentToClass.keySet().size()]);
  }

  /**
   * Return ODocument name given it's mapped AVRO {@link SpecificRecord}
   * record field name.
   *
   * @param field AVRO record field name in string
   * @return matching ODocument {@link ODocument} field name in string.
   */
  public String getDocumentField(String field) {
    return classToDocument.get(field);
  }

  /**
   * Return ODocument name given it's mapped AVRO {@link SpecificRecord}
   * record field name.
   *
   * @param field AVRO record field name in string
   * @return matching ODocument {@link ODocument} field name in string.
   */
  protected DocumentFieldType getDocumentFieldType(String field) {
    return documentFields.get(field);
  }

  /**
   * Currently supporting data types from OrientDB data types  {@link OType}
   */
  public static enum DocumentFieldType {

    INTEGER(OType.INTEGER.name()),
    LONG(OType.LONG.name()),
    FLOAT(OType.FLOAT.name()),
    SHORT(OType.SHORT.name()),
    DOUBLE(OType.DOUBLE.name()),
    STRING(OType.STRING.name()),
    ANY(OType.ANY.name()),
    TRANSIENT(OType.TRANSIENT.name()),
    BINARY(OType.BINARY.name()),
    DATE(OType.DATE.name()),
    DATETIME(OType.DATETIME.name()),
    EMBEDDED(OType.EMBEDDED.name()),
    EMBEDDEDLIST(OType.EMBEDDEDLIST.name()),
    EMBEDDEDSET(OType.EMBEDDEDSET.name()),
    EMBEDDEDMAP(OType.EMBEDDEDMAP.name());

    private final String stringValue;

    DocumentFieldType(final String s) {
      stringValue = s;
    }

    public String toString() {
      return stringValue;
    }
  }

}
