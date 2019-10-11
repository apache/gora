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
package org.apache.gora.benchmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.gora.compiler.GoraCompiler;
import org.apache.gora.compiler.utils.LicenseHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import site.ycsb.ByteIterator;

/**
 * The Class GoraBenchmarkUtils has some utilities that dynamically generate
 * files needed to for gora. It generate the following files. a. Database
 * Mapping File b. Avro Files c. Data Beans
 */
public class GoraBenchmarkUtils {

  private static final Logger LOG = LoggerFactory.getLogger(GoraBenchmarkUtils.class);

  /**
   * Checks if is field updatable.
   *
   * @param field
   *          the field
   * @param values
   *          the values
   * 
   * @return true, if is field updatable
   */
  public static boolean isFieldUpdatable(String field, Map<String, ByteIterator> values) {
    if (values.get(field) == null) {
      return false;
    }
    return true;
  }

  /**
   * Generate avro schema based on the number of fields. supplied when running
   * the benchmark. These files are json files
   *
   * @param numberOfFields
   *          the number of fields
   */
  public static void generateAvroSchema(int numberOfFields) {
    try {
      File avroFile = new File(Constants.AVRO_FULL_PATH);
      FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record(Constants.RECORD)
          .namespace(Constants.NAMESPACE_VALUE).fields();
      fieldAssembler.name(Constants.USER_ID_VALUE).type().stringType().stringDefault(Constants.NULL);
      for (int fieldCount = 0; fieldCount < numberOfFields; fieldCount++) {
        fieldAssembler.name(Constants.FIELD_PREFIX + fieldCount).type().stringType().stringDefault(Constants.NULL);
      }
      Schema sc = fieldAssembler.endRecord();
      String schemaString = sc.toString();
      JSONObject avroSchema = new JSONObject(schemaString);
      OutputStreamWriter avroWriter = new OutputStreamWriter(new FileOutputStream(avroFile), StandardCharsets.UTF_8);
      avroWriter.write(avroSchema.toString(4));
      avroWriter.close();
    } catch (FileNotFoundException e) {
      LOG.info("Cannot find file " + Constants.AVRO_FULL_PATH, e.getMessage(), e);
    } catch (IOException e) {
      LOG.info("There is a problem generating avro schema", e.getMessage(), e);
    }
  }

  /**
   * Generate database mapping file. Each database has its own mapping syntax.
   * These files generated are xml files
   *
   * @param dbName
   *          the db name
   */
  @SuppressWarnings("unchecked")
  public static void generateMappingFile(String dbName) {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder;
    try {
      docBuilder = documentBuilderFactory.newDocumentBuilder();
      Document mappingDocument = docBuilder.newDocument();
      Element rootNode = mappingDocument.createElement(Constants.GORA_ROOT_ELEMENT);

      mappingDocument.appendChild(rootNode);
      JSONObject jsonObject = generateJSONObject(Constants.AVRO_FULL_PATH);
      Iterator<String> keys = jsonObject.keys();
      String nameSpace = jsonObject.getString(Constants.NAMESPACE_KEY);
      String dataBean = jsonObject.getString(Constants.NAME_KEY);
      String fullNameSpace = nameSpace + "." + dataBean;
      Element mappingClass = mappingDocument.createElement(Constants.CLASS);
      buildMappingDocument(keys, dbName, mappingDocument, rootNode, fullNameSpace, jsonObject, mappingClass);

      // Start building the comment node
      Element commentElement = mappingDocument.getDocumentElement();
      String license = new LicenseHeaders(Constants.ASLV2).getLicense();
      Comment licenseHeader = mappingDocument.createComment(license);
      commentElement.getParentNode().insertBefore(licenseHeader, commentElement);

      // write the content into xml file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      DOMSource source = new DOMSource(mappingDocument);
      StreamResult result = null;
      if (dbName.equals(Constants.MONGODB)) {
        result = new StreamResult(new File(Constants.DB_MAPPING_PATH + "/" + Constants.MONGO_MAPPING_FILE));
      } else if (dbName.equals(Constants.HBASE)) {
        result = new StreamResult(new File(Constants.DB_MAPPING_PATH + "/" + Constants.HBASE_MAPPING_FILE));
      } else if (dbName.equals(Constants.COUCHDB)) {
        result = new StreamResult(new File(Constants.DB_MAPPING_PATH + "/" + Constants.COUCHDB_MAPPING_FILE));
      }
      transformer.transform(source, result);
      // Output to console for testing
      StreamResult consoleResult = new StreamResult(new StringWriter());
      transformer.transform(source, consoleResult);
    } catch (ParserConfigurationException e) {
      LOG.info("There is a parsing the document\n {}", e.getMessage(), e);
    } catch (TransformerConfigurationException e) {
      LOG.info("There is a problem transforming the document \n {}", e.getMessage(), e);
    } catch (TransformerException e) {
      LOG.info("There is a problem in transformation\n {}", e.getMessage(), e);
    }
  }

