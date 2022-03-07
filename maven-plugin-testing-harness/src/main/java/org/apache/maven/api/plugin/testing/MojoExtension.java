package org.apache.maven.api.plugin.testing;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.inject.internal.ProviderMethodsModule;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.testing.PlexusExtension;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class MojoExtension extends PlexusExtension implements ParameterResolver
{

    @Override
    public boolean supportsParameter( ParameterContext parameterContext, ExtensionContext extensionContext )
            throws ParameterResolutionException
    {
        return parameterContext.isAnnotated( InjectMojo.class )
                || parameterContext.getDeclaringExecutable().isAnnotationPresent( InjectMojo.class );
    }

    @Override
    public Object resolveParameter( ParameterContext parameterContext, ExtensionContext extensionContext )
            throws ParameterResolutionException
    {
        try
        {
            InjectMojo injectMojo = parameterContext.findAnnotation( InjectMojo.class ).orElseGet(
                    () -> parameterContext.getDeclaringExecutable().getAnnotation( InjectMojo.class ) );
            Class<?> holder = parameterContext.getTarget().get().getClass();
            return lookupMojo( holder, injectMojo );
        }
        catch ( Exception e )
        {
            throw new ParameterResolutionException( "Unable to resolve parameter", e );
        }
    }

    @Override
    public void beforeEach( ExtensionContext context )
            throws Exception
    {
        Field field = PlexusExtension.class.getDeclaredField( "basedir" );
        field.setAccessible( true );
        field.set( null, getBasedir() );
        field = PlexusExtension.class.getDeclaredField( "context" );
        field.setAccessible( true );
        field.set( this, context );

        getContainer().addComponent( getContainer(), PlexusContainer.class.getName() );

        ( (DefaultPlexusContainer) getContainer() ).addPlexusInjector( Collections.emptyList(),
                binder ->
                {
                    binder.install( ProviderMethodsModule.forObject( context.getRequiredTestInstance() ) );
                    binder.requestInjection( context.getRequiredTestInstance() );
                } );

        Map<Object, Object> map = getContainer().getContext().getContextData();

        try ( InputStream is = getClass().getResourceAsStream( "/" + getPluginDescriptorLocation() );
              Reader reader = new BufferedReader( new XmlStreamReader( is ) );
              InterpolationFilterReader interpolationReader = new InterpolationFilterReader( reader, map, "${", "}" ) )
        {

            PluginDescriptor pluginDescriptor = new PluginDescriptorBuilder().build( interpolationReader );

//            Artifact artifact =
//                    lookup( RepositorySystem.class ).createArtifact( pluginDescriptor.getGroupId(),
//                            pluginDescriptor.getArtifactId(),
//                            pluginDescriptor.getVersion(), ".jar" );
//
//            artifact.setFile( getPluginArtifactFile() );
//            pluginDescriptor.setPluginArtifact( artifact );
//            pluginDescriptor.setArtifacts( Collections.singletonList( artifact ) );

            for ( ComponentDescriptor<?> desc : pluginDescriptor.getComponents() )
            {
                getContainer().addComponentDescriptor( desc );
            }
        }
    }

    protected String getPluginDescriptorLocation()
    {
        return "META-INF/maven/plugin.xml";
    }

    private Mojo lookupMojo( Class<?> holder, InjectMojo injectMojo ) throws Exception
    {
        String goal = injectMojo.goal();
        String pom = injectMojo.pom();
        String[] coord = mojoCoordinates( goal );
        Xpp3Dom pomDom;
        if ( pom.startsWith( "file:" ) )
        {
            Path path = Paths.get( getBasedir() ).resolve( pom.substring( "file:".length() ) );
            pomDom = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( path.toFile() ) );
        }
        else if ( pom.startsWith( "classpath:" ) )
        {
            URL url = holder.getResource( pom.substring( "classpath:".length() ) );
            if ( url == null )
            {
                throw new IllegalStateException( "Unable to find pom on classpath: " + pom );
            }
            pomDom = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( url.openStream() ) );
        }
        else if ( pom.contains( "<project>" ) )
        {
            pomDom = Xpp3DomBuilder.build( new StringReader( pom ) );
        }
        else
        {
            Path path = Paths.get( getBasedir() ).resolve( pom );
            pomDom = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( path.toFile() ) );
        }
        PlexusConfiguration pluginConfiguration = extractPluginConfiguration( coord[1], pomDom );
        Mojo mojo = lookupMojo( coord, pluginConfiguration );
        return mojo;
    }

    protected String[] mojoCoordinates( String goal )
            throws Exception
    {
        if ( goal.matches( ".*:.*:.*:.*" ) )
        {
            return goal.split( ":" );
        }
        else
        {
            Path pluginPom = Paths.get( getBasedir(), "pom.xml" );
            Xpp3Dom pluginPomDom = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( pluginPom.toFile() ) );
            String artifactId = pluginPomDom.getChild( "artifactId" ).getValue();
            String groupId = resolveFromRootThenParent( pluginPomDom, "groupId" );
            String version = resolveFromRootThenParent( pluginPomDom, "version" );
            return new String[] { groupId, artifactId, version, goal };
        }
    }

    /**
     * lookup the mojo while we have all of the relavent information
     */
    protected Mojo lookupMojo( String[] coord, PlexusConfiguration pluginConfiguration )
            throws Exception
    {
        // pluginkey = groupId : artifactId : version : goal
        Mojo mojo = lookup( Mojo.class, coord[0] + ":" + coord[1] + ":" + coord[2] + ":" + coord[3] );
        if ( pluginConfiguration != null )
        {
            ExpressionEvaluator evaluator = new ResolverExpressionEvaluatorStub();
            ComponentConfigurator configurator = getContainer().lookup( ComponentConfigurator.class, "basic" );
            configurator.configureComponent( mojo, pluginConfiguration, evaluator, getContainer().getContainerRealm() );
        }

        return mojo;
    }

    private static Optional<Xpp3Dom> child( Xpp3Dom element, String name )
    {
        return Optional.ofNullable( element.getChild( name ) );
    }

    private static Stream<Xpp3Dom> children( Xpp3Dom element )
    {
        return Stream.of( element.getChildren() );
    }

    public static PlexusConfiguration extractPluginConfiguration( String artifactId, Xpp3Dom pomDom )
            throws Exception
    {
        Xpp3Dom pluginConfigurationElement = child( pomDom, "build" )
                .flatMap( buildElement -> child( buildElement, "plugins" ) )
                .map( MojoExtension::children )
                .orElseGet( Stream::empty )
                .filter( e -> e.getChild( "artifactId" ).getValue().equals( artifactId ) )
                .findFirst()
                .flatMap( buildElement -> child( buildElement, "configuration" ) )
                .orElseThrow( () -> new ConfigurationException(
                        "Cannot find a configuration element for a plugin with an "
                        + "artifactId of " + artifactId + "." ) );
        return new XmlPlexusConfiguration( pluginConfigurationElement );
    }

    /**
     * sometimes the parent element might contain the correct value so generalize that access
     *
     * TODO find out where this is probably done elsewhere
     */
    private static String resolveFromRootThenParent( Xpp3Dom pluginPomDom, String element )
            throws Exception
    {
        return Optional.ofNullable(
                    child( pluginPomDom, element )
                        .orElseGet( () -> child( pluginPomDom, "parent" )
                            .flatMap( e -> child( e, element ) )
                            .orElse( null ) ) )
                .map( Xpp3Dom::getValue )
                .orElseThrow( () -> new Exception( "unable to determine " + element ) );
    }


    /**
     * Convenience method to obtain the value of a variable on a mojo that might not have a getter.
     *
     * NOTE: the caller is responsible for casting to to what the desired type is.
     *
     * @param object
     * @param variable
     * @return object value of variable
     * @throws IllegalArgumentException
     */
    public static Object getVariableValueFromObject( Object object, String variable )
            throws IllegalAccessException
    {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses( variable, object.getClass() );
        field.setAccessible( true );
        return field.get( object );
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     *
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @param object
     * @return map of variable names and values
     */
    public static Map<String, Object> getVariablesAndValuesFromObject( Object object )
            throws IllegalAccessException
    {
        return getVariablesAndValuesFromObject( object.getClass(), object );
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     *
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @param clazz
     * @param object
     * @return map of variable names and values
     */
    public static Map<String, Object> getVariablesAndValuesFromObject( Class<?> clazz, Object object )
            throws IllegalAccessException
    {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = clazz.getDeclaredFields();
        AccessibleObject.setAccessible( fields, true );
        for ( Field field : fields )
        {
            map.put( field.getName(), field.get( object ) );
        }
        Class<?> superclass = clazz.getSuperclass();
        if ( !Object.class.equals( superclass ) )
        {
            map.putAll( getVariablesAndValuesFromObject( superclass, object ) );
        }
        return map;
    }

    /**
     * Convenience method to set values to variables in objects that don't have setters
     *
     * @param object
     * @param variable
     * @param value
     * @throws IllegalAccessException
     */
    public static void setVariableValueToObject( Object object, String variable, Object value )
            throws IllegalAccessException
    {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses( variable, object.getClass() );
        Objects.requireNonNull( field, "Field " + variable + " not found" );
        field.setAccessible( true );
        field.set( object, value );
    }

}
