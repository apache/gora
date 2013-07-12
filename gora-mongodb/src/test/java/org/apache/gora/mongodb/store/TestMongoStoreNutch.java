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
package org.apache.gora.mongodb.store;

import org.apache.gora.mapreduce.GoraOutputFormat;
//import org.apache.gora.mongodb.beans.tests.WebPage;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
//import org.apache.nutch.crawl.InjectorJob.UrlMapper;
//import org.apache.nutch.storage.StorageUtils;
//import org.apache.nutch.util.NutchJob;
//import org.apache.nutch.util.ToolUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

@Ignore("Needs Nutch configuration")
public class TestMongoStoreNutch {

    /**
     * For this test to work, it is necessary to provide:
     * - a plugin directory with at least one urlnormalizer
     * - include the jar of the used plugins in the classpath
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    /*@Test
    public void testNutchInjection() throws IOException, ClassNotFoundException, InterruptedException {
        Path input = new Path("src/it/resources/test-nutch-inject.csv");
        Configuration conf = new Configuration();
        conf.set("storage.data.store.class", "org.apache.gora.mongodb.store.MongoStore");
        conf.set("plugin.folders", "/home/grdscarabe/PROJECTS/DictaLab/workspace-nutch/nutch-2.1/runtime/local/plugins"); // FIXME
        conf.set("plugin.auto-activation", "true");
        conf.set("plugin.includes", "urlnormalizer-basic");
        conf.set("plugin.excludes", "");

        HashMap<String, Object> results = new HashMap<String, Object>();
        Job currentJob = new NutchJob(conf, "inject " + input);
        FileInputFormat.addInputPath(currentJob, input);
        currentJob.setMapperClass(UrlMapper.class);
        currentJob.setMapOutputKeyClass(String.class);
        currentJob.setMapOutputValueClass(WebPage.class);
        currentJob.setOutputFormatClass(GoraOutputFormat.class);
        DataStore<String, org.apache.nutch.storage.WebPage> store =
                StorageUtils.createWebStore(currentJob.getConfiguration(),
                        String.class, org.apache.nutch.storage.WebPage.class);
        GoraOutputFormat.setOutput(currentJob, store, true);
        currentJob.setReducerClass(Reducer.class);
        currentJob.setNumReduceTasks(0);
        currentJob.waitForCompletion(true);
        ToolUtil.recordJobStatus(null, currentJob, results);
    }
    */
}
