package org.apache.gora.solr;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by madhawa on 5/28/17.
 */
public class GoraSolr6TestDriver extends GoraSolrTestDriver {
    private static final Logger logger = LoggerFactory.getLogger(GoraSolr6TestDriver.class);

    //Embedded JettySolr server
    JettySolrRunner solr;

    @Override
    public void setUpClass() throws Exception {
        solr = new JettySolrRunner("src/test/conf/solr6","/solr", 9876);
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
        File employeeDirFile = new File("src/test/conf/solr6/Employee/data");
        File webpageDirFile = new File("src/test/conf/solr6/WebPage/data");
        if (employeeDirFile.exists()) {
            FileUtils.deleteDirectory(employeeDirFile);
        }
        if (webpageDirFile.exists()) {
            FileUtils.deleteDirectory(webpageDirFile);
        }
    }

}
