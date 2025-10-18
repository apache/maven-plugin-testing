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


 In real plugin development, you will use specific Maven objects like `MavenProject`, `ArtifactRepository` or `MavenSettings`. You could use them by defining stubs.


 Suppose that you have the following dependencies in the maven-my-plugin pom:



```
<project>
  ...
  <dependencies>
    <dependency>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-artifact</artifactId>
      <version>2.0.8</version>
    </dependency>
    <dependency>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-project</artifactId>
      <version>2.0.8</version>
    </dependency>
    ...
  </dependencies>
</project>
```

 You will add the following in the `MyMojo`:



```
public class MyMojo
    extends AbstractMojo
{
    /**
     * The Maven Project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    /**
     * Local Repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    protected ArtifactRepository localRepository;

    /**
     * The Maven Settings.
     */
    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;

    ...
}
```

### Create Stubs


 You need to create stub objects to run `MyMojoTest#testSomething()`. By convention, the package name should reflect the stubs, i.e. in our case `org.apache.maven.plugin.my.stubs`.



```
public class MyProjectStub
    extends MavenProjectStub
{
    /**
     * Default constructor
     */
    public MyProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;
        try
        {
            model = pomReader.read( ReaderFactory.newXmlReader( new File( getBasedir(), "pom.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        setGroupId( model.getGroupId() );
        setArtifactId( model.getArtifactId() );
        setVersion( model.getVersion() );
        setName( model.getName() );
        setUrl( model.getUrl() );
        setPackaging( model.getPackaging() );

        Build build = new Build();
        build.setFinalName( model.getArtifactId() );
        build.setDirectory( getBasedir() + "/target" );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setOutputDirectory( getBasedir() + "/target/classes" );
        build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
        build.setTestOutputDirectory( getBasedir() + "/target/test-classes" );
        setBuild( build );

        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );

        List testCompileSourceRoots = new ArrayList();
        testCompileSourceRoots.add( getBasedir() + "/src/test/java" );
        setTestCompileSourceRoots( testCompileSourceRoots );
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/project-to-test/" );
    }
}
```


```
public class SettingsStub
    extends Settings
{
    /** {@inheritDoc} */
    public List getProxies()
    {
        return Collections.EMPTY_LIST;
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
          <outputDirectory>target/test-harness/project-to-test</outputDirectory>

          <!-- By default <<<${basedir}/target/local-repo", where basedir refers
               to the basedir of maven-my-plugin. -->
          <localRepository>${localRepository}</localRepository>
          <!-- The defined stubs -->
          <project implementation="org.apache.maven.plugin.my.stubs.MyProjectStub"/>
          <settings implementation="org.apache.maven.plugin.my.stubs.SettingsStub"/>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```


