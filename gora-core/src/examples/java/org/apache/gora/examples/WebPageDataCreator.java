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

package org.apache.gora.examples;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.examples.generated.Metadata;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;

/**
 * Creates and stores some data to be used in the tests.
 */
public class WebPageDataCreator {

  private static final Logger log = LoggerFactory.getLogger(WebPageDataCreator.class);
  
  public static final String[] URLS = {
    "http://foo.com/",
    "http://foo.com/1.html",
    "http://foo.com/2.html",
    "http://bar.com/3.jsp",
    "http://bar.com/1.html",
    "http://bar.com/",
    "http://baz.com/1.jsp&q=barbaz",
    "http://baz.com/1.jsp&q=barbaz&p=foo",
    "http://baz.com/1.jsp&q=foo",
    "http://bazbar.com",
  };
  
  public static HashMap<String, Integer> URL_INDEXES = new HashMap<String, Integer>();
  
  static {
    for(int i=0; i<URLS.length; i++) {
      URL_INDEXES.put(URLS[i], i);
    }  
  }
  
  public static final String[] CONTENTS = {
    "foo baz bar",
    "foo",
    "foo1 bar1 baz1",
    "a b c d e",
    "aa bb cc dd ee",
    "1",
    "2 3",
    "a b b b b b a",
    "a a a",
    "foo bar baz",
  };
  
  public static final int[][] LINKS = {
    {1, 2, 3, 9},
    {3, 9},
    {},
    {9},
    {5},
    {1, 2, 3, 4, 6, 7, 8, 9},
    {1},
    {2},
    {3},
    {8, 1},
  };

  public static final String[][] ANCHORS = {
    {"foo", "foo", "foo", "foo"},
    {"a1", "a2"},
    {},
    {"anchor1"},
    {"bar"},
    {"a1", "a2", "a3", "a4","a5", "a6", "a7", "a8", "a9"},
    {"foo"},
    {"baz"},
    {"bazbar"},
    {"baz", "bar"},
  };

  public static final String[] SORTED_URLS = new String[URLS.length];
  static {
    for (int i = 0; i < URLS.length; i++) {
      SORTED_URLS[i] = URLS[i];
    }
    Arrays.sort(SORTED_URLS);
  }
  
  public static void createWebPageData(DataStore<String, WebPage> dataStore) 
  throws IOException {
	  try{
	    WebPage page;
	    log.info("creating web page data");
	    
	    for(int i=0; i<URLS.length; i++) {
	      page = new WebPage();
	      page.setUrl(new Utf8(URLS[i]));
	      page.setContent(ByteBuffer.wrap(CONTENTS[i].getBytes()));
	      for(String token : CONTENTS[i].split(" ")) {
	        page.addToParsedContent(new Utf8(token));  
	      }
	      
	      for(int j=0; j<LINKS[i].length; j++) {
	        page.putToOutlinks(new Utf8(URLS[LINKS[i][j]]), new Utf8(ANCHORS[i][j]));
	      }
	      
	      Metadata metadata = new Metadata();
	      metadata.setVersion(1);
	      metadata.putToData(new Utf8("metakey"), new Utf8("metavalue"));
	      page.setMetadata(metadata);
	      
	      dataStore.put(URLS[i], page);
	    }
	    dataStore.flush();
	    log.info("finished creating web page data");
  	}
 	catch(Exception e){
 		log.info("error creating web page data");
 	}
  }
  
  public int run(String[] args) throws Exception {
    String dataStoreClass = "org.apache.gora.hbase.store.HBaseStore";
    if(args.length > 0) {
      dataStoreClass = args[0];
    }
    
    DataStore<String,WebPage> store 
      = DataStoreFactory.getDataStore(dataStoreClass, String.class, WebPage.class, new Configuration());
    createWebPageData(store);
    
    return 0;
  }
  
  public static void main(String[] args) throws Exception {
    new WebPageDataCreator().run(args);
  }
}
