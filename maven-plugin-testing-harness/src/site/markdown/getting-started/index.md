title: How To Use Maven Plugin Testing Harness
author: Vincent Siveton
date: 2008-08-27

<!--  Licensed to the Apache Software Foundation (ASF) under one -->
<!--  or more contributor license agreements.  See the NOTICE file -->
<!--  distributed with this work for additional information -->
<!--  regarding copyright ownership.  The ASF licenses this file -->
<!--  to you under the Apache License, Version 2.0 (the -->
<!--  "License"); you may not use this file except in compliance -->
<!--  with the License.  You may obtain a copy of the License at -->
<!--  -->
<!--    http://www.apache.org/licenses/LICENSE-2.0 -->
<!--  -->
<!--  Unless required by applicable law or agreed to in writing, -->
<!--  software distributed under the License is distributed on an -->
<!--  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY -->
<!--  KIND, either express or implied.  See the License for the -->
<!--  specific language governing permissions and limitations -->
<!--  under the License. -->
## Cookbook: How To Use Maven Plugin Testing Harness?

This guide is intended as a reference for those developing Maven plugins, with self-contained references and solutions for common testing cases.

### Prerequisites

We assume that you have already created a plugin. In this cookbook, we make reference to `MyMojo` in `maven-my-plugin`.

You cane reference the [Guide to Developing Java Plugins](http://maven.apache.org/guides/plugin/guide-java-plugin-development.html) to create your first plugin.

### Testing Maven Plugin

#### Add `maven-plugin-testing-harness` dependency

As usual, just add `maven-plugin-testing-harness` as following in your POM. Be sure to specify `test` scope.

```
<project>
  ...
  <dependencies>
    <dependency>
      <groupId>org.apache.maven.plugin-testing</groupId>
      <artifactId>maven-plugin-testing-harness</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter-api</artifactId>
      <scope>test</scope>
    </dependency>
    ...
  </dependencies>
  ...
</project>
```

#### Create a `MyMojoTest`

Create a `MyMojoTest` (by convention) class in `src/test/java/org/example/maven/plugin/my` directory.

```java
import javax.inject.Inject;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MojoTest
class MyMojoTest {

    @Inject
    private MavenSession session;

    // optional setup before each test
    @BeforeEach
    void setUp() {
        // properties added to the Maven session can by used in the plugin configuration
        session.getUserProperties().setProperty("someProperty", "someValue");
    }

    @Test
    @InjectMojo(goal = "touch", pom = "src/test/resources/unit/project-to-test/pom.xml")
    public void testSomething(MyMojo myMojo) throws Exception {
        myMojo.execute();
        ...
    }

    @Test
    // you can use @Basedir pointing to test classpath resource
    @Basedir("/unit/project-to-test")
    // if you not provide 'pom' parameter, it will look for 'pom.xml' in basedir
    @InjectMojo(goal = "touch", pom = "another-pom.xml")
    public void testSomething2(MyMojo myMojo) throws Exception {
        myMojo.execute();
        ...
    }

    @Test
    @InjectMojo(goal = "touch")
    // you can provide simple parameters directly in the test method, without using a POM file
    @MojoParameter(name = "parameter1Name", value = "parameter1Value")
    @MojoParameter(name = "parameter2Name", value = "parameter2Value")
    public void testSomething3(MyMojo myMojo) throws Exception {
        myMojo.execute();
        ...
    }
}
```


**Note**: By convention, projects for unit testing should be in the test resources directory.

#### Configuring `project-to-test` POM

 Just create a POM as usual. Only plugin configuration is processed by the testing harness. **All other parts of the POM are ignored.**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.apache.maven.plugin.my.unit</groupId>
  <artifactId>project-to-test</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>
  <name>Test MyMojo</name>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-my-plugin</artifactId>
        <configuration>
          <!-- Specify the MyMojo parameter -->
          <outputDirectory>${basedir}/target/test-output</outputDirectory>
          <complexParameter>
            <param1>value1</param1>
            <!-- property set in BeforeEach method -->
            <param2>${someProperty}</param2>
          </complexParameter>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```


#### Execute test

As usual, just call:

```
mvn test
```

### Resources

- [Guide to Developing Java Plugins](http://maven.apache.org/guides/plugin/guide-java-plugin-development.html)
- [Guide to Configuring Plugins](http://maven.apache.org/guides/mini/guide-configuring-plugins.html)
- [Project unit tests](https://github.com/apache/maven-plugin-testing/tree/maven-plugin-testing-3.x/maven-plugin-testing-harness/src/test)



