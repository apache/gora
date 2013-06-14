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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema.Field;
import org.apache.avro.specific.SpecificData;
import org.apache.gora.util.LicenseHeaders;
import org.apache.gora.util.TimingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Generate specific Java interfaces and classes for protocols and schemas. 
 *  GoraCompiler takes its inspiration from, and is largely based on Avro's {@link SpecificCompiler}.
 */
public class GoraCompiler {
  private File dest;
  private Writer out;
  private Set<Schema> queue = new HashSet<Schema>();
  private static final Logger log = LoggerFactory.getLogger(GoraCompiler.class);
  private static LicenseHeaders licenseHeader = new LicenseHeaders(null);
  private final static String SCHEMA_EXTENTION = ".avsc";

  private GoraCompiler(File dest) {
    this.dest = dest;                             // root directory for output
  }
      
  /** Generates Java interface and classes for a protocol.
   * @param src the source Avro protocol file
   * @param dest the directory to place generated files in
   */
  public static void compileProtocol(File src, File dest) throws IOException {
    log.info("Compiling Protocol: " + src + " to: " + dest);
    if(licenseHeader != null) {
      log.info("The generated file will be " + licenseHeader.getLicenseName() + " licensed.");
    }
    GoraCompiler compiler = new GoraCompiler(dest);
    Protocol protocol = Protocol.parse(src);
    for (Schema s : protocol.getTypes())          // enqueue types 
      compiler.enqueue(s);
    compiler.compileInterface(protocol);          // generate interface
    compiler.compile();                           // generate classes for types
  }

  /** Generates Java classes for a schema. */
  public static void compileSchema(File src, File dest) throws IOException {
    log.info("Compiling Schema: " + src + " to: " + dest);
    if(licenseHeader != null) {
      log.info("The generated artifact will be " + licenseHeader.getLicenseName() + " licensed.");
    }
    GoraCompiler compiler = new GoraCompiler(dest);
    compiler.enqueue(Schema.parse(src));          // enqueue types
    compiler.compile();                           // generate classes for types
  }
  
  /** Generates Java classes for a number of schema files. */
  public static void compileSchema(File[] srcFiles, File dest) throws IOException {
  if(licenseHeader != null) {
  log.info("The generated artifact will be " + licenseHeader.getLicenseName() + " licensed.");
   }
       for (File src : srcFiles) {
        log.info("Compiling Schema: " + src + " to: " + dest);
        GoraCompiler compiler = new GoraCompiler(dest);
        compiler.enqueue(Schema.parse(src));          // enqueue types
        compiler.compile();                           // generate classes for types
  	  }
  	}

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

  /** Recursively enqueue schemas that need a class generated. */
  private void enqueue(Schema schema) throws IOException {
    if (queue.contains(schema)) return;
    switch (schema.getType()) {
    case RECORD:
      queue.add(schema);
      for (Field field : schema.getFields())
        enqueue(field.schema());
      break;
    case MAP:
      enqueue(schema.getValueType());
      break;
    case ARRAY:
      enqueue(schema.getElementType());
      break;
    case UNION:
      for (Schema s : schema.getTypes())
        enqueue(s);
      break;
    case ENUM:
    case FIXED:
      queue.add(schema);
      break;
    case STRING: case BYTES:
    case INT: case LONG:
    case FLOAT: case DOUBLE:
    case BOOLEAN: case NULL:
      break;
    default: throw new RuntimeException("Unknown type: "+schema);
    }
  }

  /** Generate java classes for enqueued schemas. */
  private void compile() throws IOException {
    for (Schema schema : queue)
      compile(schema);
  }

