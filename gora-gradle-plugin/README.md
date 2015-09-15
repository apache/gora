gora-gradle-plugin
==================

[Gradle](http://www.gradle.org/) plugin for processing [Avro](http://avro.apache.org/) files for [Gora](http://gora.apache.org)

## Overview
Generate [Apache Gora](http://gora.apache.org) java types from an [Apache Avro](http://avro.apache.org/) descriptor (.avsc files).
This plugin will delegate to GoraCompiler all schema read and file generation.

## Configuration
Configure the plugin in your project as follows:
```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "org.apache.gora.gradle:gora-gradle-plugin:0.6.1"
  }
}

apply plugin: "org.apache.gora"

dependencies {
    compile "org.apache.gora:gora-core:0.6.1"
}
```

## Usage in your project
Avro schema descriptors (.avsc) have to in the following directory :
```
src/main/resources/
```

You can generate Gora java types using compileGora directly or using build task:
```
$ gradle compileGora
$ gradle build
```

## Build this plugin
If you want to build this plugin from a Git checkout, please use Gradle Wrapper :
```
./gradlew clean build publishToMavenLocal
```

## Publishing to Bintray

As per the [Gora Release HOWTO](https://cwiki.apache.org/confluence/display/GORA/Apache+Gora+Release+Procedure+HOW_TO) we release the gora-gradle-plugin post release of the Gora release artifacts including the Maven artifacts.

In order to publish the plugin you must first register with [bintray](https://bintray.com/) and add your username and API key to your local System properties e.g. ~/.bashrc
```
export BINTRAY_USER=abc
export BINTRAY_KEY=xyz
```
Then run the following :

cd $GORA_HOME/gora-gradle-plugin; ./gradlew clean bintrayUpload

This does the following

 * Relaunch compile / assemble tasks
 * Deploy artifacts to your local Maven Repository (~/.m2/repository/)
 * Uploads those artifacts to Bintray (for publication on Maven Central repository)

