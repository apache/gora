# Howto upgrade aaws-java-sdk version
This is a short howto for developers looking to upgrade the [aws-java-sdk](https://search.maven.org/search?q=g:com.amazonaws%20a:aws-java-sdk) which simplifies the task of `<excluding>` all of the unnecessary transitive dependencies introduced from the core `com.amazonaws:aws-java-sdk` artifact.

1. Run the following 
```mvn dependency:tree -Dincludes=com.amazonaws:aws-java-sdk* | sed '/\[INFO\]    \+\-/,/\[INFO\] \-/!d;/\[INFO\] \-/d' | sed 's/^.*aws-/aws-/;s/:jar.*//' | sed $'s/^\(.*\)$/          <exclusion>\\\n            <groupId>com.amazonaws<\/groupId>\\\n            <artifactId>&<\/artifactId>\\\n          <\/exclusion>/'
```
2. In [parent pom.xml](https://github.com/apache/gora/blob/master/pom.xml) replace all lines within the `<exclusions>` portion of the `aws-java-sdk` dependency with the output of the above task
```
      <!-- Amazon Dependencies -->
      <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk</artifactId>
        <version>${amazon.version}</version>
        <exclusions>
        ... enter code here N.B. It should NOT exclude the entry with <artifactId>aws-java-sdk-dynamodb</artifactId> or <artifactId>aws-java-sdk-core</artifactId>
        </exclusions>
      </dependency>
```
3. It's usually good practice to format the parent pom at 2 space indents prior to submitting a pull request.