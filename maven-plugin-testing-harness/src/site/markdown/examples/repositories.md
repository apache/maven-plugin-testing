title: Testing Using Repositories
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
## Testing Using Repositories

### NOTE

`JUnit 3` based tests are deprecated since `3.4.0`.

Use JUnit 5 annotations, consult [javadocs](../apidocs/org/apache/maven/api/plugin/testing/package-summary.html) for examples.

 **Note**: This example improves the [cookbook](../getting-started/index.html) for testing repositories.


 When developing a Maven plugin you often need to play with repositories. Suppose that the MyMojo needs to download artifacts into your local repository, i.e.:



```
public class MyMojo
    extends AbstractMojo
{
    /**
     * Used for resolving artifacts
     */
    @Component
    private ArtifactResolver resolver;

    /**
     * Factory for creating artifact objects
     */
    @Component
    private ArtifactFactory factory;

    /**
     * Local Repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    public void execute()
        throws MojoExecutionException
    {
        ...

        Artifact artifact = factory.createArtifact( "junit", "junit", "3.8.1", "compile", "jar" );
        try
        {
            resolver.resolve( artifact, project.getRemoteArtifactRepositories(), localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to resolve artifact:" + artifact, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to find artifact:" + artifact, e );
        }

        ...
     }
}
```

### Create Stubs


 Stub for the test project:



```
public class MyProjectStub
    extends MavenProjectStub
{
    /**
     * Default constructor
     */
    public MyProjectStub()
    {
        ...
    }

    /** {@inheritDoc} */
    public List getRemoteArtifactRepositories()
    {
        ArtifactRepository repository = new DefaultArtifactRepository( "central", "http://repo.maven.apache.org/maven2",
                                                                       new DefaultRepositoryLayout() );

        return Collections.singletonList( repository );
    }
}
```


### Configure `project-to-test` pom



```
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-my-plugin</artifactId>
        <configuration>
          <!-- Specify where this pom will output files -->
          <outputDirectory>${basedir}/target/test-harness/project-to-test</outputDirectory>

          <!-- By default <<<${basedir}/target/local-repo", where basedir refers
               to the basedir of maven-my-plugin. -->
          <localRepository>${localRepository}</localRepository>
          <!-- The defined stub -->
          <project implementation="org.apache.maven.plugin.my.stubs.MyProjectStub"/>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

#### Execute test


 Calling `mvn test` will create `$\{basedir\}/target/local-repo/junitjunit/3.8.1/junit-3.8.1.jar` file.




