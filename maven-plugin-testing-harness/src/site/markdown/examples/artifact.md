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


 **Note**: This example improves the [cookbook](../getting-started/index.html) to play with artifact handler.


 Sometimes, your Mojo uses project artifact and ArtifactHandler mechanisms. For instance, you could need to filter on Java projects, i.e.:



```
public class MyMojo
    extends AbstractMojo
{
    /**
     * The Maven Project.
     */
    @Component
    protected MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        ...

        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        if ( "java".equals( artifactHandler.getLanguage() ) )
        {
            ...
        }

        ...
     }
}
```

### Create Stubs



```
public class MyArtifactHandlerStub
    extends DefaultArtifactHandler
{
    private String language;

    public String getLanguage()
    {
        if ( language == null )
        {
            language = "java";
        }

        return language;
    }

    public void setLanguage( String language )
    {
        this.language = language;
    }
}
```


```
public class MyArtifactStub
    extends ArtifactStub
{
    private String groupId;

    private String artifactId;

    private String version;

    private String packaging;

    private VersionRange versionRange;

    private ArtifactHandler handler;

    /**
     * @param groupId
     * @param artifactId
     * @param version
     * @param packaging
     */
    public ProjectInfoPluginArtifactStub( String groupId, String artifactId,
                                          String version, String packaging )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        versionRange = VersionRange.createFromVersion( version );
    }

    /** {@inheritDoc} */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /** {@inheritDoc} */
    public String getGroupId()
    {
        return groupId;
    }

    /** {@inheritDoc} */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /** {@inheritDoc} */
    public String getArtifactId()
    {
        return artifactId;
    }

    /** {@inheritDoc} */
    public void setVersion( String version )
    {
        this.version = version;
    }

    /** {@inheritDoc} */
    public String getVersion()
    {
        return version;
    }

    /**
     * @param packaging
     */
    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    /**
     * @return the packaging
     */
    public String getPackaging()
    {
        return packaging;
    }

    /** {@inheritDoc} */
    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    /** {@inheritDoc} */
    public void setVersionRange( VersionRange versionRange )
    {
        this.versionRange = versionRange;
    }

    /** {@inheritDoc} */
    public ArtifactHandler getArtifactHandler()
    {
        return handler;
    }

    /** {@inheritDoc} */
    public void setArtifactHandler( ArtifactHandler handler )
    {
        this.handler = handler;
    }
}
```


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

        Artifact artifact = new MyArtifactStub( getGroupId(), getArtifactId(),
                                                getVersion(), getPackaging() );
        artifact.setArtifactHandler( new MyArtifactHandlerStub() );
        setArtifact( artifact );

        ...
    }

    ...
}
```


