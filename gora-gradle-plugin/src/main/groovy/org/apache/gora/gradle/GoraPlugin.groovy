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
package org.apache.gora.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet

class GoraPlugin implements Plugin<Project> {
    private static final String GENERATE_GROUP = 'generate'

    @Override
    void apply(final Project project) {
        project.apply plugin: 'java'
        project.sourceSets.all { SourceSet sourceSet ->
            setupGoraFor(sourceSet, project)
        }
    }

    private setupGoraFor(SourceSet sourceSet, Project project) {
        def configName = (sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "gora" : sourceSet.getName() + "Gora")
        project.configurations.create(configName) {
            visible = false
            transitive = false
            description = "The Gora libraries to be used for this project."
            extendsFrom = []
        }

        // Wire task
        Task goraTask = createGoraTaskFor(sourceSet, project)

        // Generate source code before java compile
        String compileJavaTaskName = sourceSet.getCompileTaskName("java");
        Task compileJavaTask = project.tasks.getByName(compileJavaTaskName);
        compileJavaTask.dependsOn(goraTask)
    }

    private Task createGoraTaskFor(SourceSet sourceSet, Project project) {
        def taskName = taskName(sourceSet)
        def goraTask = project.tasks.create(taskName, GoraCompileTask)
        goraTask.group = GENERATE_GROUP
        goraTask.description = "Generates code from the ${sourceSet.name} Gora schemas."
        goraTask.source = 'src/main/resources/'
        goraTask.destinationDir = project.file('src/main/java')

        goraTask
    }

    private String taskName(SourceSet sourceSet) {
        return sourceSet.getTaskName('generate', 'Gora')
    }
}
