#!groovy

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
def AGENT_LABEL = env.AGENT_LABEL ?: 'ubuntu'
def JDK_NAME = env.JDK_NAME ?: 'jdk_1.8_latest'
def MVN_NAME = env.MVN_NAME ?: 'maven_3_latest'

// =================================================================
// https://cwiki.apache.org/confluence/display/INFRA/Jenkins
// https://cwiki.apache.org/confluence/display/INFRA/Multibranch+Pipeline+recipes
// =================================================================

// general pipeline documentation: https://jenkins.io/doc/book/pipeline/syntax/
pipeline {

    // https://jenkins.io/doc/book/pipeline/syntax/#agent
    agent {
        node {
            label AGENT_LABEL
        }
    }

    tools {
        maven MVN_NAME
        jdk JDK_NAME
    }

    // https://jenkins.io/doc/book/pipeline/syntax/#options
    options {
        // support ANSI colors in stdout/stderr
        ansiColor 'xterm'
        // only keep the latest 10 builds
        buildDiscarder logRotator(numToKeepStr: '10')
        // cancel build if not complete within two hours of scheduling
        timeout time: 2, unit: 'HOURS'
        disableConcurrentBuilds()
    }
    
    environment {
        LANG = 'C.UTF-8'
        MAVEN_CLI_OPTS = "--batch-mode --errors --fail-at-end --show-version"
    }

    stages {
        stage ('Initialize') {
            steps {
                sh '''
                echo "===== Env Details ====="
                java -version
                mvn -version
                echo "======================="
                '''
            }
        }
        
        stage('Build') {
            steps {
                sh "mvn $MAVEN_CLI_OPTS -DskipTests clean install"
            }

            post {
                success {
                    archiveArtifacts '**/target/*.jar'
                }
            }
        }
        
        stage('Test') {
            steps {
                // maven.test.failure.ignore in order to mark build as UNSTABLE
                // instead of FAILED
                sh "mvn $MAVEN_CLI_OPTS -Dmaven.test.failure.ignore=true verify"
            }

            post {
                always {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }
    }
}
