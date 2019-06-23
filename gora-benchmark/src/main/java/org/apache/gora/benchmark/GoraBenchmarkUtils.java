package org.apache.gora.benchmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.gora.compiler.GoraCompiler;
import org.apache.gora.compiler.utils.*;

import com.yahoo.ycsb.ByteIterator;

/**
 * @author sc306avroWriter.print("");
 *
 */
public class GoraBenchmarkUtils {
  private static final String AVRO_PATH = "src/main/avro";
  private static final String AVRO_FILE = "user.json";
  private static String FIELD_PREFIX = "field";
  private static final String USER_ID_VALUE = "userId";
  private static final String AVRO_FULL_PATH = AVRO_PATH + "/" + AVRO_FILE;
  private static final String NULL = "null";
  private static final String RECORD = "User";
  private static final String NAMESPACE_VALUE = "generated";
  private static final String NAMESPACE_KEY = "namespace";
  private static final String NAME_KEY = "name";
  private static final String MONGODB = "mongodb";
  private static final String HBASE = "hbase";
  private static final String KEYCLASS = "java.lang.String";
  private static String DB_MAPPING_PATH = "src/main/resources";
  private static final String MONGO_MAPPING_FILE = "gora-mongodb-mapping.xml";
  private static final String HBASE_MAPPING_FILE = "gora-hbase-mapping.xml";
  
  private static final File BEAN_DESTINATION_DIR = new File("src/main/java/");
  private static final String DEFAULT_DATA_STORE_KEY = "gora.datastore.default";
  
  //MongoDB Settings
  
  //HBase Settings

  public static boolean isFieldUpdatable(String field, HashMap<String, ByteIterator> values) {
    if (values.get(field) == null) {
      return false;
    }
    return true;
  }

  /**
   * Generate schema file. This is prerequisite for performance benchmarking
   * 
   * @param numberOfFields,
   *          the number of fields in an object.
   */
  public void generateAvroSchema(int numberOfFields) {
    try {
      File avroFile = new File(AVRO_FULL_PATH);
      FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record(RECORD).namespace(NAMESPACE_VALUE).fields();
      fieldAssembler.name(USER_ID_VALUE).type().stringType().stringDefault(NULL);
      for (int i = 0; i < numberOfFields; i++) {
        fieldAssembler.name(FIELD_PREFIX + i).type().stringType().stringDefault(NULL);
      }
      Schema sc = fieldAssembler.endRecord();
      String schemaString = sc.toString();
      JSONObject avroSchema = new JSONObject(schemaString);
      OutputStreamWriter avroWriter = new OutputStreamWriter(new FileOutputStream(avroFile), StandardCharsets.UTF_8);
      avroWriter.write(avroSchema.toString(4));
      avroWriter.close();
    } catch (FileNotFoundException e) {
      //e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      //e.printStackTrace();
    }
  }

  /**
   * @param numberOfFields
   * @param dbName
   */
  @SuppressWarnings("unchecked")
  public void generateMappingFile(String dbName) {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder;
    try {
      docBuilder = documentBuilderFactory.newDocumentBuilder();
      documentBuilderFactory = DocumentBuilderFactory.newInstance();
      Document mappingDocument = docBuilder.newDocument();

      Element rootNode = mappingDocument.createElement("gora-otd");
      mappingDocument.appendChild(rootNode);
      //System.out.println(AVRO_FULL_PATH);
      JSONObject jsonObject = generateJSONObject(AVRO_FULL_PATH);
      Iterator<String> keys = jsonObject.keys();
      String nameSpace = jsonObject.getString(NAMESPACE_KEY);
      String dataBean = jsonObject.getString(NAME_KEY);
      String fullNameSpace = nameSpace + "." + dataBean;

      
      buildMappingDocument(keys, dbName, mappingDocument, rootNode, fullNameSpace, jsonObject);
        
      // write the content into xml file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      DOMSource source = new DOMSource(mappingDocument);
      StreamResult result = null;
      if(dbName.equals(MONGODB)) {
        result = new StreamResult(new File(DB_MAPPING_PATH+"/"+MONGO_MAPPING_FILE));
      }else if(dbName.equals(HBASE)) {
        result = new StreamResult(new File(DB_MAPPING_PATH+"/"+HBASE_MAPPING_FILE));
      } 
      transformer.transform(source, result);

      // Output to console for testing
      StreamResult consoleResult = new StreamResult(new StringWriter());
      transformer.transform(source, consoleResult);
      String xmlString = consoleResult.getWriter().toString();
      //System.out.println(xmlString);
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      //e.printStackTrace();
    } catch (TransformerConfigurationException e) {
      // TODO Auto-generated catch block
      //e.printStackTrace();
    } catch (TransformerException e) {
      // TODO Auto-generated catch blockString
      //e.printStackTrace();
    }
  }
  
  /**
   * @return
   */
  public File[] getInputFiles() {
    File inputDir = new File(AVRO_PATH);
    File[] inputFiles = null;
    if (inputDir.isDirectory()) {
      if (inputDir.length() > 0)
        inputFiles = inputDir.listFiles();
    }
    return inputFiles;
  }
  
  /**
   * 
   */
  public void generateDataBeans() {
    LicenseHeaders licenseHeader = new LicenseHeaders("ASLv2");
    File[] inputFiles = getInputFiles();
    try {
      GoraCompiler.compileSchema(inputFiles, BEAN_DESTINATION_DIR, licenseHeader);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      //e.printStackTrace();
    }
  }

