/*
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

package org.apache.gora.hive.util;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.Service;
import org.apache.hive.service.cli.CLIService;
import org.apache.hive.service.cli.SessionHandle;
import org.apache.hive.service.server.HiveServer2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hive test server implementation
 */
public class HiveTestServer {

  private static final Logger log = LoggerFactory.getLogger((MethodHandles.lookup().lookupClass()));
  private HiveServer2 hiveServer;

  /**
   * Initiate a hive server instance
   *
   * @throws Exception throws if server initiation is failed
   */
  public void start() throws Exception {
    log.info("Starting Hive Test Server...");
    if (hiveServer == null) {
      hiveServer = new HiveServer2();
      hiveServer.init(new HiveConf());
      hiveServer.start();
      waitForStartup();
      log.info("Hive Test Server Started");
    }
  }

  /**
   * Waiting for maximum of one minute to start the server.
   *
   * @throws Exception throws if server couldn't start within the time limit
   */
  private void waitForStartup() throws Exception {
    long timeout = TimeUnit.MINUTES.toMillis(1);
    long unitOfWait = TimeUnit.SECONDS.toMillis(1) * 5;
    CLIService hs2Client = getServiceClientInternal();
    SessionHandle sessionHandle = null;
    for (int interval = 0; interval < timeout / unitOfWait; interval++) {
      Thread.sleep(unitOfWait);
      try {
        Map<String, String> sessionConf = new HashMap<>();
        sessionHandle = hs2Client.openSession("hive", "", sessionConf);
        return;
      } catch (Exception e) {
        //server hasn't started yet
      } finally {
        hs2Client.closeSession(sessionHandle);
      }
    }
    throw new TimeoutException("Hive test server starting timeout");
  }

  /**
   * Get the client service from the hive server instance.
   *
   * @return CLIService client service initiated in the hive server instance
   */
  private CLIService getServiceClientInternal() {
    for (Service service : hiveServer.getServices()) {
      if (service instanceof CLIService) {
        return (CLIService) service;
      }
    }
    throw new IllegalStateException("Cannot find CLIService");
  }

  /**
   * Stop hive test server
   */
  public void stop() {
    if (hiveServer != null) {
      log.info("Stopping Hive Test Server...");
      hiveServer.stop();
      hiveServer = null;
      log.info("Hive Test Server Stopped SucessFully");
    }
  }
}
