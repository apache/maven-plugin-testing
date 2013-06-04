package org.apache.maven.plugin.testing;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.input.XmlStreamReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * {@link TestRule} for usage with Junit-4.10ff. This is just a wrapper for an embedded 
 * {@link AbstractMojoTestCase}, so all <tt>protected</tt> methods of the TestCase are 
 * exhibited as <tt>public</tt> in the rule. You may annotate single tests methods with 
 * {@link WithoutMojo} to prevent the rule from firing.
 *
 * @author Mirko Friedenhagen
 * @version $Id$
 * @since 2.2
 */
public class MojoRule
    implements TestRule
{
    private final AbstractMojoTestCase testCase;
    
    public MojoRule() 
    {
        this( new AbstractMojoTestCase() {} );
    }

    public MojoRule(AbstractMojoTestCase testCase)
    {
        this.testCase = testCase;
    }

    /**
     * May be overridden in the implementation to do stuff <em>after</em> the embedded test case 
     * is set up but <em>before</em> the current test is actually run.
     *
     * @throws Throwable
     */
    protected void before() throws Throwable
    {
        
    }
    
    /**
     * May be overridden in the implementation to do stuff after the current test was run.
     */
    protected void after() 
    {
        
    }

    public InputStream getPublicDescriptorStream()
        throws Exception
    {
        return testCase.getPublicDescriptorStream();
    }

    public String getPluginDescriptorPath()
    {
        return testCase.getPluginDescriptorPath();
    }

    public String getPluginDescriptorLocation()
    {
        return testCase.getPluginDescriptorLocation();
    }

    public void setupContainer()
    {
        testCase.setupContainer();
    }

    public ContainerConfiguration setupContainerConfiguration()
    {
        return testCase.setupContainerConfiguration();
    }
    
    public PlexusContainer getContainer()
    {
        return testCase.getContainer();
    }    
    
    /**
     * Lookup the mojo leveraging the subproject pom
     *
     * @param goal
     * @param pluginPom
     * @return a Mojo instance
     * @throws Exception
     */
    public Mojo lookupMojo( String goal, String pluginPom )
        throws Exception
    {
        return testCase.lookupMojo( goal, pluginPom );
    }

    /**
     * Lookup an empty mojo
     *
     * @param goal
     * @param pluginPom
     * @return a Mojo instance
     * @throws Exception
     */
    public Mojo lookupEmptyMojo( String goal, String pluginPom )
        throws Exception
    {
        return testCase.lookupEmptyMojo( goal, new File( pluginPom ) );
    }

    /**
     * Lookup the mojo leveraging the actual subprojects pom
     *
     * @param goal
     * @param pom
     * @return a Mojo instance
     * @throws Exception
     */
    public Mojo lookupMojo( String goal, File pom )
        throws Exception
    {
        return testCase.lookupMojo( goal, pom );
    }

    /**
     * Lookup the mojo leveraging the actual subprojects pom
     *
     * @param goal
     * @param pom
     * @return a Mojo instance
     * @throws Exception
     */
    public Mojo lookupEmptyMojo( String goal, File pom )
        throws Exception
    {
        return testCase.lookupEmptyMojo( goal, pom );
    }

    public Mojo lookupMojo( String groupId, String artifactId, String version, String goal,
                               PlexusConfiguration pluginConfiguration )
        throws Exception
    {
        return testCase.lookupMojo( groupId, artifactId, version, goal, pluginConfiguration );
    }

    public Mojo lookupConfiguredMojo( MavenProject project, String goal )
        throws Exception
    {
        return testCase.lookupConfiguredMojo( project, goal );
    }

    public Mojo lookupConfiguredMojo( MavenSession session, MojoExecution execution )
        throws Exception, ComponentConfigurationException
    {
        return testCase.lookupConfiguredMojo( session, execution );
    }

    public MavenSession newMavenSession( MavenProject project )
    {
        return testCase.newMavenSession( project );
    }

    public MojoExecution newMojoExecution( String goal )
    {
        return testCase.newMojoExecution( goal );
    }

    public PlexusConfiguration extractPluginConfiguration( String artifactId, File pom )
        throws Exception
    {
        return testCase.extractPluginConfiguration( artifactId, pom );
    }

    public PlexusConfiguration extractPluginConfiguration( String artifactId, Xpp3Dom pomDom )
        throws Exception
    {
        return testCase.extractPluginConfiguration( artifactId, pomDom );
    }

    public Mojo configureMojo( Mojo mojo, String artifactId, File pom )
        throws Exception
    {
        return testCase.configureMojo( mojo, artifactId, pom );
    }

    public Mojo configureMojo( Mojo mojo, PlexusConfiguration pluginConfiguration )
        throws Exception
    {
        return testCase.configureMojo( mojo, pluginConfiguration );
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
    public Object getVariableValueFromObject( Object object, String variable )
        throws IllegalAccessException
    {
        return testCase.getVariableValueFromObject( object, variable );
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     *
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @param object
     * @return map of variable names and values
     */
    public Map<String, Object> getVariablesAndValuesFromObject( Object object )
        throws IllegalAccessException
    {
        return testCase.getVariablesAndValuesFromObject( object );
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
    public Map<String, Object> getVariablesAndValuesFromObject( Class<?> clazz, Object object )
        throws IllegalAccessException
    {
        return testCase.getVariablesAndValuesFromObject( clazz, object );
    }

    /**
     * Convenience method to set values to variables in objects that don't have setters
     *
     * @param object
     * @param variable
     * @param value
     * @throws IllegalAccessException
     */
    public void setVariableValueToObject( Object object, String variable, Object value )
        throws IllegalAccessException
    {
        testCase.setVariableValueToObject( object, variable, value );
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        if (description.getAnnotation(WithoutMojo.class) != null) // skip.
        {
            return base;
        }
        return new Statement() 
        {
            @Override
            public void evaluate() throws Throwable 
            {
                testCase.setUp();
                before();
                try 
                {
                    base.evaluate();
                } 
                finally 
                {
                    after();
                }
            }            
        };       
    }
}
