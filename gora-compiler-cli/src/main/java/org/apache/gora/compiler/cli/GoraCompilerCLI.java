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
package org.apache.gora.compiler.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.gora.compiler.GoraCompiler;


public class GoraCompilerCLI {

  public static void main(String[] args) {
    if(args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))){
      printHelp();
      System.exit(0);
    }
    if(args.length < 2){
      System.err.println("Must supply at least one source file and an output directory.");
      printHelp();
      System.exit(1);
    }
    File outputDir = new File(args[args.length-1]);
    if(!outputDir.isDirectory()){
      System.err.println("Must supply a directory for output");
      printHelp();
      System.exit(1);
    }
    File[] inputs = new File[args.length-1];
    for(int i  = 0; i<inputs.length; i++){
      File inputFile = new File(args[i]);
      if(!inputFile.isFile()){
        System.err.println("Input must be a file.");
        printHelp();
        System.exit(1);
      }
      inputs[i] = inputFile;
    }
    try {
      GoraCompiler.compileSchema(inputs, outputDir);
      System.out.println("Compiler executed SUCCESSFULL.");
    } catch (IOException e) {
      System.err.println("Error while compiling schema files. Check that the schemas are properly formatted.");
      printHelp();
      e.printStackTrace(System.err);
    }
  }

  private static void printHelp() {
    PrintStream out = System.out;
    out.println("Usage: gora-compiler ( -h | --help ) | (<input> [<input>...] <output>)");
  }
}
