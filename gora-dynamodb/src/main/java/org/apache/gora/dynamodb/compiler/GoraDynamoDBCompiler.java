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
/**
 * @author Renato Marroquin Mogrovejo
 */
package org.apache.gora.dynamodb.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.gora.dynamodb.store.DynamoDBMapping;
import org.apache.gora.dynamodb.store.DynamoDBMapping.DynamoDBMappingBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;

/** Generate specific Java classes for defined schemas. */
public class GoraDynamoDBCompiler {
  private File dest;
  private Writer out;
  private static final Logger log = LoggerFactory.getLogger(GoraDynamoDBCompiler.class);

  private GoraDynamoDBCompiler(File dest) {
    this.dest = dest;                             // root directory for output
  }

  /** Generates Java classes for a schema. */
  public static void compileSchema(File src, File dest) throws IOException {
    log.info("Compiling " + src + " to " + dest );
    GoraDynamoDBCompiler compiler = new GoraDynamoDBCompiler(dest);
    DynamoDBMapping dynamoDBMap = compiler.readMapping(src);
    if (dynamoDBMap.getTables().isEmpty())  throw new IllegalStateException("There are not tables defined.");
	
    for(String tableName : dynamoDBMap.getTables().keySet()){
      compiler.compile(tableName, dynamoDBMap.getKeySchema(tableName), dynamoDBMap.getItems(tableName));
    }
  }

  /**
   * Method in charge of compiling a specific table using a key schema and a set of attributes
   * @param pTableName	Table name
   * @param pKeySchema	Key schema used
   * @param pItems		List of items belonging to a specific table
   */
  private void compile(String pTableName, KeySchema pKeySchema, List<Map<String, String>> pItems){
    // TODO define where the generated will go 
    try {
      startFile(pTableName, pTableName);
      setHeaders(null);
      line(0, "");
      line(0, "@DynamoDBTable(tableName = \"" + pTableName + "\")");
      line(0, "public class " + pTableName + " implements Persistent {");
      setKeyAttributes(pKeySchema, 2);
      setKeyMethods(pKeySchema, 2);
      setItems(pItems, 2);
      setDefaultMethods(2);
      line(0, "}");
      out.flush();
      out.close();
    } catch (IOException e) {
      log.error("Error while compiling table " + pTableName);
      e.printStackTrace();
    }
  }
  
  /**
   * Receives a list of all items and creates getters and setters for them
   * @param pItems	The items belonging to the table
   * @param pIden	The number of spaces used for identation
   * @throws IOException
   */
  private void setItems(List<Map<String, String>> pItems, int pIden) throws IOException{
    for(Map<String, String> item : pItems){
      for (String itemName : item.keySet()){
        String itemType = "String";
        if (item.get(itemName).toString().equals("N"))
          itemType = "double";
        if (item.get(itemName).toString().equals("SS"))
          itemType = "Set<String>";
        if (item.get(itemName).toString().equals("SN"))
          itemType = "Set<double>";
        line(pIden, "private " + itemType + " " + itemName + ";");
        setItemMethods(itemName, itemType, pIden);
      }
    }
    line(0, "");
  }
  
  /**
   * Creates item getters and setters
   * @param pItemName	Item's name
   * @param pItemType	Item's type
   * @param pIden		Number of spaces used for indentation
   * @throws IOException
   */
  private void setItemMethods(String pItemName, String pItemType, int pIden) throws IOException{
    line(pIden, "@DynamoDBAttribute(attributeName = \"" + camelCasify(pItemName) + "\")");
    line(pIden, "public " + pItemType + " get" + camelCasify(pItemName) + "() {  return " + pItemName + ";  }");
    line(pIden, "public void set" + camelCasify(pItemName) + "(" + pItemType + " p" + camelCasify(pItemName) + ") {  this." + pItemName + " = p"+ camelCasify(pItemName) +";  }");
    line(0, "");
  }
  
  /**
   * Creates key getters and setters 
   * @param pKeySchema	The key schema for a specific table
   * @param pIden		Number of spaces used for indentation
   * @throws IOException
   */
  private void setKeyMethods(KeySchema pKeySchema, int pIden) throws IOException{
    KeySchemaElement hashKey = pKeySchema.getHashKeyElement();
    KeySchemaElement rangeKey = pKeySchema.getRangeKeyElement();
    StringBuilder strBuilder = new StringBuilder();
    // hash key
    if(hashKey != null){
      strBuilder.append("@DynamoDBHashKey(attributeName=\"" + hashKey.getAttributeName() + "\") \n");
      strBuilder.append("    public String getHashKey() {  return " + hashKey.getAttributeName() + "; } \n");
      strBuilder.append("    public void setHashKey(" + (hashKey.getAttributeType().equals("S")?"String ":"double "));
      strBuilder.append("p" + camelCasify(hashKey.getAttributeName()) + "){  this." + hashKey.getAttributeName());
      strBuilder.append(" = p" + camelCasify(hashKey.getAttributeName()) + "; }");
      line(pIden, strBuilder.toString());
    }
    strBuilder.delete(0, strBuilder.length());
    // range key
    if(rangeKey != null){
      strBuilder.append("@DynamoDBRangeKey(attributeName=\"" + rangeKey.getAttributeName() + "\") \n");
      strBuilder.append("    public String getRangeKey() {  return " + rangeKey.getAttributeName() + "; } \n");
      strBuilder.append("    public void setRangeKey(" + (rangeKey.getAttributeType().equals("S")?"String ":"double "));
      strBuilder.append("p" + camelCasify(rangeKey.getAttributeName()) + "){  this." + rangeKey.getAttributeName());
      strBuilder.append(" = p" + camelCasify(rangeKey.getAttributeName()) + "; }");
      line(pIden, strBuilder.toString());
    }
    line(0, "");
  }
  
