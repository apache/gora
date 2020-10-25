:: Licensed to the Apache Software Foundation (ASF) under one or more
:: contributor license agreements.  See the NOTICE file distributed with
:: this work for additional information regarding copyright ownership.
:: The ASF licenses this file to You under the Apache License, Version 2.0
:: (the "License"); you may not use this file except in compliance with
:: the License.  You may obtain a copy of the License at
:: 
::     http://www.apache.org/licenses/LICENSE-2.0
:: 
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.

::
:: The script to run Java components.
::
:: Environment Variables
::
::   GORA_HEAPSIZE  The maximum amount of heap to use, in MB. 
::                   Default is 1024.
::
::   GORA_OPTS      Extra Java runtime option.
::

:: resolve links - %0 may be a softlink
@echo off
setlocal enabledelayedexpansion
set THIS=%~f0

::if no args specified, show usage
if "%~1" == "" (GOTO ECHOOPTIONS) else (GOTO HAVEARGUMENTS)

:ECHOOPTIONS
@echo Usage: run COMMAND [COMMAND options]
@echo where COMMAND is one of:
@echo   goracompiler               Run Compiler
@echo   specificcompiler           Run Avro Specific Compiler
@echo   cassandranativecompiler    Run Gora Cassandra Native Compiler
@echo   dynamocompiler             Run Gora DynamoDB Compiler
@echo   goracirackspace            Run the GoraCI Rackspace orchestration setup
@echo   goracichef                 Run the GoraCI Chef software provisioning setup
@echo   logmanager                 Run the tutorial log manager
@echo   distributedlogmanager      Run the tutorial distributed log manager
@echo   loganalytics               Run the tutorial log analytics
@echo   loganalyticsspark          Run the tutorial log analytics spark
@echo   junit                      Run the given JUnit test
@echo   version                    Print Gora version to terminal
@echo  or
@echo  MODULE CLASSNAME   run the class named CLASSNAME in module MODULE
@echo Most commands print help when invoked w/o parameters.
exit /B 1

:HAVEARGUMENTS
set COMMAND=%1
shift

for %%F in (%THIS%) do set THIS_DIR=%%~dpF
for %%F IN ("%THIS_DIR:~0,-1%") DO set GORA_HOME=%%~dpF
if exist "%GORA_HOME%conf\gora-env.sh" CALL "%GORA_HOME%conf\gora-env.sh"
  
if not ["%JAVA_HOME%"] == [""] GOTO JAVAHOMESET 
@echo Error: JAVA_HOME is not set.
GOTO :EOF

:JAVAHOMESET
set JAVA="%JAVA_HOME%\bin\java"
set JAVA_HEAP_MAX=-Xmx1024m

:: check envvars which might override default args
if not [%GORA_HEAPSIZE%]==[] set JAVA_HEAP_MAX=-Xmx%GORA_HEAPSIZE%m

:: classpathtemp initially contains $GORA_CONF_DIR, or defaults to $GORA_HOME/conf
set classpathtemp=
if "%GORA_CONF_DIR%"=="" set GORA_CONF_DIR=%GORA_HOME%conf
echo %%classpathtemp|find "%GORA_CONF_DIR%" >nul
if errorlevel 1 set classpathtemp=%classpathtemp%;%GORA_CONF_DIR%
set classpathtemp=%classpathtemp%;%JAVA_HOME%\lib\tools.jar

:: default log directory & file
if [%GORA_LOG_DIR%] == [] set GORA_LOG_DIR="%GORA_HOME%logs"
if [%GORA_LOGFILE%] == [] set GORA_LOGFILE='gora.log'

if not [%JAVA_LIBRARY_PATH%]==[] set JAVA_OPTS=%JAVA_OPTS% -Djava.library.path=%JAVA_LIBRARY_PATH%
::GORA_OPTS="$GORA_OPTS -Dhadoop.log.dir=$GORA_LOG_DIR"
::GORA_OPTS="$GORA_OPTS -Dhadoop.log.file=$GORA_LOGFILE"

