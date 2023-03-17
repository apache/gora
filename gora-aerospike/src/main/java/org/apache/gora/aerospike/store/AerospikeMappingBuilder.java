/*
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
import com.aerospike.client.policy.ReadModeAP;
import com.aerospike.client.policy.ReadModeSC;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom.JDOMException;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Class to represent the Aerospike mapping builder
 */
public class AerospikeMappingBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(AerospikeMappingBuilder.class);

  private final AerospikeMapping aerospikeMapping;

  public AerospikeMappingBuilder() {
    this.aerospikeMapping = new AerospikeMapping();
  }

  public AerospikeMapping getAerospikeMapping() {
    return this.aerospikeMapping;
  }

  /**
   * Reads the gora aerospike mapping file
   *
   * @param mappingFile     mapping file path
   * @param keyClass        key class
   * @param persistentClass persistent class
   */
  public void readMappingFile(String mappingFile, Class<?> keyClass, Class<?> persistentClass) {

    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(mappingFile);
      if (inputStream == null) {
        LOG.error("Mapping file '{}' could not be found!", mappingFile);
        throw new IOException("Mapping file '" + mappingFile + "' could not be found!");
      }
      Document document = saxBuilder.build(inputStream);
      if (document == null) {
        LOG.error("Mapping file '{}' could not be found!", mappingFile);
        throw new IOException("Mapping file '" + mappingFile + "' could not be found!");
      }

      Element root = document.getRootElement();

      List<Element> policyElements = root.getChildren("policy");

      for (Element policyElement : policyElements) {

        String policy = policyElement.getAttributeValue("name");
        if (policy != null) {

          // Write Policies
          if (policy.equals(AerospikePolicyConst.WRITE_POLICY_NAME)) {
            WritePolicy writePolicy = new WritePolicy();
            if (policyElement.getAttributeValue(AerospikePolicyConst.GENERATION_POLICY_NAME)
                    != null) {
              writePolicy.generationPolicy = getGenerationPolicyMapping(
                      policyElement.getAttributeValue(AerospikePolicyConst.GENERATION_POLICY_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.RECORD_EXISTS_ACTION_NAME)
                    != null) {
              writePolicy.recordExistsAction = getRecordExistsAction(policyElement
                      .getAttributeValue(AerospikePolicyConst.RECORD_EXISTS_ACTION_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.COMMIT_LEVEL_NAME) != null) {
              writePolicy.commitLevel = getCommitLevel(
                      policyElement.getAttributeValue(AerospikePolicyConst.COMMIT_LEVEL_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.DURABLE_DELETE_NAME) != null) {
              writePolicy.durableDelete = isDurableDelete(
                      policyElement.getAttributeValue(AerospikePolicyConst.DURABLE_DELETE_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.EXPIRATION_NAME) != null) {
              writePolicy.expiration = getTimeDuration(
                      policyElement.getAttributeValue(AerospikePolicyConst.EXPIRATION_NAME));
            }
            aerospikeMapping.setWritePolicy(writePolicy);
          }

          // Read Policies
          else if (policy.equals(AerospikePolicyConst.READ_POLICY_NAME)) {

            Policy readPolicy = new Policy();
            if (policyElement.getAttributeValue(AerospikePolicyConst.READ_MODE_AP_NAME) != null) {
              readPolicy.readModeAP = getReadModeAP(policyElement
                      .getAttributeValue(AerospikePolicyConst.READ_MODE_AP_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.READ_MODE_SC_NAME) != null) {
              readPolicy.readModeSC = getReadModeSC(policyElement
                      .getAttributeValue(AerospikePolicyConst.READ_MODE_SC_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.REPLICA_POLICY_NAME) != null) {
              readPolicy.replica = getReplicaPolicy(
                      policyElement.getAttributeValue(AerospikePolicyConst.REPLICA_POLICY_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.SOCKET_TIMEOUT_NAME) != null) {
              readPolicy.socketTimeout = getTimeDuration(
                      policyElement.getAttributeValue(AerospikePolicyConst.SOCKET_TIMEOUT_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.TOTAL_TIMEOUT_NAME) != null) {
              readPolicy.totalTimeout = getTimeDuration(
                      policyElement.getAttributeValue(AerospikePolicyConst.TOTAL_TIMEOUT_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.TIMEOUT_DELAY_NAME) != null) {
              readPolicy.timeoutDelay = getTimeDuration(
                      policyElement.getAttributeValue(AerospikePolicyConst.TIMEOUT_DELAY_NAME));
            }
            if (policyElement.getAttributeValue(AerospikePolicyConst.MAX_RETRIES_NAME) != null) {
              readPolicy.maxRetries = getMaxRetriesValue(
                      policyElement.getAttributeValue(AerospikePolicyConst.MAX_RETRIES_NAME));
            }
            aerospikeMapping.setReadPolicy(readPolicy);
          }
        }
      }

      List<Element> classElements = root.getChildren("class");

      boolean persistentClassAndKeyClassMatches = false;
      for (Element classElement : classElements) {

        String mappingKeyClass = classElement.getAttributeValue("keyClass");
        String mappingClassName = classElement.getAttributeValue("name");

        if (mappingKeyClass != null && mappingClassName != null && mappingKeyClass
                .equals(keyClass.getCanonicalName()) && mappingClassName
                .equals(persistentClass.getCanonicalName())) {

          persistentClassAndKeyClassMatches = true;

          List<Element> fields = classElement.getChildren("field");
          Map<String, String> binMapping = new HashMap<>();
          for (Element field : fields) {
            String fieldName = field.getAttributeValue("name");
            String binName = field.getAttributeValue("bin");
            if (fieldName != null && binName != null) {
              binMapping.put(fieldName, binName);
            }
          }
          aerospikeMapping.setBinMapping(binMapping);

          String nameSpace = classElement.getAttributeValue("namespace");
          if (nameSpace == null || nameSpace.isEmpty()) {
            LOG.error("Gora-aerospike-mapping does not include the relevant namespace for the "
                    + "{} class", mappingClassName);
            throw new ConfigurationException(
                    "Gora-aerospike-mapping does not include the relevant namespace for the "
                            + mappingClassName + "class");
          }
          aerospikeMapping.setNamespace(nameSpace);

          String set = classElement.getAttributeValue("set");
          if (set != null && !set.isEmpty()) {
            aerospikeMapping.setSet(set);
          }
        }
      }
      if (!persistentClassAndKeyClassMatches) {
        LOG.error("Gora-aerospike-mapping does not include the name and keyClass specified in the "
                + "databean");
        throw new ConfigurationException(
                "Gora-aerospike-mapping does not include the name and keyClass specified in the "
                        + "databean");
      }
    } catch (IOException | JDOMException | ConfigurationException e) {
      throw new RuntimeException(e);
    }
    LOG.info("Gora Aerospike mapping file is read successfully.");
  }

  /**
   * Returns the corresponding generation policy from the user specified generation policy name
   *
   * @param genPolicy generation policy name
   * @return corresponding generation policy
   */
  private GenerationPolicy getGenerationPolicyMapping(String genPolicy) {

    if (genPolicy == null)
      return GenerationPolicy.NONE;

    for (GenerationPolicy generationPolicyEnum : GenerationPolicy.values()) {
      if (genPolicy.equalsIgnoreCase(generationPolicyEnum.toString())) {
        return generationPolicyEnum;
      }
    }
    LOG.warn("Invalid generation policy provided, using the default generation policy.");
    return GenerationPolicy.NONE;
  }

  /**
   * Returns the corresponding record exist action from the user specified exists policy name.
   * The default value is UPDATE
   *
   * @param existsPolicy exists policy name
   * @return corresponding record exist action
   */
  private RecordExistsAction getRecordExistsAction(String existsPolicy) {
    if (existsPolicy == null)
      return RecordExistsAction.UPDATE;

    for (RecordExistsAction recordExistsActionEnum : RecordExistsAction.values()) {
      if (existsPolicy.equalsIgnoreCase(recordExistsActionEnum.toString())) {
        return recordExistsActionEnum;
      }
    }
    LOG.warn("Invalid record exists action provided, using the default record exists action.");
    return RecordExistsAction.UPDATE;
  }

  /**
   * Returns the corresponding commit level from the user specified commit level name.
   * The default value is COMMIT_ALL
   *
   * @param commitLevel user specified commit level name
   * @return corresponding commit level
   */
  private CommitLevel getCommitLevel(String commitLevel) {
    if (commitLevel == null)
      return CommitLevel.COMMIT_ALL;

    for (CommitLevel commitLevelEnum : CommitLevel.values()) {
      if (commitLevel.equalsIgnoreCase(commitLevelEnum.toString())) {
        return commitLevelEnum;
      }
    }
    LOG.warn("Invalid commit level provided, using the default commit level.");
    return CommitLevel.COMMIT_ALL;
  }

  /**
   * Returns the corresponding durable delete boolean from the user specified durable delete value.
   * The default value is FALSE
   *
   * @param durableDelete user specified durable delete value
   * @return corresponding durable delete boolean value
   */
  private boolean isDurableDelete(String durableDelete) {
    if (durableDelete == null)
      return false;

    if (durableDelete.equalsIgnoreCase("false")) {
      return false;
    }
    if (durableDelete.equalsIgnoreCase("true")) {
      return true;
    }
    LOG.warn("Invalid durable delete value provided, using the default durable delete value.");
    return false;
  }

  private ReadModeAP getReadModeAP(String readModeAP) {
    if (readModeAP == null)
      return ReadModeAP.ONE;

    for (ReadModeAP readModeAPEnum : ReadModeAP.values()) {
      if (readModeAP.equalsIgnoreCase(readModeAPEnum.toString())) {
        return readModeAPEnum;
      }
    }
    LOG.warn("Invalid consistency level provided, using the default ReadModeAP level.");
    return ReadModeAP.ONE;
  }

  private ReadModeSC getReadModeSC(String readModeSC) {
    if (readModeSC == null)
      return ReadModeSC.SESSION;

    for (ReadModeSC readModeSCEnum : ReadModeSC.values()) {
      if (readModeSC.equalsIgnoreCase(readModeSCEnum.toString())) {
        return readModeSCEnum;
      }
    }
    LOG.warn("Invalid consistency level provided, using the default ReadModeSC level.");
    return ReadModeSC.SESSION;
  }

  /**
   * Returns the corresponding replica policy from the user specified replica policy name.
   * The default value is SEQUENCE
   *
   * @param replica user specified replica policy name
   * @return corresponding replica policy
   */
  private Replica getReplicaPolicy(String replica) {
    if (replica == null)
      return Replica.SEQUENCE;

    for (Replica replicaEnum : Replica.values()) {
      if (replica.equalsIgnoreCase(replicaEnum.toString())) {
        return replicaEnum;
      }
    }
    LOG.warn("Invalid replica policy provided, using the default replica policy.");
    return Replica.SEQUENCE;
  }

  /**
   * Returns the corresponding timeDuration value from the user specified timeDuration value.
   * The default value is 0
   *
   * @param timeDuration user specified timeDuration value
   * @return corresponding timeDuration value
   */
  private int getTimeDuration(String timeDuration) {

    if (timeDuration == null) {
      return 0;
    }
    int timeDurationInt = 0;
    try {
      timeDurationInt = Integer.valueOf(timeDuration);
    } catch (NumberFormatException e) {
      LOG.warn("Invalid time duration value provided, using the default time duration value");
    }
    return timeDurationInt;
  }

  /**
   * Returns the maximum retires value from the user specified maximum retires value.
   * The default value is 2
   *
   * @param retiesCount user specified retries count
   * @return corresponding maximum retry value
   */
  private int getMaxRetriesValue(String retiesCount) {

    // Default value
    int maxRetriesInt = 2;
    if (retiesCount == null) {
      return maxRetriesInt;
    }
    try {
      maxRetriesInt = Integer.valueOf(retiesCount);
    } catch (NumberFormatException e) {
      LOG.warn("Invalid timeout value provided, using the default timeout value");
    }
    return maxRetriesInt;
  }
}
