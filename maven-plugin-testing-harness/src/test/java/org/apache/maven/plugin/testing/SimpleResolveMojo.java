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
package org.apache.maven.plugin.testing;

// START SNIPPET: resolve-mojo
import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Simple mojo for resolving artifacts.
 */
@Mojo(name = "simple-resolve")
public class SimpleResolveMojo extends AbstractMojo {

    private final MavenProject project;
    private final MavenSession session;
    private final RepositorySystem repositorySystem;

    @Parameter
    private String artifact;

    @Inject
    SimpleResolveMojo(MavenProject project, MavenSession session, RepositorySystem repositorySystem) {
        this.project = project;
        this.session = session;
        this.repositorySystem = repositorySystem;
    }

    @Override
    public void execute() throws MojoExecutionException {

        ArtifactRequest request =
                new ArtifactRequest(new DefaultArtifact(artifact), project.getRemoteProjectRepositories(), null);

        try {
            ArtifactResult result = repositorySystem.resolveArtifact(session.getRepositorySession(), request);
            getLog().info("Resolved artifact to " + result.getArtifact().getFile());
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Failed to resolve artifact", e);
        }
    }
}
// END SNIPPET: resolve-mojo
