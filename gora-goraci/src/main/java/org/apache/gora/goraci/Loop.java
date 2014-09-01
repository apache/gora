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

package org.apache.gora.goraci;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/** 
 * Executes Generate and Verify in a loop. Data is not cleaned between runs, so each iteration
 * adds more data.
 */
public class Loop extends Configured implements Tool {

  private static final Log LOG = LogFactory.getLog(Loop.class); 
  
  protected void runGenerator(int numMappers, long numNodes, boolean concurrent) throws Exception {
    Generator generator = new Generator();
    generator.setConf(getConf());
    int retCode = generator.run(numMappers, numNodes, concurrent);
    
    if (retCode > 0) {
      throw new RuntimeException("Generator failed with return code: " + retCode);
    }
  }
  
  protected void runVerify(String outputDir, int numReducers, long expectedNumNodes) throws Exception {
    Verify verify = startVerify(outputDir, numReducers, false);
    verify.waitForCompletion();
    checkSuccess(verify, expectedNumNodes);
  }

  private void checkSuccess(Verify verify, long expectedNumNodes) throws Exception {
    if (!verify.isSuccessful()) {
      throw new RuntimeException("Verify.isSuccessful() returned false");
    }

    boolean verifySuccess = verify.verify(expectedNumNodes);
    if (!verifySuccess) {
      throw new RuntimeException("Verify.verify failed");
    }
    
    LOG.info("Verify finished with succees. Total nodes=" + expectedNumNodes);
  }

  protected Verify startVerify(String outputDir, int numReducers, boolean concurrent) throws Exception {
    Path outputPath = new Path(outputDir);
    UUID uuid = UUID.randomUUID(); //create a random UUID.
    Path iterationOutput = new Path(outputPath, uuid.toString());
    
    Verify verify = new Verify();
    verify.setConf(getConf());
    verify.start(iterationOutput, numReducers, concurrent);
    return verify;
  }

  @Override
  public int run(String[] args) throws Exception {
    
    Options options = new Options();
    options.addOption("c", "concurrent", false, "run generation and verification and concurrently");
    
    GnuParser parser = new GnuParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
      if (cmd.getArgs().length != 5) {
        throw new ParseException("Did not see expected # of arguments, saw " + cmd.getArgs().length);
      }
    } catch (ParseException e) {
      System.err.println("Failed to parse command line " + e.getMessage());
      System.err.println();
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(getClass().getSimpleName() + " <num iterations> <num mappers> <num nodes per mapper> <output dir> <num reducers>", options);
      System.exit(-1);
    }
    
    LOG.info("Running Loop with args:" + Arrays.deepToString(cmd.getArgs()));
    
    boolean concurrent = cmd.hasOption("c");
    int numIterations = Integer.parseInt(cmd.getArgs()[0]);
    int numMappers = Integer.parseInt(cmd.getArgs()[1]);
    long numNodes = Long.parseLong(cmd.getArgs()[2]);
    String outputDir = cmd.getArgs()[3];
    int numReducers = Integer.parseInt(cmd.getArgs()[4]);
    
    if (numNodes % Generator.WRAP != 0) {
      throw new RuntimeException("Number of node per mapper is not a multiple of " + String.format("%,d", Generator.WRAP));
    }

    long expectedNumNodes = 0;
    
    if (numIterations < 0) {
      numIterations = Integer.MAX_VALUE; //run indefinitely (kind of)
    }
    
    Verify verify = null;
    long verifyNodes = 0;

    for (int i=0; i < numIterations; i++) {
      LOG.info("Starting iteration = " + i);
      runGenerator(numMappers, numNodes, concurrent);
      expectedNumNodes += numMappers * numNodes;
      
      if (concurrent) {
        if (verify != null) {
          if (verify.isComplete()) {
            checkSuccess(verify, verifyNodes);
            verify = startVerify(outputDir, numReducers, true);
            verifyNodes = expectedNumNodes;
          }
        } else {
          verify = startVerify(outputDir, numReducers, true);
          verifyNodes = expectedNumNodes;
        }
      } else {
        runVerify(outputDir, numReducers, expectedNumNodes);
      }
    }
    
    if (verify != null) {
      verify.waitForCompletion();
      checkSuccess(verify, verifyNodes);
      
      if (verifyNodes != expectedNumNodes)
        runVerify(outputDir, numReducers, expectedNumNodes);
    }

    return 0;
  }
  
  public static void main(String[] args) throws Exception {
    int ret = ToolRunner.run(new Loop(), args);
    System.exit(ret);
  }
  
}
