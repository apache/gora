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

import org.apache.commons.lang.ArrayUtils;
import org.apache.gora.compiler.GoraCompiler;
import org.apache.gora.compiler.utils.LicenseHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GoraCompilerCLI {

  private static final Logger LOG = LoggerFactory.getLogger(GoraCompilerCLI.class);

  public static void main(String[] args) {
    if(args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))){
      printHelp();
      System.exit(0);
    }
    if(args.length < 2){
      LOG.error("Must supply at least one source file and an output directory.");
      printHelp();
      System.exit(1);
    }
    // Setting the default license header to ASLv2
    LicenseHeaders licenseHeader = new LicenseHeaders("ASLv2");
    // Checking for user provided license
    for (int i = 0; i < args.length; i++) {
      if ("-license".equals(args[i])) {
        if (i == args.length - 1) {
          LOG.error("Must supply a valid license id.");
          printHelp();
          System.exit(1);
        }
        if (licenseHeader.isValidLicense(args[i + 1])) {
          licenseHeader.setLicenseName(args[i + 1]);
          args = (String[]) ArrayUtils.removeElement(args, args[i + 1]);
          args = (String[]) ArrayUtils.removeElement(args, args[i]);
        } else {
          LOG.error("Must supply a valid license id.");
          printHelp();
          System.exit(1);
        }
      }
    }

    File outputDir = new File(args[args.length-1]);
    if(!outputDir.isDirectory()){
      LOG.error("Must supply a directory for output");
      printHelp();
      System.exit(1);
    }
    // Processing input directory or input files
    File inputDir = new File(args[0]);
    File[] inputFiles = null;
    if (inputDir.isDirectory()) {
      if (inputDir.length() > 0)
        inputFiles = inputDir.listFiles();
      else {
        LOG.error("Input directory must include at least one file.");
        printHelp();
        System.exit(1);
      }
    } else {
      inputFiles = new File[args.length - 1];
      for (int i = 0; i < inputFiles.length; i++) {
        File inputFile = new File(args[i]);
        if (!inputFile.isFile()) {
          LOG.error("Input must be a file.");
          printHelp();
          System.exit(1);
        }
        inputFiles[i] = inputFile;
      }
    }
    try {
      GoraCompiler.compileSchema(inputFiles, outputDir, licenseHeader);
      LOG.info("Compiler executed SUCCESSFULL.");
    } catch (IOException e) {
      LOG.error("Error while compiling schema files. Check that the schemas are properly formatted.");
      printHelp();
      throw new RuntimeException(e);
    }
  }

  private static void printHelp() {
    LOG.info("Usage: GoraCompiler <schema file> <output dir> [-license <id>]\n" +
            "   <schema file>     - individual avsc file to be compiled or a directory path containing avsc files\n" +
            "   <output dir>      - output directory for generated Java files\n" +
            "   [-license <id>]   - the preferred license header to add to the generated Java file.\n" +
            "Current License header options include;\n" +
            "\t\t  ASLv2   (Apache Software License v2.0) \n" +
            "\t\t  AGPLv3  (GNU Affero General Public License) \n" +
            "\t\t  CDDLv1  (Common Development and Distribution License v1.0) \n" +
            "\t\t  FDLv13  (GNU Free Documentation License v1.3) \n" +
            "\t\t  GPLv1   (GNU General Public License v1.0) \n" +
            "\t\t  GPLv2   (GNU General Public License v2.0) \n" +
            "\t\t  GPLv3   (GNU General Public License v3.0) \n" +
            "\t\t  LGPLv21 (GNU Lesser General Public License v2.1) \n" +
            "\t\t  LGPLv3  (GNU Lesser General Public License v2.1)"
    );
  }
}