:: figure out which class to run
set "defcase=0"
set arg1=%1
shift

set args=
:args_loop
if "%1"=="" GOTO after_args_loop
set args=%args%%1 
shift
GOTO args_loop

:after_args_loop
findstr /ri ":CASE_%COMMAND%" %THIS% >nul 2>nul
if errorlevel 1 (goto DEFAULT_CASE) else (goto CASE_%COMMAND%)
:CASE_goracompiler
  set MODULE=gora-compiler-cli
  set CLASS=org.apache.gora.compiler.cli.GoraCompilerCLI
  GOTO ENDSWITCH 
:CASE_specificcompiler
  set MODULE=gora-core
  set CLASS=org.apache.avro.specific.SpecificCompiler
  GOTO ENDSWITCH
:CASE_cassandranativecompiler
  set MODULE=gora-cassandra-cql
  set CLASS=org.apache.gora.cassandra.compiler.GoraCassandraNativeCompiler
  GOTO ENDSWITCH
:CASE_dynamocompiler
  set MODULE=gora-dynamodb
  set CLASS=org.apache.gora.dynamodb.compiler.GoraDynamoDBCompiler
  GOTO ENDSWITCH
:CASE_goracirackspace
  set MODULE=gora-goraci
  set CLASS=org.apache.gora.goraci.rackspace.RackspaceOrchestration
  GOTO ENDSWITCH
:CASE_goracichef
  set MODULE=gora-goraci
  set CLASS=org.apache.gora.goraci.chef.ChefSoftwareProvisioning
  GOTO ENDSWITCH
:CASE_logmanager
  set MODULE=gora-tutorial
  set CLASS=org.apache.gora.tutorial.log.LogManager
  GOTO ENDSWITCH
:CASE_distributedlogmanager
  set MODULE=gora-tutorial
  set CLASS=org.apache.gora.tutorial.log.DistributedLogManager
  GOTO ENDSWITCH
:CASE_loganalytics
  set MODULE=gora-tutorial
  set CLASS=org.apache.gora.tutorial.log.LogAnalytics
  GOTO ENDSWITCH
:CASE_loganalyticsspark
  set MODULE=gora-tutorial
  set CLASS=org.apache.gora.tutorial.log.LogAnalyticsSpark
  GOTO ENDSWITCH
:CASE_junit
  set MODULE=*
  set CLASS=junit.textui.TestRunner
  GOTO ENDSWITCH
:CASE_version
  set MODULE=gora-core
  set CLASS=org.apache.gora.util.VersionInfo
  GOTO ENDSWITCH  
:DEFAULT_CASE
  set MODULE=%COMMAND%
  set CLASS=%arg1%
  set "defcase=1"
  GOTO ENDSWITCH

:ENDSWITCH
:: add libs to classpathtemp
for /f "delims=" %%f in ('dir /b %GORA_HOME%%MODULE%\lib\*.jar') do set classpathtemp=!classpathtemp!;!GORA_HOME!!MODULE!\lib\%%f
for /f "delims=" %%f in ('dir /b %GORA_HOME%%MODULE%\target\*.jar') do set classpathtemp=!classpathtemp!;!GORA_HOME!!MODULE!\target\%%f

set classpathtemp=%classpathtemp%;%GORA_HOME%%MODULE%\target\classes\
set classpathtemp=%classpathtemp%;%GORA_HOME%%MODULE%\target\test-classes\
set classpathtemp=%classpathtemp%;%GORA_HOME%%MODULE%\conf

if %defcase%==0 set args=%arg1% %args%
:: run it
if "%JAVA_OPTS%"=="" (GOTO CALLWITHOUTOPTS) else (GOTO CALLWITHOPTS)

:CALLWITHOUTOPTS
%JAVA% %JAVA_HEAP_MAX% -cp "%classpathtemp%" %CLASS% %args%
GOTO :EOF

:CALLWITHOPTS
%JAVA% %JAVA_HEAP_MAX% %JAVA_OPTS% -cp "%classpathtemp%" %CLASS% %args%

