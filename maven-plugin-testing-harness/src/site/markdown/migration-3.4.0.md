title: Migration to 3.4.0+
author: Sławomir Jaranowski
date: January 2026

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
## Migration to 3.4.0+

Since version `3.4.0`, the Maven Plugin Testing Harness has been migrated to use JUnit 5 as the testing framework. 
This change allows for more modern testing practices and improved integration with other tools.

### Migration receipts

Replace

```java
public class MyMojoTest extends AbstractMojoTestCase {
}
```

by

```java
@MojoTest
class MyMojoTest {
}
```

---

Replace

```java
MyMojo myMojo = (MyMojo) lookupMojo("goal", pom);
```

by

```java
@InjectMojo(goal = "goal", pom = "src/test/resources/unit/project-to-test/pom.xml")
```

---

Replace

```java
public void test() {}
    MyCoponent myCoponent = lookup(MyCoponent.class);
}
```

by

```java
@MojoTest 
class MyMojoTest {
    
    @Inject
    private MyCoponent myCoponent;
}
```

Replace implementation of stubs in the configuration XML file from

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>exmaple.test</groupId>
  <artifactId>example</artifactId>
  <version>1.0-SNAPSHOT</version>
  <build>
    <plugins>
      <plugin>
        <groupId>org.exmaple.plugins</groupId>
        <artifactId>my-maven-plugin</artifactId>
        <configuration>
          <project implementation="org.example.plugin.stubs.MavenProjectStub" />
          <settings implementation="org.apache.maven.settings.Settings" />
          <reactorProjects>
            <project implementation="org.example.plugin.stubs.SubProject1Stub"/>
            <project implementation="org.example.plugin.stubs.SubProject2Stub"/>
          </reactorProjects>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

by code in the test class:

```java
import javax.inject.Inject;

import java.util.Arrays;

import org.apache.maven.api.di.Provides;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

@MojoTest
class MyMojoTest {

    @Provides
    MavenProject stubProject() {
        // return your stub implementation
        return new MavenProjectStub();
    }

    // default Mockito mock for MavenSession will be injected
    @Inject
    private MavenSession session;

    // project crated by stubProject() method will be injected
    @Inject
    private MavenProject project;

    @BeforeEach
    void setup() {
        MavenProject stub1 = new SubProject1Stub();
        MavenProject stub2 = new SubProject2Stub();

        // reactorProjects
        Mockito.when(session.getProjects()).thenReturn(Arrays.asList(project, stub1, stub2));

        Mockito.when(session.getSettings()).thenReturn(new Settings());
    }
}
```