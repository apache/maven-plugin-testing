package org.apache.maven.api.plugin.testing.stubs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.annotation.Nonnull;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Project;
import org.apache.maven.model.Model;

/**
 * @author Olivier Lamy
 * @since 1.0-beta-1
 *
 */
public class ProjectStub
        implements Project
{

    private Model model = new Model();
    private Path basedir;
    private boolean executionRoot;

    @Nonnull
    @Override
    public String getGroupId()
    {
        return model.getGroupId();
    }

    public void setGroupId( String groupId )
    {
        model.setGroupId( groupId );
    }

    @Nonnull
    @Override
    public String getArtifactId()
    {
        return model.getArtifactId();
    }

    public void setArtifactId( String artifactId )
    {
        model.setArtifactId( artifactId );
    }

    @Nonnull
    @Override
    public String getVersion()
    {
        return model.getVersion();
    }

    public void setVersion( String version )
    {
        model.setVersion( version );
    }

    public String getName()
    {
        return model.getName();
    }

    public void setName( String name )
    {
        model.setName( name );
    }

    @Nonnull
    @Override
    public String getPackaging()
    {
        return model.getPackaging();
    }

    @Nonnull
    @Override
    public Artifact getArtifact()
    {
        return null;
    }

    @Nonnull
    @Override
    public Model getModel()
    {
        return model;
    }

    @Nonnull
    @Override
    public Path getPomPath()
    {
        return null;
    }

    @Nonnull
    @Override
    public List<Dependency> getDependencies()
    {
        return null;
    }

    @Nonnull
    @Override
    public List<Dependency> getManagedDependencies()
    {
        return null;
    }

    @Override
    public Path getBasedir()
    {
        return basedir;
    }

    public void setBasedir( Path basedir )
    {
        this.basedir = basedir;
    }

    @Override
    public boolean isExecutionRoot()
    {
        return executionRoot;
    }

    public void setExecutionRoot( boolean executionRoot )
    {
        this.executionRoot = executionRoot;
    }
}