  /**
   * Gets the input files.
   *
   * @return the input files
   */
  public static File[] getInputFiles() {
    File inputDir = new File(Constants.AVRO_PATH);
    File[] inputFiles = null;
    if (inputDir.isDirectory()) {
      if (inputDir.length() > 0)
        inputFiles = inputDir.listFiles();
    }
    return inputFiles;
  }

  /**
   * Generate data beans.
   */
  public static void generateDataBeans() {
    LicenseHeaders licenseHeader = new LicenseHeaders("ASLv2");
    File[] inputFiles = getInputFiles();
    try {
      GoraCompiler.compileSchema(inputFiles, Constants.BEAN_DESTINATION_DIR, licenseHeader);
    } catch (IOException e) {
      LOG.info("There is a problem generating Data Beans \n {}", e.getMessage(), e);
    }
  }

  /**
   * Generate JSON object.
   *
   * @param fileName
   *          the file name
   * @return the JSON object
   */
  public static JSONObject generateJSONObject(String fileName) {
    JSONObject jsonObject = new JSONObject();
    try {
      String jsonString = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);
      jsonObject = new JSONObject(jsonString);
    } catch (IOException e) {
      LOG.info("There is a problem generating JSON object\n {}", e.getMessage(), e);
    }
    return jsonObject;
  }

  /**
   * Builds the mapping document for a particular data store.
   * @param keys  The keys of the mapping file
   * @param db    The datastore to build the mapping file for
   * @param mappingDocument   The name of the mapping document
   * @param rootNode          The root node of the mapping document
   * @param fullNameSpace     The full name space of the persistent object
   * @param jsonObject        The json object of the avro schema
   * @param mappingClass      The mapping class element
   */
  public static void buildMappingDocument(Iterator<String> keys, String db, Document mappingDocument, Element rootNode,
      String fullNameSpace, JSONObject jsonObject, Element mappingClass) {

    switch (db) {
    case Constants.COUCHDB: {
      setMappingRootNode(mappingClass, mappingDocument, rootNode, fullNameSpace, Constants.COUCHDB);
      while (keys.hasNext()) {
        String currentKey = keys.next();
        if (jsonObject.get(currentKey) instanceof JSONArray) {
          JSONArray mappingFields = jsonObject.getJSONArray(currentKey);
          for (int i = 0; i < mappingFields.length(); i++) {
            JSONObject currentObj = mappingFields.getJSONObject(i);
            Element fields = mappingDocument.createElement(Constants.FIELD);
            Attr name = mappingDocument.createAttribute(Constants.NAME_KEY);
            name.setValue(currentObj.getString(Constants.NAME_KEY));
            fields.setAttributeNode(name);
            mappingClass.appendChild(fields);
          }
        }
      }
    }
      break;
    case Constants.MONGODB: {
      setMappingRootNode(mappingClass, mappingDocument, rootNode, fullNameSpace, Constants.MONGODB);
      while (keys.hasNext()) {
        String currentKey = keys.next();
        if (jsonObject.get(currentKey) instanceof JSONArray) {
          JSONArray mappingFields = jsonObject.getJSONArray(currentKey);
          for (int i = 0; i < mappingFields.length(); i++) {
            JSONObject currentObj = mappingFields.getJSONObject(i);
            Element fields = mappingDocument.createElement(Constants.FIELD);
            Attr name = mappingDocument.createAttribute(Constants.NAME_KEY);
            name.setValue(currentObj.getString(Constants.NAME_KEY));
            fields.setAttributeNode(name);
            // mappingClass.appendChild(fields);
            Attr docfield = mappingDocument.createAttribute(Constants.DOCFIELD);
            docfield.setValue(currentObj.getString(Constants.NAME_KEY));
            fields.setAttributeNode(docfield);
            // mappingClass.appendChild(fields);
            Attr type = mappingDocument.createAttribute(Constants.TYPE);
            type.setValue(currentObj.getString(Constants.TYPE));
            fields.setAttributeNode(type);
            mappingClass.appendChild(fields);
          }
        }
      }
    }
      break;
    case Constants.HBASE: {
      Element mappingDescription = mappingDocument.createElement(Constants.TABLE);
      rootNode.appendChild(mappingDescription);
      // setting attribute to element
      Attr tableAttribute = mappingDocument.createAttribute(Constants.NAME_KEY);
      tableAttribute.setValue(jsonObject.getString(Constants.NAME_KEY).toLowerCase(Locale.ROOT) + "s");
      mappingDescription.setAttributeNode(tableAttribute);
      Element familyName = mappingDocument.createElement(Constants.FAMILY);
      mappingDescription.appendChild(familyName);
      // setting attribute to element
      Attr familyAttribute = mappingDocument.createAttribute(Constants.NAME_KEY);
      familyAttribute.setValue(Constants.INFO);
      familyName.setAttributeNode(familyAttribute);
      mappingClass = mappingDocument.createElement(Constants.CLASS);
      rootNode.appendChild(mappingClass);
      // setting attribute to element
      Attr beanName = mappingDocument.createAttribute(Constants.NAME_KEY);
      beanName.setValue(fullNameSpace);
      mappingClass.setAttributeNode(beanName);
      // setting attribute to element.getJSONArray("type").getString(0)
      Attr keyClass = mappingDocument.createAttribute(Constants.KEYCLASS_ATT);
      keyClass.setValue(Constants.KEYCLASS);
      mappingClass.setAttributeNode(keyClass);
      // setting attribute to elementC:\\cars.xml
      Attr table = mappingDocument.createAttribute(Constants.TABLE);
      table.setValue(Constants.USERS);
      mappingClass.setAttributeNode(table);
      while (keys.hasNext()) {
        String currentKey = keys.next();
        if (jsonObject.get(currentKey) instanceof JSONArray) {
          JSONArray mappingFields = jsonObject.getJSONArray(currentKey);
          for (int i = 0; i < mappingFields.length(); i++) {
            JSONObject currentObj = mappingFields.getJSONObject(i);
            Element fields = mappingDocument.createElement(Constants.FIELD);
            Attr name = mappingDocument.createAttribute(Constants.NAME_KEY);
            name.setValue(currentObj.getString(Constants.NAME_KEY));
            fields.setAttributeNode(name);
            Attr docfield = mappingDocument.createAttribute(Constants.QUALIFIER);
            docfield.setValue(currentObj.getString(Constants.NAME_KEY));
            fields.setAttributeNode(docfield);
            Attr type = mappingDocument.createAttribute(Constants.FAMILY);
            type.setValue(Constants.INFO);
            fields.setAttributeNode(type);
            mappingClass.appendChild(fields);
          }
        }
      }
      break;
    }
    default:
      break;
    }
  }

  /**
   * Gets the data store to benchmark.
   *
   * @param properties the properties
   * @return the data store
   */
  public static String getDataBase(Properties properties) {
    String defaultDataStore = properties.getProperty(Constants.DEFAULT_DATA_STORE_KEY);
    String dataBase = Constants.HBASE;
    switch (defaultDataStore) {
    case Constants.MONGODB_CLASS:
      dataBase = Constants.MONGODB;
      break;
    case Constants.HBASEDB_CLASS:
      dataBase = Constants.HBASE;
      break;
    case Constants.COUCHDB_CLASS:
      dataBase = Constants.COUCHDB;
      break;
    default:
      break;
    }
    return dataBase;
  }

  /**
   * Sets the mapping root node. //MongoDB
   * class keyClass="java.lang.String" name=
   * "org.apache.gora.benchmark.generated.User" document="users" //CouchDB
   * class keyClass="java.lang.String" name=
   * "org.apache.gora.benchmark.generated.User" table="users"
   *
   * @param mappingClass the mapping class
   * @param mappingDocument          the entire mapping documentmappingClass
   * @param rootNode          the node we are setting, top most node
   * @param fullNameSpace          name clearspace of data bean
   * @param dataStore the data store
   */
  public static void setMappingRootNode(Element mappingClass, Document mappingDocument, Element rootNode,
      String fullNameSpace, String dataStore) {
    rootNode.appendChild(mappingClass);
    // setting attribute to element
    Attr beanName = mappingDocument.createAttribute(Constants.NAME_KEY);
    beanName.setValue(fullNameSpace);
    mappingClass.setAttributeNode(beanName);
    // setting attribute to element
    Attr keyClass = mappingDocument.createAttribute(Constants.KEYCLASS_ATT);
    keyClass.setValue(Constants.KEYCLASS);
    mappingClass.setAttributeNode(keyClass);
    // setting attribute to element
    if (dataStore == Constants.MONGODB) {
      Attr collection = mappingDocument.createAttribute(Constants.DOCUMENT);
      collection.setValue(Constants.USERS);
      mappingClass.setAttributeNode(collection);
    } else if (dataStore == Constants.COUCHDB) {
      Attr table = mappingDocument.createAttribute(Constants.TABLE);
      table.setValue(Constants.USERS);
      mappingClass.setAttributeNode(table);
    }
  }

}