  private void compileInterface(Protocol protocol) throws IOException {
    startFile(protocol.getName(), protocol.getNamespace());
    try {
      line(0, "public interface "+protocol.getName()+" {");

      out.append("\n");
      for (Map.Entry<String,Message> e : protocol.getMessages().entrySet()) {
        String name = e.getKey();
        Message message = e.getValue();
        Schema request = message.getRequest();
        Schema response = message.getResponse();
        line(1, unbox(response)+" "+name+"("+params(request)+")");
        line(2,"throws AvroRemoteException"+errors(message.getErrors())+";");
      }
      line(0, "}");
    } finally {
      out.close();
    }
  }

  private void startFile(String name, String space) throws IOException {
    File dir = new File(dest, space.replace('.', File.separatorChar));
    if (!dir.exists())
      if (!dir.mkdirs())
        throw new IOException("Unable to create " + dir);
    name = cap(name) + ".java";
    out = new OutputStreamWriter(new FileOutputStream(new File(dir, name)));
    header(space);
  }

  private void header(String namespace) throws IOException {
    if (licenseHeader != null) {
      line(0, licenseHeader.getLicense());
    }
    if(namespace != null) {
      line(0, "package "+namespace+";\n");
    }
    line(0, "import java.nio.ByteBuffer;");
    line(0, "import java.util.Map;");
    line(0, "import java.util.HashMap;");
    line(0, "import org.apache.avro.Protocol;");
    line(0, "import org.apache.avro.Schema;");
    line(0, "import org.apache.avro.AvroRuntimeException;");
    line(0, "import org.apache.avro.Protocol;");
    line(0, "import org.apache.avro.util.Utf8;");
    line(0, "import org.apache.avro.ipc.AvroRemoteException;");
    line(0, "import org.apache.avro.generic.GenericArray;");
    line(0, "import org.apache.avro.specific.FixedSize;");
    line(0, "import org.apache.avro.specific.SpecificExceptionBase;");
    line(0, "import org.apache.avro.specific.SpecificRecordBase;");
    line(0, "import org.apache.avro.specific.SpecificRecord;");
    line(0, "import org.apache.avro.specific.SpecificFixed;");
    line(0, "import org.apache.gora.persistency.StateManager;");
    line(0, "import org.apache.gora.persistency.impl.PersistentBase;");
    line(0, "import org.apache.gora.persistency.impl.StateManagerImpl;");
    line(0, "import org.apache.gora.persistency.StatefulHashMap;");
    line(0, "import org.apache.gora.persistency.ListGenericArray;");
    for (Schema s : queue)
      if (namespace == null
          ? (s.getNamespace() != null)
          : !namespace.equals(s.getNamespace()))
        line(0, "import "+SpecificData.get().getClassName(s)+";");
    line(0, "");
    line(0, "@SuppressWarnings(\"all\")");
  }

  private String params(Schema request) throws IOException {
    StringBuilder b = new StringBuilder();
    int count = 0;
    for (Field field : request.getFields()) {
      b.append(unbox(field.schema()));
      b.append(" ");
      b.append(field.name());
      if (++count < request.getFields().size())
        b.append(", ");
    }
    return b.toString();
  }

  private String errors(Schema errs) throws IOException {
    StringBuilder b = new StringBuilder();
    for (Schema error : errs.getTypes().subList(1, errs.getTypes().size())) {
      b.append(", ");
      b.append(error.getName());
    }
    return b.toString();
  }

