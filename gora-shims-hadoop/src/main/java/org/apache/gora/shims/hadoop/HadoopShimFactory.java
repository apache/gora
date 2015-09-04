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
package org.apache.gora.shims.hadoop;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.util.VersionInfo;

/**
 * Factory to create {@link HadoopShim} objects.
 */
public class HadoopShimFactory {

  /**
   * Hadoop shim version mapping.
   */
  private static final Map<String, String> HADOOP_VERSION_TO_IMPL_MAP = new HashMap<String, String>();

  static {
    HADOOP_VERSION_TO_IMPL_MAP.put("1",
        "org.apache.gora.shims.hadoop1.HadoopShim1");
    HADOOP_VERSION_TO_IMPL_MAP.put("2",
        "org.apache.gora.shims.hadoop2.HadoopShim2");
  }

  // package private
  static class Singleton {
    public static final HadoopShimFactory INSTANCE = new HadoopShimFactory();
  }

  /**
   * Access the {@link HadoopShimFactory} singleton.
   * 
   * @return the shared instance of {@link HadoopShimFactory}.
   */
  public static HadoopShimFactory INSTANCE() {
    return Singleton.INSTANCE;
  }

  /**
   * Get the Hadoop shim for the Hadoop version on the class path. In case it
   * fails to obtain an appropriate shim (i.e. unsupported Hadoop version), it
   * throws a {@link RuntimeException}.
   * 
   * Note that this method is potentially costly.
   * 
   * @return A newly created instance of a {@link HadoopShim}.
   */
  public HadoopShim getHadoopShim() {
    String version = getMajorVersion();
    String className = HADOOP_VERSION_TO_IMPL_MAP.get(version);

    try {
      Class<?> class1 = Class.forName(className);
      return HadoopShim.class.cast(class1.newInstance());
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not load Hadoop shim for version " + version
          + ", className=" + className, e);
    }
  }

  /**
   * Get the Hadoop major version number.
   * 
   * @return The major version number of Hadoop.
   */
  public String getMajorVersion() {
    String vers = VersionInfo.getVersion();

    String[] parts = vers.split("\\.");
    if (parts.length < 2) {
      throw new RuntimeException("Unable to parse Hadoop version: "
          + vers + " (expected X.Y.* format)");
    }
    return parts[0];

  }

  // package private
  HadoopShimFactory() {
  }

}
