/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.solr;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * This class provides Gora Solr Test Driver to run test cases with managed schemas.
 */
public class GoraSolrManagedSchemaTestDriver extends GoraSolrTestDriver {
    private static final Logger logger = LoggerFactory.getLogger(GoraSolrManagedSchemaTestDriver.class);

    //Embedded JettySolr server
    JettySolrRunner solr;

    @Override
    public void setUpClass() throws Exception {
        solr = new JettySolrRunner("src/test/conf/solr-managed-schema","/solr", 9876);
        solr.start();
    }

    @Override
    public void tearDownClass() throws Exception {
        if (solr != null) {
            solr.stop();
            solr = null;
        }
        cleanupDirectoriesFailover();
    }

    /**
     * Simply cleans up Solr's output from the Unit tests.
     * In the case of a failure, it waits 250 msecs and tries again, 3 times in total.
     */
    private void cleanupDirectoriesFailover() {
        int tries = 3;
        while (tries-- > 0) {
            try {
                cleanupDirectories();
                break;
            } catch (Exception e) {
                //ignore exception
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e1) {
                    //ignore exception
                }
            }
        }
    }

    /**
     * Cleans up Solr's temp base directory.
     *
     * @throws Exception
     *    if an error occurs
     */
    private void cleanupDirectories() throws Exception {
        File employeeDirFile = new File("src/test/conf/solr-managed-schema/Employee/data");
        File webpageDirFile = new File("src/test/conf/solr-managed-schema/WebPage/data");
        if (employeeDirFile.exists()) {
            FileUtils.deleteDirectory(employeeDirFile);
        }
        if (webpageDirFile.exists()) {
            FileUtils.deleteDirectory(webpageDirFile);
        }
    }

}
