/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.goraci.rackspace;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.gora.mock.store.MockDataStore;
import org.apache.gora.store.DataStoreFactory;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.nova.v2_0.predicates.ServerPredicates;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main class for initiating Rackspace cloud
 * topography for use within the GoraCI job.
 * @param <K>
 *
 */
public class RackspaceOrchestration<K> {

  private static final Logger LOG = LoggerFactory.getLogger(RackspaceOrchestration.class);

  private static String RS_CONTINENT = "rackspace.continent";

  private static String RS_USERNAME = "rackspace.username";

  private static String RS_APIKEY = "rackspace.apikey";

  private static String RS_REGION = "rackspace.region";

  private static String RS_IMAGEID = "rackspace.imageid";

  private static String RS_FLAVORID = "rackspace.flavorid";

  private static String RS_SERVERNAME = "rackspace.servername";

  private static String RS_NUM_SERVERS = "rackspace.num.servers";

  /* we can optionally upload our own keys for security, otherwise 
   * we can have Rackspace autocreate one for us*/
  private static String RS_PUBKEY = "rackspace.uploadpubkey";

  /**
   * @param args
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws NoSuchElementException 
   */
  public static void main(String[] args) throws NoSuchElementException, InstantiationException, IllegalAccessException, IOException {
    Properties properties = DataStoreFactory.createProps();
    String rsContinent = DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), 
        RS_CONTINENT, "rackspace-cloudservers-us");
    String rsUser = DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), RS_USERNAME, "asf-gora");
    String rsApiKey = DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), RS_APIKEY, null);
    String rs_region = DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), RS_REGION, "DFW");
    String rs_flavourId = DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), RS_FLAVORID, null);
    String rs_imageId = DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), RS_IMAGEID, null);
    int num_servers = Integer.parseInt(DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), RS_NUM_SERVERS, "10"));
    String serverName = DataStoreFactory.findProperty(properties, MockDataStore.class.newInstance(), RS_SERVERNAME, "goraci_test_server");

    NovaApi novaApi = ContextBuilder.newBuilder(rsContinent).credentials(rsUser, rsApiKey).buildApi(NovaApi.class);
    LOG.info("Defining Rackspace cloudserver continent as: {}, and region: {}.", rsContinent, rs_region);

    //Choose operating system
    ImageApi imageApi = novaApi.getImageApiForZone(rs_region);
    Image image = imageApi.get(rs_imageId);
    //Choose hardware configuration
    FlavorApi flavorApi = novaApi.getFlavorApiForZone(rs_region);
    Flavor flavor = flavorApi.get(rs_flavourId);
    LOG.info("Defining Rackspace cloudserver flavors as: {}, with image id's {}", rs_flavourId, rs_imageId);

    //Manage keypairs
    KeyPairApi keyPairApi = novaApi.getKeyPairExtensionForZone(rs_region).get();

    File keyPairFile = null;
    String publicKey = null;
    //Use your own .pub key which should be on CP
    if (DataStoreFactory.findBooleanProperty(properties, MockDataStore.class.newInstance(), RS_PUBKEY, "true")) {
      keyPairFile = new File("~/.ssh/id_rsa.pub");
      LOG.info("Uploading local public key from ~/.ssh/id_rsa.pub to Rackspace...");
    } else {
      //obtain existing key from Rackspace
      if (keyPairApi.get("goraci-keypair") != null) {
        publicKey = keyPairApi.get("goraci-keypair").getPublicKey();
        LOG.info("Obtained existing public key 'goraci-keypair' from Rackspace...");
      } else {
        //have Rackspace generate us a key
        LOG.info("Generating local keypair 'goraci-keypair' and uploading to Rackspace...");
        KeyPair keyPair = keyPairApi.create("goraci-keypair");
        keyPairFile = new File("/target/goraci-keypair.pem");
        try {
          Files.write(keyPair.getPrivateKey(), keyPairFile, Charsets.UTF_8);
        } catch (IOException e) {
          throw new IOException(e);
        }
        try {
          publicKey = Files.toString(keyPairFile, Charsets.UTF_8);
        } catch (IOException e) {
          throw new IOException(e);
        }
        keyPairApi.createWithPublicKey("goraci-keypair", publicKey);
      }
    }
    
    ServerApi serverApi = novaApi.getServerApiForZone(rs_region);
    CreateServerOptions options = CreateServerOptions.Builder.keyPairName("goraci-keypair");
    ServerCreated serverCreated = null;
    for (int i = 0; i < num_servers; i++) {
      if (serverName != null) {
        serverCreated = serverApi.create(serverName + num_servers, rs_imageId, rs_flavourId, options);

      } else {
        serverCreated = serverApi.create("goraci_test_server" + num_servers, image.getId(), flavor.getId(), options);
      }
      ServerPredicates.awaitActive(serverApi).apply(serverCreated.getId());
      LOG.info("Provisioned node " + i + 1 + " of " + num_servers + " with name: " + serverName + i);
    }
  }

}
