
How to execute from Pig Client in Hadoop
========================================

The following example can be executed in pig shell and dumps all the data from a table in HBase:

```
register gora/*.jar ;

webpage = LOAD '.' USING org.apache.gora.pig.GoraStorage('{
      "persistentClass": "com.example.WebPage",
      "fields": "baseUrl,status,content"
}') ;

dump webpage ;
```

**The first line**, '`register gora/*.jar`', will load all gora dependencies (if not installed in the client and cluster), the generated entities and resources.
The entities and resources `.jar` could be generated from a project including any custom Pig's UDF.
For example, the content could be:

```
- gora/
  - gora-core-X.Y.jar
  - gora-pig-X.Y.jar
  - gora-hbase-X.Y.jar
  - <more dependencies>.jar
  - my-app.jar$ [Entities and resources .jar]
    - gora.properties
    - gora-hbase-mapping.xml
    - hbase-site.xml
    - com/
      - example/
        - WebPage.class
      - udfs/
        - TfIdf.class
```

Gora modules are configured to copy all the dependencies to `$MODULE/lib` when compiling the project, so if you have checked out Gora's source and compiled it with maven, you
may copy the dependencies at `gora-core/libs` into `/<pig_client_folder>/gora`.

**The second line** loads the data. The *location* is ignored, so a dummy parameter `'.'` is passed in.

The Storage receives a JSON as parameter with the configuration. Some parameters are optional, and since we are configuring Gora already with the file `gora.properties` in `my-app.jar`, and
the connection to HBase with `hbase-site.xml` and mapping with `gora-hbase-mapping.xml`, we only have to configure the persistent class name and the fields to load.
Further configuration can me set with more parameters. See `GoraStorage.java` and `StorageConfiguration.java` javadoc for more details.
