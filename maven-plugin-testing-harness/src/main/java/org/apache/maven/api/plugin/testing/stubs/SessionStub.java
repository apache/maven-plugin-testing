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

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.maven.api.Listener;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.services.Service;
import org.apache.maven.settings.Settings;

/**
 * @author Olivier Lamy
 * @since 1.0-beta-1
 *
 */
public class SessionStub
    implements Session
{

    private Properties userProperties;

    private Properties systemProperties;

    private final Settings settings;

    public SessionStub( Settings settings )
    {
        this( null, null, settings );
    }

    public SessionStub()
    {
        this( null, null, null );
    }

    public SessionStub( Properties userProperties )
    {
        this( null, userProperties, null );
    }

    public SessionStub( Properties systemProperties, Properties userProperties, Settings settings )
    {

        this.settings = settings;

        this.systemProperties = new Properties();
        if ( systemProperties != null )
        {
            this.systemProperties.putAll( systemProperties );
        }
        this.systemProperties.putAll( System.getProperties() );

        this.userProperties = new Properties();
        if ( userProperties != null )
        {
            this.userProperties.putAll( userProperties );
        }
    }

    @Override
    public Settings getSettings()
    {
        return settings;
    }

    @Override
    public Properties getSystemProperties()
    {
        return this.systemProperties;
    }

    @Override
    public Properties getUserProperties()
    {
        return this.userProperties;
    }

    @Nonnull
    @Override
    public LocalRepository getLocalRepository()
    {
        return null;
    }

    @Nonnull
    @Override
    public List<RemoteRepository> getRemoteRepositories()
    {
        return null;
    }

    @Nonnull
    @Override
    public SessionData getData()
    {
        return null;
    }

    @Nonnull
    @Override
    public <T extends Service> T getService( Class<T> clazz ) throws NoSuchElementException
    {
        return null;
    }

    @Nonnull
    @Override
    public Session withLocalRepository( @Nonnull LocalRepository localRepository )
    {
        return null;
    }

    @Nonnull
    @Override
    public Session withRemoteRepositories( @Nonnull List<RemoteRepository> repositories )
    {
        return null;
    }

    @Override
    public void registerListener( @Nonnull Listener listener )
    {

    }

    @Override
    public void unregisterListener( @Nonnull Listener listener )
    {

    }

    @Nonnull
    @Override
    public Collection<Listener> getListeners()
    {
        return null;
    }
}
