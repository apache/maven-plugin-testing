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
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.model.Build;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Site;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Testing tool used to read MavenProject instances from pom.xml files, and to create plugin jar
 * files (package phase of the normal build process) for distribution to a test local repository
 * directory.
 *
 * @author jdcasey
 * @version $Id$
 */
@Deprecated
@Component( role = ProjectTool.class )
public class ProjectTool
{
    /** Plexus role */
    public static final String ROLE = ProjectTool.class.getName();

    public static final String INTEGRATION_TEST_DEPLOYMENT_REPO_URL = "integration-test.deployment.repo.url";

    @Requirement
    private BuildTool buildTool;

    @Requirement
    private RepositoryTool repositoryTool;

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * Construct a MavenProject instance from the specified POM file.
     *
     * @param pomFile current POM file
     * @return the Maven project from a file
     * @throws TestToolsException if any
     */
    public MavenProject readProject( File pomFile )
        throws TestToolsException
    {
        return readProject( pomFile, repositoryTool.findLocalRepositoryDirectory() );
    }

    /**
     * Construct a MavenProject instance from the specified POM file, using the specified local
     * repository directory to resolve ancestor POMs as needed.
     *
     * @param pomFile current POM file
     * @param localRepositoryBasedir
     * @return the Maven project from a file and a local repo
     * @throws TestToolsException if any
     */
    public MavenProject readProject( File pomFile, File localRepositoryBasedir )
        throws TestToolsException
    {
        try
        {
            ArtifactRepository localRepository = repositoryTool
                .createLocalArtifactRepositoryInstance( localRepositoryBasedir );

            ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            return projectBuilder.build( pomFile, request ).getProject();
        }
        catch ( ProjectBuildingException e )
        {
            throw new TestToolsException( "Error building MavenProject instance from test pom: " + pomFile, e );
        }
    }

    /**
     * Construct a MavenProject instance from the specified POM file with dependencies.
     *
     * @param pomFile current POM file
     * @return the Maven project with dependencies from a file
     * @throws TestToolsException if any
     */
    public MavenProject readProjectWithDependencies( File pomFile )
        throws TestToolsException
    {
        return readProjectWithDependencies( pomFile, repositoryTool.findLocalRepositoryDirectory() );
    }

    /**
     * Construct a MavenProject instance from the specified POM file with dependencies, using the specified local
     * repository directory to resolve ancestor POMs as needed.
     *
     * @param pomFile current POM file
     * @param localRepositoryBasedir
     * @return the Maven project with dependencies from a file and a local repo
     * @throws TestToolsException if any
     */
    public MavenProject readProjectWithDependencies( File pomFile, File localRepositoryBasedir )
        throws TestToolsException
    {
        try
        {
            ArtifactRepository localRepository = repositoryTool
                .createLocalArtifactRepositoryInstance( localRepositoryBasedir );

            ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            return projectBuilder.build( pomFile, request ).getProject();
        }
        catch ( ProjectBuildingException e )
        {
            throw new TestToolsException( "Error building MavenProject instance from test pom: " + pomFile, e );
        }
    }

    /**
     * Run the plugin's Maven build up to the package phase, in order to produce a jar file for
     * distribution to a test-time local repository. The testVersion parameter specifies the version
     * to be used in the &lt;version/&gt; element of the plugin configuration, and also in fully
     * qualified, unambiguous goal invocations (as in
     * org.apache.maven.plugins:maven-eclipse-plugin:test:eclipse).
     *
     * @param pomFile The plugin's POM
     * @param testVersion The version to use for testing this plugin. To promote test resiliency,
     *   this version should remain unchanged, regardless of what plugin version is under
     *   development.
     * @param skipUnitTests In cases where test builds occur during the unit-testing phase (usually
     *   a bad testing smell), the plugin jar must be produced <b>without</b> running unit tests.
     *   Otherwise, the testing process will result in a recursive loop of building a plugin jar and
     *   trying to unit test it during the build. In these cases, set this flag to <code>true</code>.
     * @return The resulting MavenProject, after the test version and skip flag (for unit tests)
     *   have been appropriately configured.
     * @throws TestToolsException if any
     */
    public MavenProject packageProjectArtifact( File pomFile, String testVersion, boolean skipUnitTests )
        throws TestToolsException
    {
        return packageProjectArtifact( pomFile, testVersion, skipUnitTests, null );
    }