  /**
   * @param fileName
   * @return
   */
  public JSONObject generateJSONObject(String fileName) {
    JSONObject jsonObject = new JSONObject();
    try {
      String jsonString = new String(Files.readAllBytes(Paths.get(fileName)),StandardCharsets.UTF_8);
      jsonObject = new JSONObject(jsonString);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      //e.printStackTrace();
    }
    return jsonObject;
  }

  /**
   * @param keysmillis
   * @param db
   * @param mappingDocument
   * @param rootNode
   * @param fullNameSpace
   * @param jsonObject
   */
  public void buildMappingDocument(Iterator<String> keys, String db, Document mappingDocument, Element rootNode,
      String fullNameSpace, JSONObject jsonObject) {
    switch (db) {
    case MONGODB: {
      Element mappingClass = mappingDocument.createElement("class");
      rootNode.appendChild(mappingClass);

      // setting attribute to element
      Attr beanName = mappingDocument.createAttribute("name");
      beanName.setValue(fullNameSpace);
      mappingClass.setAttributeNode(beanName);

      // setting attribute to element
      Attr keyClass = mappingDocument.createAttribute("keyClass");
      keyClass.setValue(KEYCLASS);
      mappingClass.setAttributeNode(keyClass);

      // setting attribute to element
      Attr collection = mappingDocument.createAttribute("document");
      collection.setValue("users");
      mappingClass.setAttributeNode(collection);
      while (keys.hasNext()) {
        String currentKey = keys.next();
        if (jsonObject.get(currentKey) instanceof JSONArray) {
          JSONArray mappingFields = jsonObject.getJSONArray(currentKey);
          for (int i = 0; i < mappingFields.length(); i++) {
            JSONObject currentObj = mappingFields.getJSONObject(i);
            Element fields = mappingDocument.createElement("field");

            Attr name = mappingDocument.createAttribute("name");
            name.setValue(currentObj.getString("name"));
            fields.setAttributeNode(name);
            // mappingClass.appendChild(fields);

            Attr docfield = mappingDocument.createAttribute("docfield");
            docfield.setValue(currentObj.getString("name"));
            fields.setAttributeNode(docfield);
            // mappingClass.appendChild(fields);

            Attr type = mappingDocument.createAttribute("type");
            type.setValue(currentObj.getString("type"));
            fields.setAttributeNode(type);
            mappingClass.appendChild(fields);
          }
        }
      }
    }

      break;
    case HBASE:{
      {
        Element mappingDescription = mappingDocument.createElement("table");
        rootNode.appendChild(mappingDescription);
        
        // setting attribute to element
        Attr tableAttribute = mappingDocument.createAttribute("name");
        tableAttribute.setValue(jsonObject.getString("name").toLowerCase(Locale.ROOT)+"s");
        mappingDescription.setAttributeNode(tableAttribute);
        
        Element familyName = mappingDocument.createElement("family");
        mappingDescription.appendChild(familyName);
        
        // setting attribute to element
        Attr familyAttribute = mappingDocument.createAttribute("name");
        familyAttribute.setValue("info");
        familyName.setAttributeNode(familyAttribute);
        
        
        Element mappingClass = mappingDocument.createElement("class");
        rootNode.appendChild(mappingClass);

        // setting attribute to element
        Attr beanName = mappingDocument.createAttribute("name");
        beanName.setValue(fullNameSpace);
        mappingClass.setAttributeNode(beanName);

        // setting attribute to element.getJSONArray("type").getString(0)
        Attr keyClass = mappingDocument.createAttribute("keyClass");
        keyClass.setValue(KEYCLASS);
        mappingClass.setAttributeNode(keyClass);

        // setting attribute to elementC:\\cars.xml
        Attr table = mappingDocument.createAttribute("table");
        table.setValue("users");
        mappingClass.setAttributeNode(table);
        while (keys.hasNext()) {
          String currentKey = keys.next();
          if (jsonObject.get(currentKey) instanceof JSONArray) {
            JSONArray mappingFields = jsonObject.getJSONArray(currentKey);
            for (int i = 0; i < mappingFields.length(); i++) {
              JSONObject currentObj = mappingFields.getJSONObject(i);
              Element fields = mappingDocument.createElement("field");

              Attr name = mappingDocument.createAttribute("name");
              name.setValue(currentObj.getString("name"));
              fields.setAttributeNode(name);
              // mappingClass.appendChild(fields);

              Attr docfield = mappingDocument.createAttribute("qualifier");
              docfield.setValue(currentObj.getString("name"));
              fields.setAttributeNode(docfield);
              // mappingClass.appendChild(fields);

              Attr type = mappingDocument.createAttribute("family");
              type.setValue("info");
              fields.setAttributeNode(type);
              mappingClass.appendChild(fields);
            }
          }
        }
      }
      break;
    }

    default:
      break;
    }
  }
  
  public String getDataStore(Properties p) {
    String defaultDataStore = p.getProperty(DEFAULT_DATA_STORE_KEY);
    String dataStore = "hbase";
    switch (defaultDataStore) {
    case "org.apache.gora.mongodb.store.MongoStore":
      dataStore = "mongodb";
      break;
    case "org.apache.gora.mongodb.store.HBaseStore":
      dataStore = "hbase";
      break;
    default:
      break;
    }
    return dataStore;
  }

}
