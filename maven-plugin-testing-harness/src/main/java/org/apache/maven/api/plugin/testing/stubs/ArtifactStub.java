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

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.internal.impl.DefaultVersionParser;

/**
 *
 */
public class ArtifactStub implements Artifact
{
    private String groupId;
    private String artifactId;
    private String classifier;
    private String version;
    private String extension;
    private Path path;

    public ArtifactStub()
    {
        groupId = "";
        artifactId = "";
        version = "";
        classifier = "";
        extension = "";
    }

    public ArtifactStub( String groupId, String artifactId, String classifier, String version, String extension )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.version = version;
        this.extension = extension;
    }

    @Nonnull
    @Override
    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    @Nonnull
    @Override
    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    @Nonnull
    @Override
    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        return new DefaultVersionParser().parseVersion( version );
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    @Nonnull
    @Override
    public String getExtension()
    {
        return extension;
    }

    public void setExtension( String extension )
    {
        this.extension = extension;
    }

    @Override
    public boolean isSnapshot()
    {
        return false;
    }

    @Override
    public Type getType()
    {
        return new Type()
        {
            @Override
            public String getName()
            {
                return extension;
            }

            @Override
            public String getExtension()
            {
                return extension;
            }

            @Override
            public String getClassifier()
            {
                return null;
            }

            @Override
            public boolean isIncludesDependencies()
            {
                return false;
            }

            @Override
            public boolean isAddedToClasspath()
            {
                return false;
            }
        };
    }

    @Nonnull
    @Override
    public Optional<Path> getPath()
    {
        return Optional.ofNullable( path );
    }

    public void setPath( Path path )
    {
        this.path = path;
    }

    @Override
    public String toString()
    {
        return "ArtifactStub["
                + "groupId='" + groupId + '\''
                + ", artifactId='" + artifactId + '\''
                + ", classifier='" + classifier + '\''
                + ", version='" + version + '\''
                + ", extension='" + extension + '\''
                + ", path=" + path
                + ']';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ArtifactStub ) )
        {
            return false;
        }
        ArtifactStub that = ( ArtifactStub ) o;
        return Objects.equals( groupId, that.groupId )
                && Objects.equals( artifactId, that.artifactId )
                && Objects.equals( classifier, that.classifier )
                && Objects.equals( version, that.version )
                && Objects.equals( extension, that.extension )
                && Objects.equals( path, that.path );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( groupId, artifactId, classifier, version, extension, path );
    }
}
