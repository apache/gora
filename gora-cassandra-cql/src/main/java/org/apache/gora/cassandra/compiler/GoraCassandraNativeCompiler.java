/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.cassandra.compiler;

import org.apache.commons.io.FilenameUtils;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.cassandra.store.CassandraMappingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

/**
 * This class generate Java classes for Cassandra Native Serialization.
 * <p>
 * Generate specific Java classes for defined Gora cassandra mapping.
 * Different from the @see org.apache.gora.compiler.GoraCompiler,
 * which uses an .avsc or .json schema definition, this compiler
 * expects an XML mapping file as input.
 */
public class GoraCassandraNativeCompiler {

  private static final Logger log = LoggerFactory.getLogger(GoraCassandraNativeCompiler.class);

  private Writer out;
  private File dest;

  GoraCassandraNativeCompiler(File dest) {
    this.dest = dest;
  }

  /**
   * Start point of the compiler program
   *
   * @param args the schema file to be compiled and where this should be written
   */
  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        log.error("Usage: Compiler <mapping file> <output dir>");
        System.exit(1);
      }
      compileSchema(new File(args[0]), new File(args[1]));
    } catch (Exception e) {
      log.error("Something went wrong. Please check the input file.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Generates Java classes for a mapping.
   */
  private static void compileSchema(File src, File dest) throws Exception {
    log.info("Compiling {} to {}", src, dest);
    GoraCassandraNativeCompiler compiler = new GoraCassandraNativeCompiler(dest);
    List<CassandraMapping> mappings = readMappingFile(src);
    for (CassandraMapping mapping : mappings) {
      compiler.compile(mapping);
    }
  }

  private static List<CassandraMapping> readMappingFile(File src) throws Exception {
    List<CassandraMapping> mappings = new CassandraMappingBuilder().readMappingFile(src);
    return mappings;
  }

  /**
   * Returns the string received with the first letter in uppercase
   *
   * @param name to be converted
   * @return camelCase String
   */
  static String cap(String name) {
    return name.substring(0, 1).toUpperCase(Locale.getDefault())
            + name.substring(1, name.length());
  }

  /**
   * Method in charge of compiling a specific table using a key schema and a set
   * of attributes
   *
   * @param mapping Cassandra Mapping
   */
  private void compile(CassandraMapping mapping) {
    String fullQualifiedName = mapping.getProperty("name");
    String tableName = mapping.getCoreName();
    String packageName = fullQualifiedName.substring(0, fullQualifiedName.lastIndexOf("."));
    String className = fullQualifiedName.substring(packageName.length() + 1, fullQualifiedName.length());
    String keySpace = mapping.getKeySpace().getName();

    try {
      startFile(className, packageName);
      setHeaders(packageName);
      line(0, "");
      line(0, "@Table(keyspace = \"" + keySpace + "\", name = \"" + tableName + "\"," +
              "readConsistency = \"QUORUM\"," +
              "writeConsistency = \"QUORUM\"," +
              "caseSensitiveKeyspace = false," +
              "caseSensitiveTable = false)");
      line(0, "public class " + className + " implements Persistent {");
      for (Field field : mapping.getFieldList()) {
        processFields(field);
        processGetterAndSetters(field);
        line(2, "");
      }

      setDefaultMethods(2, className);
      line(0, "}");
      out.flush();
      out.close();
    } catch (IOException e) {
      log.error("Error while compiling table {}", className, e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the necessary imports for the generated java class to work
   *
   * @param namespace Namespace
   * @throws IOException
   */
  private void setHeaders(String namespace) throws IOException {
    if (namespace != null) {
      line(0, "package " + namespace + ";\n");
    }
    line(0, "import java.util.List;");
    line(0, "import java.util.Set;");
    line(0, "import java.util.Map;");
    line(0, "import java.util.UUID;");
    line(0, "import java.math.BigDecimal;");
    line(0, "import java.math.BigInteger;");
    line(0, "import java.net.InetAddress;");
    line(0, "import java.nio.ByteBuffer;");
    line(0, "import java.util.Date;");
    line(0, "");
    line(0, "import org.apache.avro.Schema.Field;");
    line(0, "import org.apache.gora.persistency.Persistent;");
    line(0, "import org.apache.gora.persistency.Tombstone;");
    line(0, "import com.datastax.driver.mapping.annotations.Column;");
    line(0, "import com.datastax.driver.mapping.annotations.PartitionKey;");
    line(0, "import com.datastax.driver.mapping.annotations.Table;");
    line(0, "import com.datastax.driver.mapping.annotations.Transient;");
  }

  /**
   * Starts the java generated class file
   *
   * @param name Class name
   * @throws IOException
   */
  private void startFile(String name, String packageName) throws IOException {
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
   * Creates default methods inherited from upper classes
   *
   * @param pIden     of spaces used for indentation
   * @param className class Name
   * @throws IOException
   */
  private void setDefaultMethods(int pIden, String className) throws IOException {
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public void clear() { }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public boolean isDirty() { return false; }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public boolean isDirty(int fieldIndex) { return false; }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public boolean isDirty(String field) { return false; }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public void setDirty() { }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public void setDirty(int fieldIndex) { }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public void setDirty(String field) { }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public void clearDirty(int fieldIndex) { }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public void clearDirty(String field) { }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public void clearDirty() { }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public Tombstone getTombstone() { return null; }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public List<Field> getUnmanagedFields() { return null; }");
    line(pIden, "@Transient");
    line(pIden, "@Override");
    line(pIden, "public Persistent newInstance() { return new " + className + "(); }");
  }

  private void processFields(Field field) throws IOException {
    String fieldName = field.getFieldName();
    String columnName = field.getColumnName();
    if (Boolean.parseBoolean(field.getProperty("primarykey"))) {
      line(2, "@PartitionKey");
    }
    line(2, "@Column(name = \"" + columnName + "\")");
    line(2, "private " + getDataType(field.getType(), false) + " " + fieldName + ";");
  }

  private void processGetterAndSetters(Field field) throws IOException {
    String dataType = getDataType(field.getType(), false);
    line(2, "public " + dataType + " get" + cap(field.getFieldName()) + "() {");
    line(2, "return " + field.getFieldName() + ";");
    line(2, "}");
    line(2, "public void set" + cap(field.getFieldName()) + "(" + dataType + " field) {");
    line(2, field.getFieldName() + " = field;");
    line(2, "}");
  }

  private String getDataType(String dbType, boolean isInner) {
    if (dbType.equalsIgnoreCase("uuid")) {
      return "UUID";
    } else if (dbType.equalsIgnoreCase("text") || dbType.equalsIgnoreCase("ascii") || dbType.equalsIgnoreCase("varchar")) {
      return "String";
    } else if (dbType.equalsIgnoreCase("timestamp")) {
      return "Date";
    } else if (dbType.startsWith("list")) {
      String innerType = dbType.substring(dbType.indexOf("<") + 1, dbType.indexOf(">"));
      return "List<" + getDataType(innerType, true) + ">";
    } else if (dbType.startsWith("set")) {
      String innerType = dbType.substring(dbType.indexOf("<") + 1, dbType.indexOf(">"));
      return "Set<" + getDataType(innerType, true) + ">";
    } else if (dbType.startsWith("map")) {
      String innerTypes = dbType.substring(dbType.indexOf("<") + 1, dbType.indexOf(">"));
      String[] types = innerTypes.split(",");
      return "Map<" + getDataType(types[0], true) + "," + getDataType(types[1], true) + ">";
    } else if (dbType.equalsIgnoreCase("blob")) {
      return "ByteBuffer";
    } else if (dbType.equalsIgnoreCase("int")) {
      if (isInner) {
        return "Integer";
      } else {
        return "int";
      }
    } else if (dbType.equalsIgnoreCase("float")) {
      if (isInner) {
        return "Float";
      } else {
        return "float";
      }
    } else if (dbType.equalsIgnoreCase("double")) {
      if (isInner) {
        return "Double";
      } else {
        return "double";
      }
    } else if (dbType.equalsIgnoreCase("decimal")) {
      return "BigDecimal";
    } else if (dbType.equalsIgnoreCase("bigint") || dbType.equalsIgnoreCase("counter")) {
      return "Long";
    } else if (dbType.equalsIgnoreCase("boolean")) {
      if (isInner) {
        return "Boolean";
      } else {
        return "boolean";
      }
    } else if (dbType.equalsIgnoreCase("varint")) {
      return "BigInteger";
    } else if (dbType.equalsIgnoreCase("inet")) {
      return "InetAddress";
    } else if (dbType.contains("frozen")) {
      throw new RuntimeException("Compiler Doesn't support user define types");
    }
    throw new RuntimeException("Invalid Cassandra DataType");
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
}
