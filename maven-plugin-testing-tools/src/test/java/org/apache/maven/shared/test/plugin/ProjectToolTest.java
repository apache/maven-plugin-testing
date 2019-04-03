package org.apache.maven.shared.test.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.test.plugin.ProjectTool.PomInfo;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * @version $Id$
 */
public class ProjectToolTest
    extends PlexusTestCase
{

    private File getPom( String test )
        throws IOException
    {
        File src = new File( "src/test/resources/projects/" + test );
        File dst = new File( "target/unit/projects/" + test );

        FileUtils.copyDirectoryStructureIfModified( src, dst );

        return new File( dst, "pom.xml" );
    }

    public void testManglePomForTesting_ShouldPopulateOutDirAndFinalName()
        throws Exception
    {
        ProjectTool tool = (ProjectTool) lookup( ProjectTool.ROLE, "default" );

        File pomFile = getPom( "basic" );

        PomInfo info = tool.manglePomForTesting( pomFile, "test", true );

        assertEquals( "target"+File.separatorChar+"it-build-target", info.getBuildDirectory() );
        assertEquals( "maven-it-plugin-test.jar", info.getFinalName() );
        assertEquals( "target"+File.separatorChar+"it-build-target"+File.separatorChar+"classes",info.getBuildOutputDirectory() );
    }

    public void testPackageProjectArtifact_ShouldPopulateArtifactFileWithJarLocation()
        throws Exception
    {
        ProjectTool tool = (ProjectTool) lookup( ProjectTool.ROLE, "default" );

        File pomFile = getPom( "basic" );

        MavenProject project = tool.packageProjectArtifact( pomFile, "test", true );

        String expectedPath = "target/unit/projects/basic/target/it-build-target/maven-it-plugin-test.jar";

        // be nice with windows
        String actualPath = StringUtils.replace( project.getArtifact().getFile().getPath(), "\\", "/" );

        assertEquals( expectedPath, actualPath );
    }

    public void testPackageProjectArtifact_ShouldPopulateWithCorrectArtifactAndMetadata()
        throws Exception
    {
        ProjectTool tool = (ProjectTool) lookup( ProjectTool.ROLE, "default" );

        File pomFile = getPom( "basic" );

        MavenProject project = tool.packageProjectArtifact( pomFile, "test", true );

        Artifact artifact = project.getArtifact();

        assertEquals( "maven-plugin", artifact.getType() );
        assertTrue( "Missing " + artifact.getFile(), artifact.getFile().exists() );

        Collection<ArtifactMetadata> metadata = artifact.getMetadataList();

        boolean foundPomMetadata = false;

        for ( ArtifactMetadata metadataItem : metadata )
        {
            if ( metadataItem instanceof ProjectArtifactMetadata )
            {
                foundPomMetadata = true;
            }
        }

        assertTrue( foundPomMetadata );
    }
    
    @Override
    protected void customizeContainerConfiguration(ContainerConfiguration configuration) 
    {
        configuration.setClassPathScanning( PlexusConstants.SCANNING_INDEX );
        configuration.setAutoWiring( true );      
    }
}