    /**
     * Run the plugin's Maven build up to the package phase, in order to produce a jar file for
     * distribution to a test-time local repository. The testVersion parameter specifies the version
     * to be used in the &lt;version/&gt; element of the plugin configuration, and also in fully
     * qualified, unambiguous goal invocations (as in
     * org.apache.maven.plugins:maven-eclipse-plugin:test:eclipse).
     *
     * @param pomFile The plugin's POM
     * @param testVersion The version to use for testing this plugin. To promote test resiliency,
     *   this version should remain unchanged, regardless of what plugin version is under
     *   development.
     * @param skipUnitTests In cases where test builds occur during the unit-testing phase (usually
     *   a bad testing smell), the plugin jar must be produced <b>without</b> running unit tests.
     *   Otherwise, the testing process will result in a recursive loop of building a plugin jar and
     *   trying to unit test it during the build. In these cases, set this flag to <code>true</code>.
     * @param logFile The file to which build output should be logged, in order to allow later
     *   inspection in case this build fails.
     * @return The resulting MavenProject, after the test version and skip flag (for unit tests)
     *   have been appropriately configured.
     * @throws TestToolsException if any
     */
    public MavenProject packageProjectArtifact( File pomFile, String testVersion, boolean skipUnitTests, File logFile )
        throws TestToolsException
    {
        PomInfo pomInfo = manglePomForTesting( pomFile, testVersion, skipUnitTests );

        Properties properties = new Properties();

        List<String> goals = new ArrayList<String>();
        goals.add( "package" );

        File buildLog = logFile == null ? pomInfo.getBuildLogFile() : logFile;
        System.out.println( "Now Building test version of the plugin...\nUsing staged plugin-pom: "
            + pomInfo.getPomFile().getAbsolutePath() );

        buildTool.executeMaven( pomInfo.getPomFile(), properties, goals, buildLog );

        File artifactFile = new File( pomInfo.getPomFile().getParentFile(),
                                      pomInfo.getBuildDirectory() + "/" + pomInfo.getFinalName() );
        System.out.println( "Using IT Plugin Jar: " + artifactFile.getAbsolutePath() );
        try
        {
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            request.setLocalRepository( artifactRepositoryFactory.createArtifactRepository( "local",
                                            new File( "target/localrepo" ).getCanonicalFile().toURL().toExternalForm(),
                                            "default", null, null ) );
            request.setRepositorySession( MavenRepositorySystemUtils.newSession() );
            MavenProject project = projectBuilder.build( pomInfo.getPomFile(), request ).getProject();

            Artifact artifact = artifactFactory.createArtifact( project.getGroupId(), project.getArtifactId(), project
                .getVersion(), null, project.getPackaging() );

            artifact.setFile( artifactFile );
            artifact.addMetadata( new ProjectArtifactMetadata( artifact, project.getFile() ) );

            project.setArtifact( artifact );

            return project;
        }
        catch ( ProjectBuildingException e )
        {
            throw new TestToolsException( "Error building MavenProject instance from test pom: " + pomInfo.getPomFile(),
                                          e );
        }
        catch ( UnknownRepositoryLayoutException e )
        {
            throw new TestToolsException( "Error building ArtifactRepository instance from test pom: "
                + pomInfo.getPomFile(), e );
        }
        catch ( IOException e )
        {
            throw new TestToolsException( "Error building ArtifactRepository instance from test pom: "
                + pomInfo.getPomFile(), e );
        }
    }

