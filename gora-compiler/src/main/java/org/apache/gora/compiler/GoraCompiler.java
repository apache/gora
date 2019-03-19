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
package org.apache.gora.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.SchemaNormalization;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.gora.compiler.utils.LicenseHeaders;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoraCompiler extends SpecificCompiler {

  private static final Logger LOG = LoggerFactory.getLogger(GoraCompiler.class);

  public static final String DIRTY_BYTES_FIELD_NAME = "__g__dirty";

  public static final int FIRST_UNMANAGED_FIELD_INDEX = 1;

  private static final Set<String> GORA_RESERVED_NAMES = new HashSet<>();
  
  private static final String DEFAULT_TEMPLATES_PATH = "/org/apache/gora/compiler/templates/" ;
  
  static {
    GORA_RESERVED_NAMES.addAll(Arrays.asList(DIRTY_BYTES_FIELD_NAME));
  }

  private static final Set<String> GORA_HIDDEN_FIELD_NAMES = new HashSet<>();
  
  static {
    GORA_HIDDEN_FIELD_NAMES.add(DIRTY_BYTES_FIELD_NAME);
  }
  
  public static void compileSchema(File[] srcFiles, File dest, LicenseHeaders licenseHeader)
      throws IOException {
    Schema.Parser parser = new Schema.Parser();

    for (File src : srcFiles) {
      LOG.info("Compiling: {}", src.getAbsolutePath());
      Schema originalSchema = parser.parse(src);
      //Map<Schema,Schema> queue = new HashMap<>();
      //Schema newSchema = getSchemaWithDirtySupport(originalSchema, queue);
      Schema newSchema = originalSchema;
      GoraCompiler compiler = new GoraCompiler(newSchema);
      compiler.setTemplateDir(DEFAULT_TEMPLATES_PATH);
      compiler.compileToDestination(src, dest);

      //Adding the license to the compiled file
      Path path = Paths.get(generateDestinationFileName(dest.toString(), newSchema));
      String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
      content = licenseHeader.getLicense() + content.substring(content.indexOf("package"));
      Files.write(path, content.getBytes(StandardCharsets.UTF_8));

      LOG.info("Compiled into: {}", dest.getAbsolutePath());
    }
  }

  /**
   * Compiles a single schema. Any subschemas must be included in the own schema.
   * 
   * @param sourceSchema String with the schema definition in json (avro)
   * @param dest Path where .java classes will be written
   * @param templatesPath Path where Gora's velocity templates are. If null, will use DEFAULT_TEMPLATES_PATH
   * @return The compiled resulting Schema
   * @throws IOException
   */
  public static Schema compileSchema(String sourceSchema, File dest, String templatesPath) throws IOException {
    Schema.Parser parser = new Schema.Parser();

    if (templatesPath == null) {
      templatesPath = DEFAULT_TEMPLATES_PATH ;
    }
    
    LOG.info("Compiling source schema from String into {} using templates in {}", dest.getPath(), templatesPath);
    Schema newSchema = parser.parse(sourceSchema);
    GoraCompiler compiler = new GoraCompiler(newSchema);
    compiler.setTemplateDir(templatesPath);
    compiler.compileToDestination(null, dest); // Will always write to destination
    LOG.info("Compiled avro into: {}", dest.getAbsolutePath());
    return newSchema;
  }
  
  public static String generateAppropriateImmutabilityModifier(Schema schema){
    switch (schema.getType()) {
      case BYTES:
        return ".asReadOnlyBuffer()";
      default:
        return "";
    }
  }

  public static String generateAppropriateWrapperOrValue(Schema schema) {
    switch (schema.getType()) {
      case MAP:
        return "(value instanceof org.apache.gora.persistency.Dirtyable) ? "
          + "value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value)";
      case ARRAY:
        return "(value instanceof org.apache.gora.persistency.Dirtyable) ? "
          + "value : new org.apache.gora.persistency.impl.DirtyListWrapper(value)";
      case BYTES:
        return "deepCopyToReadOnlyBuffer(value)";
      default:
        return "value";
    }
  }

  public static String generateAppropriateWrapperOrValueForPut(Schema schema) {
    switch (schema.getType()) {
      case MAP:
        return "(value instanceof org.apache.gora.persistency.Dirtyable) ? "
          + "value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)";
      case ARRAY:
        return "(value instanceof org.apache.gora.persistency.Dirtyable) ? "
          + "value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)";
      default:
        return "value";
    }
  }

  public static String generateAppropriateWrapper(Schema schema, Field field) {
    if (DIRTY_BYTES_FIELD_NAME.equals(field.name())) {
      return "java.nio.ByteBuffer.wrap(new byte["
        + getNumberOfBytesNeededForDirtyBits(schema) + "])";
    } else {
      switch (field.schema().getType()) {
      case RECORD:
        return field.schema().getName()+".newBuilder().build()";
      case MAP:
        return "new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()["+field.pos()+"]))";
      case ARRAY:
        return "new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()["+field.pos()+"]))";
      default:
        return "defaultValue(fields()["+field.pos()+"])";
      }
    }
    
  }
  
  public static String generateAppropriateValue(Field field) {
    switch (field.schema().getType()) {
      case RECORD:
        return field.schema().getName()+".newBuilder().build()";
      case MAP:
        return "new org.apache.gora.persistency.impl.DirtyMapWrapper(new java.util.HashMap())";
      case ARRAY:
        return "new org.apache.gora.persistency.impl.DirtyListWrapper(new java.util.ArrayList())";
      default:
        return "this."+field.name();
    }
  }

  /**
   * Recognizes camel case
   *
   * @param s converts the given input string to camel case
   * @return the converted camel case string
   */
  public static String toUpperCase(String s) {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
      if (i > 0) {
        if (Character.isUpperCase(s.charAt(i))
            && Character.isLowerCase(s.charAt(i - 1))
            && Character.isLetter(s.charAt(i))) {
          builder.append("_");
        }
      }
      builder.append(Character.toUpperCase(s.charAt(i)));
    }

    return builder.toString();
  }

  private static int getNumberOfBytesNeededForDirtyBits(Schema originalSchema) {
    return (int) Math.ceil((originalSchema.getFields().size() + 1) * 0.125);
  }

  public static String generateDirtyMethod(Schema schema, Field field) {
    /*
     * TODO: See AVRO-1127. This is dirty. We need to file a bug in avro to
     * get them to open the API so other compilers can use their utility
     * methods
     */
    String getMethod = generateGetMethod(schema, field);
    String dirtyMethod = "is" + getMethod.substring(3) + "Dirty";
    return dirtyMethod; 
  }

  public static String generateDefaultValueString(Schema schema, String fieldName) {
    if (DIRTY_BYTES_FIELD_NAME.equals(fieldName)) {
      return "java.nio.ByteBuffer.wrap(new byte["
        + getNumberOfBytesNeededForDirtyBits(schema) + "])";
    } else {
      throw new IllegalArgumentException(fieldName
        + " is not a gora managed field.");
    }
  }

  public static boolean isNotHiddenField(String fieldName) {
    return !GORA_HIDDEN_FIELD_NAMES.contains(fieldName);
  }

  GoraCompiler(Schema schema) {
    super(schema);
  }

  private static Schema getSchemaWithDirtySupport(Schema originalSchema, Map<Schema,Schema> queue) throws IOException {
    switch (originalSchema.getType()) {
      case RECORD:
        if (queue.containsKey(originalSchema)) {
          return queue.get(originalSchema);
        }
        return getRecordSchemaWithDirtySupport(originalSchema,queue);
      case UNION:
        return getUnionSchemaWithDirtySupport(originalSchema,queue);
      case MAP:
        return getMapSchemaWithDirtySupport(originalSchema,queue);
      case ARRAY:
        return getArraySchemaWithDirtySupport(originalSchema,queue);
      default:
        return originalSchema;
    }
  }
  
  private static Schema getArraySchemaWithDirtySupport(Schema originalSchema, Map<Schema,Schema> queue) throws IOException {
    return Schema.createArray(getSchemaWithDirtySupport(originalSchema.getElementType(),queue));
  }

  private static Schema getMapSchemaWithDirtySupport(Schema originalSchema, Map<Schema,Schema> queue) throws IOException {
    return Schema.createMap(getSchemaWithDirtySupport(originalSchema.getValueType(),queue));
  }

  private static Schema getUnionSchemaWithDirtySupport(Schema originalSchema, Map<Schema,Schema> queue) throws IOException {
    List<Schema> schemaTypes = originalSchema.getTypes();
    List<Schema> newTypeSchemas = new ArrayList<>();
    for (Schema currentTypeSchema : schemaTypes) {
      newTypeSchemas.add(getSchemaWithDirtySupport(currentTypeSchema, queue));
    }
    return Schema.createUnion(newTypeSchemas);
  }

  private static Schema getRecordSchemaWithDirtySupport(Schema originalSchema, Map<Schema,Schema> queue) throws IOException {
    if (originalSchema.getType() != Type.RECORD) {
      throw new IOException("Gora only supports record schemas.");
    }
    List<Field> originalFields = originalSchema.getFields();
    /* make sure the schema doesn't contain the field __g__dirty */
    for (Field field : originalFields) {
      if (GORA_RESERVED_NAMES.contains(field.name())) {
        throw new IOException(
          "Gora schemas cannot contain the field name " + field.name());
      }
    }
    Schema newSchema = Schema.createRecord(originalSchema.getName(),
    originalSchema.getDoc(), originalSchema.getNamespace(),
    originalSchema.isError());
    
    queue.put(originalSchema, newSchema);
    
    List<Field> newFields = new ArrayList<>();
    byte[] defaultDirtyBytesValue = new byte[getNumberOfBytesNeededForDirtyBits(originalSchema)];
    Arrays.fill(defaultDirtyBytesValue, (byte) 0);
    JsonNode defaultDirtyJsonValue = JsonNodeFactory.instance
      .binaryNode(defaultDirtyBytesValue);
    Field dirtyBits = new Field(DIRTY_BYTES_FIELD_NAME,
      Schema.create(Type.BYTES),
      "Bytes used to represent weather or not a field is dirty.",
      defaultDirtyJsonValue);
    newFields.add(dirtyBits);
    for (Field originalField : originalFields) {
      // recursively add dirty support
      Field newField = new Field(originalField.name(),
        getSchemaWithDirtySupport(originalField.schema(),queue),
        originalField.doc(), originalField.defaultValue(),
        originalField.order());
      newFields.add(newField);
    }
    newSchema.setFields(newFields);
    return newSchema;
  }

  /**
   * Utility method used by velocity templates to generate serialVersionUID on AVRO beans.
   *
   * @param schema Data bean AVRO schema.
   * @return serialVersionUID for Serializable AVRO databeans.
   */
  public static long fingerprint64(Schema schema) {
    return SchemaNormalization.parsingFingerprint64(schema);
  }

  public static String generateDestinationFileName(String destDir, Schema schema) {
    return destDir + File.separatorChar + schema.getNamespace().replace('.', File.separatorChar)
            + File.separatorChar + schema.getName() + ".java";
  }
}
