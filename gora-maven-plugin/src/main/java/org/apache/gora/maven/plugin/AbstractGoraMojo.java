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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.SchemaParseException;
import org.apache.gora.compiler.GoraCompiler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class AbstractGoraMojo extends AbstractMojo {

  /**
   * License to chose for created files.
   *
   * Currently ignored, as schema compiler does not support this parameter
   * yet.
   */
  @Parameter(defaultValue = "ASLv2")
  protected String license;

  /**
   * Patterns for file inclusion
   */
  @Parameter(defaultValue = "*.json")
  protected String[] includes;

  /**
   * Patterns for file exclusion
   */
  @Parameter(defaultValue = ".*")
  protected String[] excludes;

  @Component
  protected MavenProject project;

  @Component
  protected BuildContext context;

  public AbstractGoraMojo() {
    super();
  }

  protected void compile() throws IOException {
    File sourceDirectory = getSourcesDirectory();
    File outputDirectory = getOutputDirectory();

    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    Scanner fileScanner = context.newScanner(sourceDirectory, true);
    fileScanner.setIncludes(includes);
    fileScanner.setExcludes(excludes);
    fileScanner.scan();
    File basedir = fileScanner.getBasedir();

    List<File> changedFiles = new ArrayList<File>();
    for (String fileName : fileScanner.getIncludedFiles()) {
      File file = new File(basedir, fileName);
      changedFiles.add(file);
      context.removeMessages(file);
    }
    if (!changedFiles.isEmpty()) {
      try {
        File[] schemaFile = changedFiles.toArray(new File[changedFiles.size()]);
        GoraCompiler.compileSchema(schemaFile, outputDirectory);
      } catch (SchemaParseException e) {
        if (e.getCause() != null && e.getCause() instanceof JsonParseException) {
          attachErrorMessage((JsonParseException) e.getCause());
        } else {
          throw e;
        }
      }
    }
    context.refresh(outputDirectory);
  }

  private void attachErrorMessage(JsonParseException e) {
    JsonLocation location = e.getLocation();
    File file;
    if (location.getSourceRef() instanceof File) {
      file = (File) location.getSourceRef();
    } else {
      file = null;
    }
    context.addMessage(file, location.getLineNr(), location.getColumnNr(), e.getLocalizedMessage(), BuildContext.SEVERITY_ERROR, e);
  }

  @Override
  public void execute() throws MojoExecutionException {
    try {
      compile();
      registerSourceDirectory(project, getSourcesDirectory().getPath());
      registerSourceDirectory(project, getOutputDirectory().getPath());
    } catch (IOException e) {
      throw new MojoExecutionException(
          "Could not compile schema in " + getSourcesDirectory(), e);
    }
  }

  protected abstract File getSourcesDirectory();

  protected abstract File getOutputDirectory();

  /**
   * Register the given path as a source directory.
   *
   * The directory will be used as input for the compilation step, as well as
   * shown as a source folder in IDEs.
   *
   * @param project
   *            project descriptor
   * @param path
   *            relative path to a source directory
   */
  protected abstract void registerSourceDirectory(MavenProject project, String path);
}