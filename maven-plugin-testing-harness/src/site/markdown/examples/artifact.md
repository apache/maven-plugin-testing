title: Testing Project Artifact
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
## Testing Project Artifact


### NOTE

**Note**: This example improves the [cookbook](../getting-started/index.html) to play with artifact handler.


Sometimes, your Mojo uses project artifact and ArtifactHandler mechanisms. For instance, you could need to filter on Java projects, i.e.:

```java
import javax.inject.Inject;

import org.apache.maven.project.MavenProject;

public class MyMojo extends AbstractMojo {
    /**
     * The Maven Project.
     */
    private final MavenProject project;

    @Inject
    MyMojo(MavenProject project) {
        this.project = project;
    }

    public void execute() throws MojoExecutionException {
        // ...

        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        if ("java".equals(artifactHandler.getLanguage())) {
            //...
        }

        // ...
    }
}
```

### Create a test

```java
import org.apache.maven.api.di.Provides;import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

@MojoTest
class ArtifactTest {

    @Inject
    private MavenProject project;

    @Test
    @InjectMojo(goal = "test")
    void testUsingMockito(MyMojo mojo) {
        // Mock ArtifactHandler
        ArtifactHandler artifactHandler = Mockito.mock(ArtifactHandler.class);
        Mockito.when(artifactHandler.getLanguage()).thenReturn("java");

        // Mock Artifact
        Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getArtifactHandler()).thenReturn(artifactHandler);

        // Set the mocked Artifact to the default provided project
        project.setArtifact(artifact);

        // Now you can test your Mojo logic that depends on the ArtifactHandler
        mojo.execute();
    }

    @Nested
    class NestedTest1 {

        @Inject
        private ArtifactHandlerManager artifactHandlerManager;

        @Provides
        MavenProject stubbedProject() {
            MavenProject stubProject = new CustomMavenProject(); // your custom implementation
            
            ArtifactHandler stubArtifactHandler = new CustomArtifactHandler(); // your custom implementation
            
            // You can also get a real ArtifactHandler from the manager if needed
            ArtifactHandler jarArtifactHandler = artifactHandlerManager.getArtifactHandler("jar");
            
            Artifact stubArtifact = new CustomArtifact(stubArtifactHandler); // your custom implementation
            
            stubProject.setArtifact(stubArtifact);
            return stubProject;
        }

        @Test
        @InjectMojo(goal = "test")
        void testUsingStubbedProject (MyMojo mojo) {
            // Use the stubbed project in your test
            mojo.execute();
        }
    }
}
```

