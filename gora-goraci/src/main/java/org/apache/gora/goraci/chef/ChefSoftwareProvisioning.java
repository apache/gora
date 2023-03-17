/*
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
package org.apache.gora.goraci.chef;

import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.concat;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.gora.goraci.rackspace.RackspaceOrchestration;
import org.apache.gora.memory.store.MemStore;
import org.apache.gora.store.DataStoreFactory;
import org.jclouds.ContextBuilder;
import org.jclouds.chef.ChefApi;
import org.jclouds.chef.ChefApiMetadata;
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
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.inject.Module;
import java.lang.reflect.InvocationTargetException;

/**
 *This class contains all of the Chef software provisioning code required to
 *provision nodes for use within GoraCI.
 *For configuration options required you should see the chef properties within
 *<code>gora.properties</code>.
 */
public class ChefSoftwareProvisioning {
  
  private static String CHEF_CLIENT = "chef.client";
  
  private static String CHEF_ORGANIZATION = "chef.organization";
  
  private static String RS_CONTINENT = "rackspace.continent";

  private static String RS_USERNAME = "rackspace.username";

  private static String RS_APIKEY = "rackspace.apikey";

  private static String RS_REGION = "rackspace.region";
  
  private static final Logger LOG = LoggerFactory.getLogger(RackspaceOrchestration.class);

  /**
   * Default constructor
   */
  public ChefSoftwareProvisioning() {
  }
  
  private static void performChefComputeServiceBootstrapping(Properties properties) throws 
          IOException, InstantiationException, IllegalAccessException, 
          NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    // Get the credentials that will be used to authenticate to the Chef server
    String rsContinent = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), 
        RS_CONTINENT, "rackspace-cloudservers-us");
    String rsUser = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_USERNAME, "asf-gora");
    String rsApiKey = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_APIKEY, null);
    String rsRegion = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), RS_REGION, "DFW");
    String client = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), CHEF_CLIENT, System.getProperty("user.name"));
    String organization = DataStoreFactory.findProperty(properties, MemStore.class.getDeclaredConstructor().newInstance(), CHEF_ORGANIZATION, null);
    String pemFile = System.getProperty("user.home") + "/.chef/" + client + ".pem";
    String credential = Files.toString(new File(pemFile), Charsets.UTF_8);

    // Provide the validator information to let the nodes to auto-register themselves
    // in the Chef server during bootstrap
    String validator = organization + "-validator";
    String validatorPemFile = System.getProperty("user.home") + "/.chef/" + validator + ".pem";
    String validatorCredential = Files.toString(new File(validatorPemFile), Charsets.UTF_8);

    Properties chefConfig = new Properties();
    chefConfig.put(ChefProperties.CHEF_VALIDATOR_NAME, validator);
    chefConfig.put(ChefProperties.CHEF_VALIDATOR_CREDENTIAL, validatorCredential);

    // Create the connection to the Chef server
    ChefApi chefApi = ContextBuilder.newBuilder(new ChefApiMetadata())
        .endpoint("https://api.opscode.com/organizations/" + organization)
        .credentials(client, credential)
        .overrides(chefConfig)
        .buildApi(ChefApi.class);

    // Create the connection to the compute provider. Note that ssh will be used to bootstrap chef
    ComputeServiceContext computeContext = ContextBuilder.newBuilder(rsContinent)
        .endpoint(rsRegion)
        .credentials(rsUser, rsApiKey)
        .modules(ImmutableSet.<Module> of(new SshjSshClientModule()))
        .buildView(ComputeServiceContext.class);

    // Group all nodes in both Chef and the compute provider by this group
    String group = "jclouds-chef-goraci";

    // Set the recipe to install and the configuration values to override
    String recipe = "apache2";
    JsonBall attributes = new JsonBall("{\"apache\": {\"listen_ports\": \"8080\"}}");

    // Check to see if the recipe you want exists
    List<String> runlist = null;
    Iterable< ? extends CookbookVersion> cookbookVersions =
        chefApi.chefService().listCookbookVersions();
    if (any(cookbookVersions, CookbookVersionPredicates.containsRecipe(recipe))) {
      runlist = new RunListBuilder().addRecipe(recipe).build();
    }
    for (Iterator<String> iterator = runlist.iterator(); iterator.hasNext();) {
      String string = (String) iterator.next();
      LOG.info(string);
    }

    // Update the chef service with the run list you wish to apply to all nodes in the group
    // and also provide the json configuration used to customize the desired values
    BootstrapConfig config = BootstrapConfig.builder().runList(runlist).attributes(attributes).build();
    chefApi.chefService().updateBootstrapConfigForGroup(group, config);

    // Build the script that will bootstrap the node
    Statement bootstrap = chefApi.chefService().createBootstrapScriptForGroup(group);

    TemplateBuilder templateBuilder = computeContext.getComputeService().templateBuilder();
    templateBuilder.options(runScript(bootstrap));
    // Run a node on the compute provider that bootstraps chef
    try {
      Set< ? extends NodeMetadata> nodes =
          computeContext.getComputeService().createNodesInGroup(group, 1, templateBuilder.build());
      for (NodeMetadata nodeMetadata : nodes) {
        LOG.info("<< node %s: %s%n", nodeMetadata.getId(),
            concat(nodeMetadata.getPrivateAddresses(), nodeMetadata.getPublicAddresses()));
      }
    } catch (RunNodesException e) {
      throw new RuntimeException(e.getMessage());
    }

    // Release resources
    chefApi.close();
    computeContext.close();

  }

  /**
   * @param args
   * @throws IOException 
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    Properties props = DataStoreFactory.createProps();
    ChefSoftwareProvisioning.performChefComputeServiceBootstrapping(props);
  }

}
