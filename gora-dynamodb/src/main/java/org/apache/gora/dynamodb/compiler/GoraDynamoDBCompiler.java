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

package org.apache.gora.dynamodb.compiler;

import static org.apache.gora.dynamodb.store.DynamoDBUtils.MAPPING_FILE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.gora.dynamodb.store.DynamoDBMapping;
import org.apache.gora.dynamodb.store.DynamoDBMapping.DynamoDBMappingBuilder;
import org.apache.gora.util.GoraException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

/** 
 * Generate specific Java classes for defined schemas. 
 * Different from the {@link org.apache.gora.compiler.GoraCompiler}, 
 * which uses an .avsc or .json schema definition, this compiler 
 * expects an XML schema file as input.
 */
public class GoraDynamoDBCompiler {
  private File dest;
  private Writer out;
  private static final Logger log = LoggerFactory.getLogger(GoraDynamoDBCompiler.class);
  private String packageName;

  /**
   * GoraDynamoDBCompiler
   * 
   * @param dest where the data bean will be written.
   */
  private GoraDynamoDBCompiler(File dest) {
    this.dest = dest;
  }

  /** Generates Java classes for a schema. */
  public static void compileSchema(File src, File dest) throws IOException {
    log.info("Compiling {} to {}", src, dest);
    GoraDynamoDBCompiler compiler = new GoraDynamoDBCompiler(dest);
    DynamoDBMapping dynamoDBMap = compiler.readMapping(src);
    if (dynamoDBMap.getTables().isEmpty()) 
      throw new IllegalStateException("There are no tables defined.");

    for(String tableName : dynamoDBMap.getTables().keySet()) {
      compiler.compile(tableName, dynamoDBMap.getKeySchema(tableName), 
          dynamoDBMap.getItems(tableName));
      log.info("{} written without issues to {}", tableName, dest.getAbsolutePath());
    }
  }