  private void compile(Schema schema) throws IOException {
    startFile(schema.getName(), schema.getNamespace());
    try {
      switch (schema.getType()) {
      case RECORD:
        String type = type(schema);
        line(0, "public class "+ type
             +" extends PersistentBase {");
        // schema definition
        line(1, "public static final Schema _SCHEMA = Schema.parse(\""
             +esc(schema)+"\");");

        //field information
        line(1, "public static enum Field {");
        int i=0;
        for (Field field : schema.getFields()) {
          line(2,toUpperCase(field.name())+"("+(i++)+ ",\"" + field.name() + "\"),");
        }
        line(2, ";");
        line(2, "private int index;");
        line(2, "private String name;");
        line(2, "Field(int index, String name) {this.index=index;this.name=name;}");
        line(2, "public int getIndex() {return index;}");
        line(2, "public String getName() {return name;}");
        line(2, "public String toString() {return name;}");
        line(1, "};");

        StringBuilder builder = new StringBuilder(
        "public static final String[] _ALL_FIELDS = {");
        for (Field field : schema.getFields()) {
          builder.append("\"").append(field.name()).append("\",");
        }
        builder.append("};");
        line(1, builder.toString());

        line(1, "static {");
        line(2, "PersistentBase.registerFields("+type+".class, _ALL_FIELDS);");
        line(1, "}");

        // field declations
        for (Field field : schema.getFields()) {
          line(1,"private "+unbox(field.schema())+" "+field.name()+";");
        }

        //constructors
        line(1, "public " + type + "() {");
        line(2, "this(new StateManagerImpl());");
        line(1, "}");
        line(1, "public " + type + "(StateManager stateManager) {");
        line(2, "super(stateManager);");
        for (Field field : schema.getFields()) {
          Schema fieldSchema = field.schema();
          switch (fieldSchema.getType()) {
          case ARRAY:
            String valueType = type(fieldSchema.getElementType());
            line(2, field.name()+" = new ListGenericArray<"+valueType+">(getSchema()" +
                ".getField(\""+field.name()+"\").schema());");
            break;
          case MAP:
            valueType = type(fieldSchema.getValueType());
            line(2, field.name()+" = new StatefulHashMap<Utf8,"+valueType+">();");
          }
        }
        line(1, "}");

        //newInstance(StateManager)
        line(1, "public " + type + " newInstance(StateManager stateManager) {");
        line(2, "return new " + type + "(stateManager);" );
        line(1, "}");

        // schema method
        line(1, "public Schema getSchema() { return _SCHEMA; }");
        // get method
        line(1, "public Object get(int _field) {");
        line(2, "switch (_field) {");
        i = 0;
        for (Field field : schema.getFields()) {
          line(2, "case "+(i++)+": return "+field.name()+";");
        }
        line(2, "default: throw new AvroRuntimeException(\"Bad index\");");
        line(2, "}");
        line(1, "}");
        // put method
        line(1, "@SuppressWarnings(value=\"unchecked\")");
        line(1, "public void put(int _field, Object _value) {");
        line(2, "if(isFieldEqual(_field, _value)) return;");
        line(2, "getStateManager().setDirty(this, _field);");
        line(2, "switch (_field) {");
        i = 0;
        for (Field field : schema.getFields()) {
          line(2, "case "+i+":"+field.name()+" = ("+
               type(field.schema())+")_value; break;");
          i++;
        }
        line(2, "default: throw new AvroRuntimeException(\"Bad index\");");
        line(2, "}");
        line(1, "}");

        // java bean style getters and setters
        i = 0;
        for (Field field : schema.getFields()) {
          String camelKey = camelCasify(field.name());
          Schema fieldSchema = field.schema();
          switch (fieldSchema.getType()) {
          case INT:case LONG:case FLOAT:case DOUBLE:
          case BOOLEAN:case BYTES:case STRING: case ENUM: case RECORD:
          case FIXED:
            String unboxed = unbox(fieldSchema);
            String fieldType = type(fieldSchema);
            line(1, "public "+unboxed+" get" +camelKey+"() {");
            line(2, "return ("+fieldType+") get("+i+");");
            line(1, "}");
            line(1, "public void set"+camelKey+"("+unboxed+" value) {");
            line(2, "put("+i+", value);");
            line(1, "}");
            break;
          case ARRAY:
            unboxed = unbox(fieldSchema.getElementType());
            fieldType = type(fieldSchema.getElementType());
            line(1, "public GenericArray<"+fieldType+"> get"+camelKey+"() {");
            line(2, "return (GenericArray<"+fieldType+">) get("+i+");");
            line(1, "}");
            line(1, "public void addTo"+camelKey+"("+unboxed+" element) {");
            line(2, "getStateManager().setDirty(this, "+i+");");
            line(2, field.name()+".add(element);");
            line(1, "}");
            break;
          case MAP:
            unboxed = unbox(fieldSchema.getValueType());
            fieldType = type(fieldSchema.getValueType());
            line(1, "public Map<Utf8, "+fieldType+"> get"+camelKey+"() {");
            line(2, "return (Map<Utf8, "+fieldType+">) get("+i+");");
            line(1, "}");
            line(1, "public "+fieldType+" getFrom"+camelKey+"(Utf8 key) {");
            line(2, "if ("+field.name()+" == null) { return null; }");
            line(2, "return "+field.name()+".get(key);");
            line(1, "}");
            line(1, "public void putTo"+camelKey+"(Utf8 key, "+unboxed+" value) {");
            line(2, "getStateManager().setDirty(this, "+i+");");
            line(2, field.name()+".put(key, value);");
            line(1, "}");
            line(1, "public "+fieldType+" removeFrom"+camelKey+"(Utf8 key) {");
            line(2, "if ("+field.name()+" == null) { return null; }");
            line(2, "getStateManager().setDirty(this, "+i+");");
            line(2, "return "+field.name()+".remove(key);");
            line(1, "}");
            break;
          case UNION:
            fieldType = type(fieldSchema);
            //Create get method: public <unbox(field.schema())> get<camelKey>()
            line(1, "public "+unbox(field.schema())+" get" +camelKey+"() {");
            line(2, "return ("+unbox(field.schema())+") get("+i+");");
            line(1, "}");
            
            //Create set methods: public void set<camelKey>(<subschema.fieldType> value)
            for (Schema s : fieldSchema.getTypes()) {
              if (s.getType().equals(Schema.Type.NULL)) continue ;
              String unionFieldType = type(s);
              line(1, "public void set"+camelKey+"("+unionFieldType+" value) {");
              line(2, "put("+i+", value);");
              line(1, "}");
            }
            break;
          case NULL:
            throw new RuntimeException("Unexpected NULL field: "+field);
          default:
            throw new RuntimeException("Unknown field: "+field);
          }
          i++;
        }
        line(0, "}");

        break;
      case ENUM:
        line(0, "public enum "+type(schema)+" { ");
        StringBuilder b = new StringBuilder();
        int count = 0;
        for (String symbol : schema.getEnumSymbols()) {
          b.append(symbol);
          if (++count < schema.getEnumSymbols().size())
            b.append(", ");
        }
        line(1, b.toString());
        line(0, "}");
        break;
      case FIXED:
        line(0, "@FixedSize("+schema.getFixedSize()+")");
        line(0, "public class "+type(schema)+" extends SpecificFixed {}");
        break;
      case MAP: case ARRAY: case UNION: case STRING: case BYTES:
      case INT: case LONG: case FLOAT: case DOUBLE: case BOOLEAN: case NULL:
        break;
      default: throw new RuntimeException("Unknown type: "+schema);
      }
    } finally {
      out.close();
    }
  }

