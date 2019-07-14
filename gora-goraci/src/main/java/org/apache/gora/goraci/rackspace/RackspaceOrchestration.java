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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.apache.gora.memory.store.MemStore;
import org.apache.gora.store.DataStoreFactory;
import org.jclouds.ContextBuilder;
import org.jclouds.chef.ChefContext;
import org.jclouds.chef.config.ChefProperties;
import org.jclouds.chef.domain.BootstrapConfig;
import org.jclouds.chef.domain.CookbookVersion;
import org.jclouds.chef.predicates.CookbookVersionPredicates;
import org.jclouds.chef.util.RunListBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.domain.JsonBall;
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
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.sshj.config.SshjSshClientModule;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript; 

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.inject.Module;

import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.concat;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This is the main class for initiating Rackspace cloud
 * topography for use within GoraCI jobs. A wealth of settings
 * are configurable from within <code>gora.properties</code>.</p> 
 * <p>For
 * further documentation on the Rackspace Orchestration please see the
 * <a href="http://gora.apache.org/current/index.html#goraci-integration-testsing-suite">
 * current documentation</a>.</p>
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
  
  private static NovaApi novaApi = null;
  
  private static String rsContinent = null;
  
  private static String rsUser = null;
  
  private static String rsApiKey = null;
  
  private static String rsRegion = null;

  /**
   * Right now the Rackspace Orchestration and Services Provisioning
   * requires no arguments. It can be invoked from
   * <code>$GORA_HOME/bin/gora goracisetup</code>
   * @param args
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws NoSuchElementException 
   * @throws java.io.IOException 
   * @throws java.lang.NoSuchMethodException 
   * @throws java.lang.reflect.InvocationTargetException 
   */
  public static void main(String[] args) throws NoSuchElementException, InstantiationException, IllegalAccessException, IOException, NoSuchMethodException, InvocationTargetException {
    Properties properties = DataStoreFactory.createProps();
    performRackspaceOrchestration(properties);
  }
  
  private static void performRackspaceOrchestration(Properties properties) throws InstantiationException, 
          IllegalAccessException, IOException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    rsContinent = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), 
        RS_CONTINENT, "rackspace-cloudservers-us");
    rsUser = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_USERNAME, "asf-gora");
    rsApiKey = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_APIKEY, null);
    rsRegion = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_REGION, "DFW");
    String rsFlavourId = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_FLAVORID, null);
    String rsImageId = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_IMAGEID, null);
    int numServers = Integer.parseInt(DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_NUM_SERVERS, "10"));
    String serverName = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_SERVERNAME, "goraci_test_server");

    novaApi = ContextBuilder.newBuilder(rsContinent).credentials(rsUser, rsApiKey).buildApi(NovaApi.class);
    LOG.info("Defining Rackspace cloudserver continent as: {}, and region: {}.", rsContinent, rsRegion);

    //Choose operating system
    ImageApi imageApi = novaApi.getImageApi(rsRegion);
    Image image = imageApi.get(rsImageId);
    //Choose hardware configuration
    FlavorApi flavorApi = novaApi.getFlavorApi(rsRegion);
    Flavor flavor = flavorApi.get(rsFlavourId);
    LOG.info("Defining Rackspace cloudserver flavors as: {}, with image id's {}", rsFlavourId, rsImageId);

    //Manage keypairs
    KeyPairApi keyPairApi = novaApi.getKeyPairApi(rsRegion).get();

    File keyPairFile = null;
    String publicKey = null;
    //Use your own .pub key which should be on CP
    if (DataStoreFactory.findBooleanProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_PUBKEY, "true")) {
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

    ServerApi serverApi = novaApi.getServerApi(rsRegion);
    CreateServerOptions options = CreateServerOptions.Builder.keyPairName("goraci-keypair");
    ServerCreated serverCreated = null;
    for (int i = 0; i < numServers; i++) {
      if (serverName != null) {
        serverCreated = serverApi.create(serverName + i, rsImageId, rsFlavourId, options);

      } else {
        serverCreated = serverApi.create("goraci_test_server" + i, image.getId(), flavor.getId(), options);
      }
      ServerPredicates.awaitActive(serverApi).apply(serverCreated.getId());
      LOG.info("Provisioned node {} of {} with name {}", new Object[]{i + 1, numServers, serverName + i});
    }

  }
}