  /**
   * Method in charge of compiling a specific table using a key schema and a set
   * of attributes
   *
   * @param pTableName
   * @param arrayList
   * @param map
   */
  private void compile(String pTableName, ArrayList<KeySchemaElement> arrayList, Map<String, String> map){
    try {
      startFile(pTableName, pTableName);
      setHeaders(packageName);
      line(0, "");
      line(0, "@DynamoDBTable(tableName = \"" + pTableName + "\")");
      line(0, "public class " + pTableName + " implements Persistent {");
      for (KeySchemaElement pKeySchema : arrayList) {
        setKeyAttributes(pKeySchema, map.get(pKeySchema.getAttributeName()), 2);
        setKeyMethods(pKeySchema, map.get(pKeySchema.getAttributeName()), 2);
        map.remove(pKeySchema.getAttributeName());
      }
      setItems(map, 2);
      setDefaultMethods(2, pTableName);
      line(0, "}");
      out.flush();
      out.close();
    } catch (IOException e) {
      log.error("Error while compiling table {}",pTableName, e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Receives a list of all items and creates getters and setters for them
   *
   * @param pItems The items belonging to the table
   * @param pIden  The number of spaces used for identation
   * @throws IOException
   */
  private void setItems(Map<String, String> pItems, int pIden)
      throws IOException {
    for (String itemName : pItems.keySet()) {
      String itemType = "String";
      if (pItems.get(itemName).toString().equals("N"))
        itemType = "double";
      if (pItems.get(itemName).toString().equals("SS"))
        itemType = "Set<String>";
      if (pItems.get(itemName).toString().equals("SN"))
        itemType = "Set<double>";
      line(pIden, "private " + itemType + " " + itemName + ";");
      setItemMethods(itemName, itemType, pIden);
    }
    line(0, "");
  }

  /**
   * Creates item getters and setters
   *
   * @param pItemName Item's name
   * @param pItemType Item's type
   * @param pIden     Number of spaces used for indentation
   * @throws IOException
   */
  private void setItemMethods(String pItemName, String pItemType, int pIden)
      throws IOException {
    line(pIden, "@DynamoDBAttribute(attributeName = \""
        + camelCasify(pItemName) + "\")");
    line(pIden, "public " + pItemType + " get" + camelCasify(pItemName)
    + "() {  return " + pItemName + ";  }");
    line(pIden, "public void set" + camelCasify(pItemName) + "(" + pItemType
        + " p" + camelCasify(pItemName) + ") {  this." + pItemName + " = p"
        + camelCasify(pItemName) + ";  }");
    line(0, "");
  }

  /**
   * Creates key getters and setters
   *
   * @param pKeySchema The key schema for a specific table
   * @param pIden      Number of spaces used for indentation
   * @throws IOException
   */
  private void setKeyMethods(KeySchemaElement pKeySchema, String attType,
      int pIden) throws IOException {
    StringBuilder strBuilder = new StringBuilder();
    attType = attType.equals("S") ? "String" : "double";
    // hash key
    if (pKeySchema.getKeyType().equals(KeyType.HASH.toString())) {
      strBuilder.append("@DynamoDBHashKey(attributeName=\""
          + pKeySchema.getAttributeName() + "\") \n");
      strBuilder.append("    public " + attType + " getHashKey() {  return "
          + pKeySchema.getAttributeName() + "; } \n");
      strBuilder.append("    public void setHashKey(" + attType + " ");
      strBuilder.append("p" + camelCasify(pKeySchema.getAttributeName())
      + "){  this." + pKeySchema.getAttributeName());
      strBuilder.append(" = p" + camelCasify(pKeySchema.getAttributeName())
      + "; }");
      line(pIden, strBuilder.toString());
    }
    strBuilder.delete(0, strBuilder.length());
    // range key
    if (pKeySchema.getKeyType().equals(KeyType.RANGE.toString())) {
      strBuilder.append("@DynamoDBRangeKey(attributeName=\""
          + pKeySchema.getAttributeName() + "\") \n");
      strBuilder.append("    public " + attType + " getRangeKey() { return "
          + pKeySchema.getAttributeName() + "; } \n");
      strBuilder.append("    public void setRangeKey(" + attType + " ");
      strBuilder.append("p" + camelCasify(pKeySchema.getAttributeName())
      + "){  this." + pKeySchema.getAttributeName());
      strBuilder.append(" = p" + camelCasify(pKeySchema.getAttributeName())
      + "; }");
      line(pIden, strBuilder.toString());
    }
    line(0, "");
  }

  /**
   * Creates the key attributes within the generated class
   * @param pKeySchema
   *          schema
   * @param attType
   *          attribute type
   * @param pIden
   *          of spaces used for indentation
   * @throws IOException
   */
  private void setKeyAttributes(KeySchemaElement pKeySchema, String attType,
      int pIden) throws IOException {
    StringBuilder strBuilder = new StringBuilder();
    attType = attType.equals("S") ? "String " : "double ";
    if (pKeySchema != null) {
      strBuilder.append("private " + attType);
      strBuilder.append(pKeySchema.getAttributeName() + ";");
      line(pIden, strBuilder.toString());
    }
    strBuilder.delete(0, strBuilder.length());
    line(0, "");
  }

  /**
   * Returns camel case version of a string
   *
   * @param s String to be camelcasified
   * @return
   */
  private static String camelCasify(String s) {
    return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
  }

  /**
   * Starts the java generated class file
   *
   * @param name  Class name
   * @param space spacing
   * @throws IOException
   */
  private void startFile(String name, String space) throws IOException {
    String fullDest = FilenameUtils.normalize
        (dest.getAbsolutePath() + File.separatorChar + packageName.replace('.', File.separatorChar));
    File dir = new File(fullDest);
    if (!dir.exists())
      if (!dir.mkdirs())
        throw new IOException("Unable to create " + dir);
    name = cap(name) + ".java";
    out = new OutputStreamWriter(new FileOutputStream(new File(dir, name)), Charset.defaultCharset());

  }

  /**
   * Sets the necessary imports for the generated java class to work
   * @param namespace
   * @throws IOException
   */
  private void setHeaders(String namespace) throws IOException {
    if (namespace != null) {
      line(0, "package " + namespace + ";\n");
    }
    line(0, "import java.util.List;");
    line(0, "import java.util.Set;");
    line(0, "");
    line(0, "import org.apache.avro.Schema.Field;");
    line(0, "import org.apache.gora.persistency.Persistent;");
    line(0, "import org.apache.gora.persistency.Tombstone;");
    line(0, "import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;");
    line(0, "import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;");
    line(0, "import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;");
    line(0, "import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;");
  }

  /**
   * Creates default methods inherited from upper classes
   * @param pIden 
   *          of spaces used for indentation
   * @param tabName
   *          table name
   * @throws IOException
   */
  private void setDefaultMethods(int pIden, String tabName) throws IOException {
    line(pIden, "public void setNew(boolean pNew){}");
    line(pIden, "public void setDirty(boolean pDirty){}");
    line(pIden, "@Override");
    line(pIden, "public void clear() { }");
    line(pIden, "@Override");
    line(pIden, "public " + tabName + " clone() { return null; }");
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
    line(pIden, "public Tombstone getTombstone() { return null; }");
    line(pIden, "@Override");
    line(pIden, "public List<Field> getUnmanagedFields() { return null; }");
    line(pIden, "@Override");
    line(pIden, "public Persistent newInstance() { return new " + tabName + "(); }");
  }

  /**
   * Writes a line within the output stream
   *
   * @param indent Number of spaces used for indentation
   * @param text   Text to be written
   * @throws IOException
   */
  private void line(int indent, String text) throws IOException {
    for (int i = 0; i < indent; i++) {
      out.append("  ");
    }
    out.append(text);
    out.append("\n");
  }

  /**
   * Returns the string received with the first letter in uppercase
   * @param name 
   *          to be converted
   * @return
   */
  static String cap(String name) {
    return name.substring(0,1).toUpperCase(Locale.getDefault())
        + name.substring(1,name.length());
  }

  /**
   * Start point of the compiler program
   *
   * @param args the schema file to be compiled and where this should be written
   */
  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        log.error("Usage: Compiler <schema file> <output dir>");
        System.exit(1);
      }
      compileSchema(new File(args[0]), new File(args[1]));
    } catch (Exception e) {
      log.error("Something went wrong. Please check the input file.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads the schema file and converts it into a data structure to be used
   * @param pMapFile
   *          schema file to be mapped into a table
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private DynamoDBMapping readMapping(File pMapFile) throws IOException {

    DynamoDBMappingBuilder mappingBuilder = new DynamoDBMappingBuilder();

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(pMapFile);

      if (doc == null || doc.getRootElement() == null)
        throw new GoraException("Unable to load " + MAPPING_FILE
            + ". Please check its existance!");

      Element root = doc.getRootElement();

      List<Element> tableElements = root.getChildren("table");
      boolean keys = false;
      for (Element tableElement : tableElements) {

        String tableName = tableElement.getAttributeValue("name");
        long readCapacUnits = Long.parseLong(tableElement
            .getAttributeValue("readcunit"));
        long writeCapacUnits = Long.parseLong(tableElement
            .getAttributeValue("writecunit"));
        this.packageName = tableElement.getAttributeValue("package");

        mappingBuilder.setProvisionedThroughput(tableName, readCapacUnits,
            writeCapacUnits);
        log.debug("Table properties have been set for name, package and provisioned throughput.");

        // Retrieving attributes
        List<Element> fieldElements = tableElement.getChildren("attribute");
        for (Element fieldElement : fieldElements) {
          String key = fieldElement.getAttributeValue("key");
          String attributeName = fieldElement.getAttributeValue("name");
          String attributeType = fieldElement.getAttributeValue("type");
          mappingBuilder.addAttribute(tableName, attributeName, attributeType);
          // Retrieving key's features
          if (key != null) {
            mappingBuilder.setKeySchema(tableName, attributeName, key);
            keys = true;
          }
        }
        log.debug("Attributes for table '{}' have been read.", tableName);
        if (!keys)
          log.warn("Keys for table '{}' have NOT been set.", tableName);
      }
    } catch(IOException ex) {
      log.error("Error while performing xml mapping.", ex.getMessage());
      throw new RuntimeException(ex);
    } catch(Exception ex) {
      log.error("An error occured whilst reading the xml mapping file!", ex.getMessage());
      throw new IOException(ex);
    }

    return mappingBuilder.build();
  }
}