  private static final Schema NULL_SCHEMA = Schema.create(Schema.Type.NULL);

  public static String type(Schema schema) {
    switch (schema.getType()) {
    case RECORD:
    case ENUM:
    case FIXED:
      return schema.getName();
    case ARRAY:
      return "GenericArray<"+type(schema.getElementType())+">";
    case MAP:
      return "Map<Utf8,"+type(schema.getValueType())+">";
    case UNION:
      List<Schema> types = schema.getTypes();     // elide unions with null
      if ((types.size() == 2) && types.contains(NULL_SCHEMA))
        return type(types.get(types.get(0).equals(NULL_SCHEMA) ? 1 : 0));
      return "Object";
    case STRING:  return "Utf8";
    case BYTES:   return "ByteBuffer";
    case INT:     return "Integer";
    case LONG:    return "Long";
    case FLOAT:   return "Float";
    case DOUBLE:  return "Double";
    case BOOLEAN: return "Boolean";
    case NULL:    return "Void";
    default: throw new RuntimeException("Unknown type: "+schema);
    }
  }

  public static String unbox(Schema schema) {
    switch (schema.getType()) {
    case INT:     return "int";
    case LONG:    return "long";
    case FLOAT:   return "float";
    case DOUBLE:  return "double";
    case BOOLEAN: return "boolean";
    default:      return type(schema);
    }
  }

