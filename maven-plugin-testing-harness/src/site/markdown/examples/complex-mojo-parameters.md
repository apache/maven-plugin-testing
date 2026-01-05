title: Testing Complex Mojo Parameters
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
## Testing Complex Mojo Parameters

**Note**: This example improves the [cookbook](../getting-started/index.html) for testing complex Mojo parameters.

In real plugin development, you will use specific Maven objects like `MavenProject`, `MavenSession`, `MavenSettings` and so on.

### Provided mocks and stubs

The Maven Plugin Testing Harness provides mocks and stubs for the following Maven objects.
For creating mocks [Mockito](https://site.mockito.org/) framework is used.

#### MavenSession

There is provided a **mock** for `MavenSession` with basic configuration:

- `session#getUserProperties` and `session#getSystemProperties` empty `Properties` objects.
- `session#getCurrentProject` - a current prepared `MavenProject`
- `session#getLocalRepository` - equals to `request#getLocalRepository`
- `session#getRequest` returns a `spy` for `DefaultMavenExecutionRequest` with configuration
    - `request#getStartTime` - celling time
    - `request#getBaseDirectory()` - current base directory
    - `request#getUserProperties`, `request#getSystemProperties` - copy of `MavenSession` properties
    - `request#getLocalRepository`, `request#getLocalRepositoryPath` - point to local repository in `${basedir}/target/local-repo`
    - `request#getRemoteRepositories`, `request#getPluginArtifactRepositories` - default remote repositories for Maven Central

If `@MojoTest(realRepositorySession = true)` is used, then `session#getRepositorySession` returns
a real `RepositorySystemSession` configured with local repository in `${basedir}/target/local-repo`
and default remote repositories for Maven Central.

#### MavenProject

There is provided a **spy** for `MavenProject` with basic configuration:

- `project#getBasedir` - current base directory
- `project#getCompileSourceRoots` - `${basedir}/src/main/java`
- `project#getTestCompileSourceRoots` - `${basedir}/src/test/java`
- `project#getBuild` - a spy for `Build` with configuration
    - `build#getDirectory` - `${basedir}/target`
    - `build#getOutputDirectory` - `${basedir}/target/classes`
    - `build#getTestOutputDirectory` - `${basedir}/target/test-classes`
    - `build#getSourceDirectory` - `${basedir}/src/main/java`
    - `build#getTestSourceDirectory` - `${basedir}/src/test/java`
    - `build#getResources` - `${basedir}/src/main/resources`
    - `build#getTestResources` - `${basedir}/src/test/resources`

If `@MojoTest(realRepositorySession = true)` is used, then `project#getRemote*Repositories` returns default remote repositories for Maven Central.

#### MojoExecution

There is provided a **spy** for `MojoExecution` with current plugin Mojo descriptor.

#### Plugin logger

There is provided a **spy** for Slf4j wrapper of `org.apache.maven.plugin.logging.Log` interface. 
You can verify log messages using Mockito verifications.

### Create custom Stubs for Maven objects

You can create your own stubs for Maven objects by define them in plugin test.

If you provide your own stub for a Maven object, then it will be used instead of the default provided one.

```java
import javax.inject.Inject;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@MojoTest
class MyMojoTest {

    // define your stub for MavenProject
    @Provides
    MavenProject stubProject() {
        // return your stub implementation
        return new MavenProjectStub();
    }

    @Provides
    MavenSession stubSession() {
        // return your stub implementation
        return new MyMavenSessionStub();
    }

    // inject the stubbed or default MavenSession
    @Inject
    private MavenSession session;

    @Inject
    private Log log;
    
    @BeforeEach

    void setup() {
        // customize injected stubs or mocks
        Mockito.when(session.getSettings()).thenReturn(new MySettingsStub());
    }

    @Test
    @InjectMojo(goal = "my-goal")
    void myMojoTest(MyMojo mojo) {
        mojo.execute();
        // your verifications, eg
        Mockito.verify(log).info("My info log message");
    }

    // you can also group stubs in nested test classes
    @Nested
    class NestedTest {

        // define your stub for MavenProject in nested class
        @Provides
        MavenProject stubProject() {
            // return your stub implementation
            return new MavenProjectStub2();
        }

        @BeforeEach
        void setup() {
            // customize injected stubs or mocks for this nested class
            Mockito.when(session.getSettings()).thenReturn(new MySettingsStub());
        }

        @Test
        @InjectMojo(goal = "my-goal")
        void myMojoTest(MyMojo mojo) {
            mojo.execute();
            // your verifications
        }
    }
}
```
