title: Testing Multiproject
author: Vincent Siveton
date: February 2008

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
## Testing Multiproject

### NOTE

 **Note**: This example improves the [cookbook](../getting-started/index.html) for multi-project testing.

Your Mojo should have `aggregator` parameter set to `true` - [Maven Plugin Tools Java Annotations](https://maven.apache.org/plugin-tools/maven-plugin-tools-annotations/index.html)

```java
@Mojo(name = "touch", aggregator = true)
public class MyMojo extends AbstractMojo {
  ...
}
```


To test a Mojo in a multiproject area, you need to define several stubs, i.e. for the main test project and its modules.


### Configure main project and create Stubs for the subprojects

```java
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.mockito.Mockito;

@MojoTest
class AggregateTest {

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession session;

    @BeforeEach
    void setup() {
        // Configure the main project as execution root
        project.setExecutionRoot(true);

        MavenProject stub1 = new MavenProject();
        // configure stub1 as needed

        MavenProject stub2 = new MavenProject();
        // configure stub2 as needed

        // return all projects in the reactor - reactorProjects
        Mockito.when(session.getProjects()).thenReturn(Arrays.asList(project, stub1, stub2));
    }

    @Test
    @Basedir("/unit/aggregate-test")
    @InjectMojo(goal = "aggregate")
    void aggregate(AggregatorMojo mojo) throws Exception {

        mojo.execute();

        // Verify behavior across all projects
    }
}
```