  private void line(int indent, String text) throws IOException {
    for (int i = 0; i < indent; i ++) {
      out.append("  ");
    }
    out.append(text);
    out.append("\n");
  }

  static String cap(String name) {
    return name.substring(0,1).toUpperCase()+name.substring(1,name.length());
  }

  private static String esc(Object o) {
    return o.toString().replace("\"", "\\\"");
  }
  
  /**
   * The main method used to invoke the GoraCompiler. It accepts an input (JSON) avsc 
   * schema file, the target output directory and an optional parameter defining the
   * license header to be used when compiling the avsc into the generated class.
   * If no license is explicitely defined, an ASFv2.0 license header is attributed
   * to all generated files by default.
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: GoraCompiler <schema file> <output dir> [-license <id>]");
      System.err.println("  <schema file>     - individual avsc file to be compiled or a directory path containing avsc files");
      System.err.println("  <output dir>      - output directory for generated Java files");
      System.err.println("  [-license <id>]   - the preferred license header to add to the\n" +
                                           "\t\t      generated Java file. Current options include; \n" +
                                              "\t\t  ASLv2   (Apache Software License v2.0) \n" +
                                              "\t\t  AGPLv3  (GNU Affero General Public License)\n" +
                                              "\t\t  CDDLv1  (Common Development and Distribution License v1.0)\n" +
                                              "\t\t  FDLv13  (GNU Free Documentation License v1.3)\n" +
                                              "\t\t  GPLv1   (GNU General Public License v1.0)\n" +
                                              "\t\t  GPLv2   (GNU General Public License v2.0)\n" +
                                              "\t\t  GPLv3   (GNU General Public License v3.0)\n " +
                                              "\t\t  LGPLv21 (GNU Lesser General Public License v2.1)\n" +
                                              "\t\t  LGPLv3  (GNU Lesser General Public License v2.1)\n") ;
      System.exit(1);
    }
    File inputFile = new File(args[0]);
    File output = new File(args[1]);
    if(!inputFile.exists() || !output.exists()){
    	System.err.println("input file path or output file path doesn't exists.");
    	System.exit(1);
    }
    for (int i = 1; i < args.length; i++) {
      licenseHeader.setLicenseName("ASLv2");
      if ("-license".equals(args[i])) {
        licenseHeader.setLicenseName(args[++i]);
      } 
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    long start = System.currentTimeMillis();
    log.info("GoraCompiler: starting at " + sdf.format(start));
    if(inputFile.isDirectory()) {
    	ArrayList<File> inputSchemas = new ArrayList<File>();
    	File[] listOfFiles= inputFile.listFiles();
    	for (File file : listOfFiles) {
    	    if (file.isFile() && file.exists() && file.getName().endsWith(SCHEMA_EXTENTION)) {
    	    	inputSchemas.add(file);
    	    }
    	}
    compileSchema(inputSchemas.toArray(new File[inputSchemas.size()]), output);
    }
    else if (inputFile.isFile()) {
    	compileSchema(inputFile, output);	
    }
    long end = System.currentTimeMillis();
    log.info("GoraCompiler: finished at " + sdf.format(end) + ", elapsed: " + TimingUtil.elapsedTime(start, end));
    return;
  }

}