    /**
     * Inject a special version for testing, to allow tests to unambiguously reference the plugin
     * currently under test. If test builds will be executed from the unit-testing phase, also inject
     * &lt;skip&gt;true&lt;/skip&gt; into the configuration of the <code>maven-surefire-plugin</code>
     * to allow production of a test-only version of the plugin jar without running unit tests.
     *
     * @param pomFile The plugin POM
     * @param testVersion The version that allows test builds to reference the plugin under test
     * @param skipUnitTests If true, configure the surefire plugin to skip unit tests
     * @return Information about mangled POM, including the temporary file to which it was written.
     * @throws TestToolsException if any
     */
    protected PomInfo manglePomForTesting( File pomFile, String testVersion, boolean skipUnitTests )
        throws TestToolsException
    {
        File input = pomFile;

        File output = new File( pomFile.getParentFile(), "pom-" + testVersion + ".xml" );
        output.deleteOnExit();

        Reader reader = null;
        Writer writer = null;

        Model model = null;
        String finalName = null;
        String buildDirectory = null;

        try
        {
            reader = ReaderFactory.newXmlReader( input );

            model = new MavenXpp3Reader().read( reader );
        }
        catch ( IOException e )
        {
            throw new TestToolsException( "Error creating test-time version of POM for: " + input, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new TestToolsException( "Error creating test-time version of POM for: " + input, e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        try
        {
            model.setVersion( testVersion );

            Build build = model.getBuild();
            if ( build == null )
            {
                build = new Build();
            }
            buildDirectory = build.getDirectory();

            if ( buildDirectory == null )
            {
                buildDirectory = "target";
            }

            buildDirectory = ( buildDirectory + File.separatorChar + "it-build-target" );
            build.setDirectory( buildDirectory );
            build.setOutputDirectory( buildDirectory + File.separatorChar + "classes" );
            System.out.println( "Using " + build.getDirectory() + " and " + build.getOutputDirectory()
                + " to build IT version of plugin" );
            model.setBuild( build );

            finalName = build.getFinalName();

            if ( finalName == null )
            {
                ArtifactHandler handler = artifactHandlerManager.getArtifactHandler( model.getPackaging() );

                String ext = handler.getExtension();

                finalName = model.getArtifactId() + "-" + model.getVersion() + "." + ext;
            }

            DistributionManagement distMgmt = new DistributionManagement();

            DeploymentRepository deployRepo = new DeploymentRepository();

            deployRepo.setId( "integration-test.output" );

            File tmpDir = FileUtils.createTempFile( "integration-test-repo", "", null );
            String tmpUrl = tmpDir.toURL().toExternalForm();

            deployRepo.setUrl( tmpUrl );

            distMgmt.setRepository( deployRepo );
            distMgmt.setSnapshotRepository( deployRepo );

            Repository localAsRemote = new Repository();
            localAsRemote.setId( "testing.mainLocalAsRemote" );

            File localRepoDir = repositoryTool.findLocalRepositoryDirectory();
            localAsRemote.setUrl( localRepoDir.toURL().toExternalForm() );

            model.addRepository( localAsRemote );
            model.addPluginRepository( localAsRemote );

            Site site = new Site();

            site.setId( "integration-test.output" );
            site.setUrl( tmpUrl );

            distMgmt.setSite( site );

            model.setDistributionManagement( distMgmt );

            model.addProperty( INTEGRATION_TEST_DEPLOYMENT_REPO_URL, tmpUrl );

            if ( skipUnitTests )
            {
                List<Plugin> plugins = build.getPlugins();
                Plugin plugin = null;
                for ( Plugin plug : plugins )
                {
                    if ( "maven-surefire-plugin".equals( plug.getArtifactId() ) )
                    {
                        plugin = plug;
                        break;
                    }
                }

                if ( plugin == null )
                {
                    plugin = new Plugin();
                    plugin.setArtifactId( "maven-surefire-plugin" );
                    build.addPlugin( plugin );
                }

                Xpp3Dom configDom = (Xpp3Dom) plugin.getConfiguration();
                if ( configDom == null )
                {
                    configDom = new Xpp3Dom( "configuration" );
                    plugin.setConfiguration( configDom );
                }

                Xpp3Dom skipDom = new Xpp3Dom( "skip" );
                skipDom.setValue( "true" );

                configDom.addChild( skipDom );
            }

            writer = WriterFactory.newXmlWriter( output );

            new MavenXpp3Writer().write( writer, model );
        }
        catch ( IOException e )
        {
            throw new TestToolsException( "Error creating test-time version of POM for: " + input, e );
        }
        finally
        {
            IOUtil.close( writer );
        }

        return new PomInfo( output, model.getGroupId(), model.getArtifactId(), model.getVersion(),
                            model.getBuild().getDirectory(), model.getBuild().getOutputDirectory(), finalName );
    }

    static final class PomInfo
    {
        private final File pomFile;

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String finalName;

        private final String buildDirectory;

        private final String buildOutputDirectory;

        PomInfo( File pomFile, String groupId, String artifactId, String version, String buildDirectory,
                 String buildOutputDirectory, String finalName )
        {
            this.pomFile = pomFile;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.buildDirectory = buildDirectory;
            this.buildOutputDirectory = buildOutputDirectory;
            this.finalName = finalName;
        }

        File getPomFile()
        {
            return pomFile;
        }

        String getBuildDirectory()
        {
            return buildDirectory;
        }

        String getBuildOutputDirectory()
        {
            return buildOutputDirectory;
        }

        String getFinalName()
        {
            return finalName;
        }

        File getBuildLogFile()
        {
            return new File( buildDirectory + "/test-build-logs/" + groupId + "_" + artifactId + "_" + version
                + ".build.log" );
        }
    }
}