  /**
   * Creates the key attributes within the generated class
   * @param pKeySchema		Key schema
   * @param pIden			Number of spaces used for indentation
   * @throws IOException
   */
  private void setKeyAttributes(KeySchema pKeySchema, int pIden) throws IOException{
    KeySchemaElement hashKey = pKeySchema.getHashKeyElement();
    KeySchemaElement rangeKey = pKeySchema.getRangeKeyElement();
    StringBuilder strBuilder = new StringBuilder();
    // hash key
    if(hashKey != null){
      strBuilder.append("private " + (hashKey.getAttributeType().equals("S")?"String ":"double "));
      strBuilder.append(hashKey.getAttributeName() + ";");
      line(pIden, strBuilder.toString());
    }
    strBuilder.delete(0, strBuilder.length());
    // range key
    if(rangeKey != null){
      strBuilder.append("private " + (rangeKey.getAttributeType().equals("S")?"String ":"double "));
      strBuilder.append(rangeKey.getAttributeName() + ";");
      line(pIden, strBuilder.toString());
    }
    line(0, "");
  }
  
  /**
   * Returns camel case version of a string
   * @param s	String to be camelcasified
   * @return
   */
  private static String camelCasify(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  /** Recognizes camel case */
  private static String toUpperCase(String s) {
    StringBuilder builder = new StringBuilder();
    for(int i=0; i<s.length(); i++) {
      if(i > 0) {
        if(Character.isUpperCase(s.charAt(i))
         && Character.isLowerCase(s.charAt(i-1))
         && Character.isLetter(s.charAt(i))) {
            builder.append("_");
        }
      }
      builder.append(Character.toUpperCase(s.charAt(i)));
    }
    return builder.toString();
  }

  /**
   * Starts the java generated class file
   * @param name	Class name
   * @param space
   * @throws IOException
   */
  private void startFile(String name, String space) throws IOException {
    File dir = new File(dest, space.replace('.', File.separatorChar));
    if (!dir.exists())
      if (!dir.mkdirs())
        throw new IOException("Unable to create " + dir);
    name = cap(name) + ".java";
    out = new OutputStreamWriter(new FileOutputStream(new File(dir, name)));

  }

  /**
   * Sets the necessary imports for the generated java class to work
   * @param namespace
   * @throws IOException
   */
  private void setHeaders(String namespace) throws IOException {
    if(namespace != null) {
      line(0, "package "+namespace+";\n");
    }
    line(0, "import java.util.Set;");
    line(0, "import org.apache.gora.persistency.Persistent;");
    line(0, "import org.apache.gora.persistency.StateManager;");
    line(0, "import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAttribute;");
    line(0, "import com.amazonaws.services.dynamodb.datamodeling.DynamoDBHashKey;");
    line(0, "import com.amazonaws.services.dynamodb.datamodeling.DynamoDBRangeKey;");
    line(0, "import com.amazonaws.services.dynamodb.datamodeling.DynamoDBTable;");
  }

  /**
   * Creates default methods inherited from upper classes
   * @param pIden	Number of spaces used for indentation
   * @throws IOException
   */
  private void setDefaultMethods(int pIden) throws IOException {
    line(pIden, "public void setNew(boolean pNew){}");
    line(pIden, "public void setDirty(boolean pDirty){}");
    line(pIden, "@Override");
    line(pIden, "public StateManager getStateManager() { return null; }");
    line(pIden, "@Override");
    line(pIden, "public Persistent newInstance(StateManager stateManager) { return null; }");
    line(pIden, "@Override");
    line(pIden, "public String[] getFields() { return null; }");
    line(pIden, "@Override");
    line(pIden, "public String getField(int index) {	return null; }");
    line(pIden, "@Override");
    line(pIden, "public int getFieldIndex(String field) { return 0; }");
    line(pIden, "@Override");
    line(pIden, "public void clear() { }");
    line(pIden, "@Override");
    line(pIden, "public person clone() {	return null; }");
    line(pIden, "@Override");
    line(pIden, "public boolean isNew() { return false; }");
    line(pIden, "@Override");
    line(pIden, "public void setNew() { }");
    line(pIden, "@Override");
    line(pIden, "public void clearNew() {	}");
    line(pIden, "@Override");
    line(pIden, "public boolean isDirty() { return false; }");
    line(pIden, "@Override");
    line(pIden, "public boolean isDirty(int fieldIndex) { return false; }");
    line(pIden, "@Override");
    line(pIden, "public boolean isDirty(String field) { return false; }");
    line(pIden, "@Override");
    line(pIden, "public void setDirty() { }");
    line(pIden, "@Override");
    line(pIden, "public void setDirty(int fieldIndex) { }");
    line(pIden, "@Override");
    line(pIden, "public void setDirty(String field) { }");
    line(pIden, "@Override");
    line(pIden, "public void clearDirty(int fieldIndex) { }");
    line(pIden, "@Override");
    line(pIden, "public void clearDirty(String field) { }");
    line(pIden, "@Override");
    line(pIden, "public void clearDirty() { }");
    line(pIden, "@Override");
    line(pIden, "public boolean isReadable(int fieldIndex) {	return false; }");
    line(pIden, "@Override");
    line(pIden, "public boolean isReadable(String field) { return false; }");
    line(pIden, "@Override");
    line(pIden, "public void setReadable(int fieldIndex) { }");
    line(pIden, "@Override");
    line(pIden, "public void setReadable(String field) { }");
    line(pIden, "@Override");
    line(pIden, "public void clearReadable(int fieldIndex) { }");
    line(pIden, "@Override");
    line(pIden, "public void clearReadable(String field) { }");
    line(pIden, "@Override");
    line(pIden, "public void clearReadable() { }");
  }

  /**
   * Writes a line within the output stream
   * @param indent	Number of spaces used for indentation
   * @param text	Text to be written
   * @throws IOException
   */
  private void line(int indent, String text) throws IOException {
    for (int i = 0; i < indent; i ++) {
      out.append("  ");
    }
    out.append(text);
    out.append("\n");
  }

  /**
   * Returns the string received with the first letter in uppercase
   * @param name	String to be converted
   * @return
   */
  static String cap(String name) {
    return name.substring(0,1).toUpperCase()+name.substring(1,name.length());
  }

  /**
   * Start point of the compiler program
   * @param args	Receives the schema file to be compiled and where this should be written
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: Compiler <schema file> <output dir>");
      System.exit(1);
    }
    compileSchema(new File(args[0]), new File(args[1]));
  }

  /**
   * Reads the schema file and converts it into a data structure to be used
   * @param pMapFile	The schema file to be mapped into a table
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private DynamoDBMapping readMapping(File pMapFile) throws IOException {

    DynamoDBMappingBuilder mappingBuilder = new DynamoDBMappingBuilder();

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(pMapFile);
      
      Element root = doc.getRootElement();

      List<Element> tableElements = root.getChildren("table");
      for(Element tableElement : tableElements) {
    	  
      String tableName = tableElement.getAttributeValue("name");
      long readCapacUnits = Long.parseLong(tableElement.getAttributeValue("readcunit"));
      long writeCapacUnits = Long.parseLong(tableElement.getAttributeValue("readcunit"));
    	
      mappingBuilder.setTableName(tableName);
      mappingBuilder.setProvisionedThroughput(tableName, readCapacUnits, writeCapacUnits);
      log.debug("Basic table properties have been set: Name, and Provisioned throughput.");
    	
      // Retrieving key's features
      List<Element> fieldElements = tableElement.getChildren("key");
      for(Element fieldElement : fieldElements) {
        String keyName  = fieldElement.getAttributeValue("name");
        String keyType  = fieldElement.getAttributeValue("type");
        String keyAttrType  = fieldElement.getAttributeValue("att-type");
        if(keyType.equals("hash"))
          mappingBuilder.setHashKeySchema(tableName, keyName, keyAttrType);
        else if(keyType.equals("hashrange"))
          mappingBuilder.setHashRangeKeySchema(tableName, keyName, keyAttrType);
      }
      log.debug("Table key schemas have been set.");
    	
      // Retrieving attributes
        fieldElements = tableElement.getChildren("attribute");
        for(Element fieldElement : fieldElements) {
          String attributeName  = fieldElement.getAttributeValue("name");
          String attributeType = fieldElement.getAttributeValue("type");
          mappingBuilder.addAttribute(tableName, attributeName, attributeType, 0);
        }
        log.info("Table attributes have been read.");
      }

    } catch(IOException ex) {
      log.info("Error while performing xml mapping.");
      ex.printStackTrace();
      throw ex;

    } catch(Exception ex) {
      ex.printStackTrace();
      throw new IOException(ex);
    }

    return mappingBuilder.build();
  }
}

