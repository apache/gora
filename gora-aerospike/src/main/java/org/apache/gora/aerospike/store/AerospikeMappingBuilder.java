/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.aerospike.store;

import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class AerospikeMappingBuilder {

  public static final Logger LOG = LoggerFactory.getLogger(AerospikeMappingBuilder.class);

  private AerospikeMapping aerospikeMapping;

  public AerospikeMappingBuilder(String mappingFile, Class<?> keyClass, Class<?> persistentClass) throws IOException {
    this.aerospikeMapping = new AerospikeMapping();
    this.readMappingFile(mappingFile, keyClass, persistentClass);
  }

  public AerospikeMapping getAerospikeMapping() {
    return this.aerospikeMapping;
  }

  private void readMappingFile(String fileName, Class<?> keyClass, Class<?> persistentClass) throws IOException {
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
      if (inputStream == null) {
        LOG.warn("Mapping file '" + fileName + "' could not be found!");
        throw new IOException("Mapping file '" + fileName + "' could not be found!");
      }
      Document document = saxBuilder.build(inputStream);
      if (document == null) {
        LOG.warn("Mapping file '" + fileName + "' could not be found!");
        throw new IOException("Mapping file '" + fileName + "' could not be found!");
      }

      Element root = document.getRootElement();

      // Mapping the defined policies
      List<Element> policyElements = root.getChildren("policy");

      for (Element policyElement : policyElements) {

        String policy = policyElement.getAttributeValue("name");

        if (policy.equals("write")) {

          WritePolicy writePolicy = new WritePolicy();
          if (policyElement.getAttributeValue("gen") != null)
            writePolicy.generationPolicy = getGenerationPolicyMapping(policyElement.getAttributeValue
              ("gen").toUpperCase(Locale.getDefault()));
          if (policyElement.getAttributeValue("exists") != null)
            writePolicy.recordExistsAction = getRecordExistsAction(policyElement.getAttributeValue
              ("exists").toUpperCase(Locale.getDefault()));
          if (policyElement.getAttributeValue("key") != null)
            writePolicy.sendKey = getKeyUsagePolicy(policyElement.getAttributeValue("key").toUpperCase
              (Locale.getDefault()));
          if (policyElement.getAttributeValue("retry") != null)
            writePolicy.retryOnTimeout = getRetryOnTimeoutPolicy(policyElement.getAttributeValue
              ("retry").toUpperCase(Locale.getDefault()));
          if (policyElement.getAttributeValue("timeout") != null)
            writePolicy.timeout = getTimeoutValue(policyElement.getAttributeValue("timeout"));
          aerospikeMapping.setWritePolicy(writePolicy);
        } else if (policy.equals("read")) {

          Policy readPolicy = new Policy();
          if (policyElement.getAttributeValue("key") != null)
            readPolicy.sendKey = getKeyUsagePolicy(policyElement.getAttributeValue("key").toUpperCase(Locale
              .getDefault()));
          if (policyElement.getAttributeValue("timeout") != null)
            readPolicy.timeout = getTimeoutValue(policyElement.getAttributeValue("timeout"));
          aerospikeMapping.setReadPolicy(readPolicy);
        }
      }

      // Mapping the defined classes
      List<Element> classElements = root.getChildren("policy");

      boolean persistentAndKeyClassMatches = false;
      for (Element classElement : classElements) {
        if (classElement.getAttributeValue("keyClass").equals(keyClass.getCanonicalName())
          && classElement.getAttributeValue("name").equals(persistentClass.getCanonicalName())) {
          persistentAndKeyClassMatches = true;

          String nameSpace = classElement.getAttributeValue("namespace");
          if (nameSpace == null || nameSpace.isEmpty()) {
            throw new ConfigurationException("Gora-aerospike-mapping does not include the relevant namespace for the " +
              "class");
          }
          aerospikeMapping.setNamespace(nameSpace);

          String set = classElement.getAttributeValue("set");
          if (set != null && !set.isEmpty()) {
            //ToDo : check for schema set name
            aerospikeMapping.setSet(set);
          }
        }
      }
      if (!persistentAndKeyClassMatches)
        throw new ConfigurationException("Gora-aerospike-mapping does not include the name and keyClass in the " +
          "databean");
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      throw new IOException(e);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new IOException(e);
    }
  }

  private GenerationPolicy getGenerationPolicyMapping(String genPolicy) {

    if (genPolicy == null)
      return GenerationPolicy.NONE;

    GenerationPolicy generationPolicy;
    switch (genPolicy) {
      case "IGNORE":
        generationPolicy = GenerationPolicy.NONE;
        break;
      case "EQ":
        generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;
        break;
      case "GT":
        generationPolicy = GenerationPolicy.EXPECT_GEN_GT;
        break;
      default: {
        LOG.warn("Invalid generation policy provided, using the default generation policy");
        generationPolicy = GenerationPolicy.NONE;
      }
    }
    return generationPolicy;
  }

  private RecordExistsAction getRecordExistsAction(String existsPolicy) {
    if (existsPolicy == null)
      return RecordExistsAction.UPDATE;

    RecordExistsAction recordExistsAction;
    switch (existsPolicy) {
      case "UPDATE":
        recordExistsAction = RecordExistsAction.UPDATE;
        break;
      case "UPDATE_ONLY":
        recordExistsAction = RecordExistsAction.UPDATE_ONLY;
        break;
      case "REPLACE":
        recordExistsAction = RecordExistsAction.REPLACE;
        break;
      case "REPLACE_ONLY":
        recordExistsAction = RecordExistsAction.REPLACE_ONLY;
        break;
      case "CREATE_ONLY":
        recordExistsAction = RecordExistsAction.CREATE_ONLY;
        break;
      default: {
        LOG.warn("Invalid record exists action provided, using the default record exists action");
        recordExistsAction = RecordExistsAction.UPDATE;
      }
    }
    return recordExistsAction;
  }

  private boolean getKeyUsagePolicy(String keyPolicy) {

    if (keyPolicy == null)
      return false;

    boolean sendKey;
    switch (keyPolicy) {
      case "DIGEST":
        sendKey = false;
        break;
      case "SEND":
        sendKey = true;
        break;
      default: {
        LOG.warn("Invalid key action policy provided, using the default key action policy");
        sendKey = false;
      }
    }
    return sendKey;
  }

  private boolean getRetryOnTimeoutPolicy(String retry) {

    if (retry == null)
      return false;

    boolean retryOnTimeout;
    switch (retry) {
      case "NONE":
        retryOnTimeout = false;
        break;
      case "ONCE":
        retryOnTimeout = true;
        break;
      default: {
        LOG.warn("Invalid key retry policy provided, using the default retry policy");
        retryOnTimeout = false;
      }
    }
    return retryOnTimeout;
  }

  private int getTimeoutValue(String timeout) {

    if (timeout == null)
      return 0;
    int timeoutInt = 0;
    try {
      timeoutInt = Integer.valueOf(timeout);
    } catch (NumberFormatException e) {
      LOG.warn("Invalid timeout value provided, using the default timeout value");
    }
    return timeoutInt;
  }

}
