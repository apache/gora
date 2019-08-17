# Howto upgrade aaws-java-sdk version
This is a short howto for developers looking to upgrade the aws-java-sdk which simplifies the task of `<excluding>` all of the unnecessary transitive dependencies introduced from the code `com.amazonaws:aws-java-sdk` artifact.

1. Run the following 
```mvn dependency:tree -Dincludes=com.amazonaws:aws-java-sdk* | sed '/\[INFO\]    \+\-/,/\[INFO\] \-/!d;/\[INFO\] \-/d' | sed 's/^.*aws-/aws-/;s/:jar.*//' | sed $'s/^\(.*\)$/          <exclusion>\\\n            <groupId>com.amazonaws<\/groupId>\\\n            <artifactId>&<\/artifactId>\\\n          <\/exclusion>/'
```
2. In [parent pom.xml](https://github.com/apache/gora/blob/master/pom.xml) replace all lines within the `<exclusions>` portion of the `aws-java-sdk` dependency
```
      <!-- Amazon Dependencies -->
      <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk</artifactId>
        <version>${amazon.version}</version>
        <exclusions>
        ... enter code here note that should should NOT exclude the entry with <artifactId>aws-java-sdk-dynamodb</artifactId>
        </exclusions>
      </dependency>
```