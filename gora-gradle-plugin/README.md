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
    classpath "org.apache.gora.gradle:gora-gradle-plugin:0.6"
  }
}

apply plugin: "org.apache.gora"

dependencies {
    compile "org.apache.gora:gora-core:0.4"
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

