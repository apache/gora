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
package org.apache.gora.maven.plugin;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal which compiles Gora schemas.
 *
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractGoraMojo {
  /**
   * The source directory of Apache Avro files. This directory is added to the
   * classpath at schema compiling time. All files can therefore be referenced
   * as classpath resources following the directory structure under the source
   * directory.
   *
   */
  @Parameter(defaultValue = "${basedir}/src/main/avro")
  private File sourceDirectory;

  @Parameter(defaultValue = "${basedir}/src/main/java")
  private File outputDirectory;

  @Override
  protected File getSourcesDirectory() {
    return sourceDirectory;
  }

  @Override
  protected File getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  protected void registerSourceDirectory(MavenProject project, String path) {
    project.addCompileSourceRoot(path);
  }
}