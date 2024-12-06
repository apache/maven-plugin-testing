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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.inject.Module;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
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
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * @author jesse
 */
public abstract class AbstractMojoTestCase extends PlexusTestCase {
    private static final DefaultArtifactVersion MAVEN_VERSION;

    static {
        DefaultArtifactVersion version = null;
        String path = "/META-INF/maven/org.apache.maven/maven-core/pom.properties";

        try (InputStream is = AbstractMojoTestCase.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            if (is != null) {
                properties.load(is);
            }
            String property = properties.getProperty("version");
            if (property != null) {
                version = new DefaultArtifactVersion(property);
            }
        } catch (IOException e) {
            // odd, where did this come from
        }
        MAVEN_VERSION = version;
    }

    private ComponentConfigurator configurator;

    private PlexusContainer container;

    private Map<String, MojoDescriptor> mojoDescriptors;

    @Override
    protected void setUp() throws Exception {
        assertTrue(
                "Maven 3.2.4 or better is required",
                MAVEN_VERSION == null || new DefaultArtifactVersion("3.2.3").compareTo(MAVEN_VERSION) < 0);

        configurator = getContainer().lookup(ComponentConfigurator.class, "basic");
        Context context = container.getContext();
        Map<Object, Object> map = context.getContextData();

        try (InputStream is = getClass().getResourceAsStream("/" + getPluginDescriptorLocation());
                Reader reader = new BufferedReader(new XmlStreamReader(is));
                InterpolationFilterReader interpolationReader = new InterpolationFilterReader(reader, map, "${", "}")) {

            PluginDescriptor pluginDescriptor = new PluginDescriptorBuilder().build(interpolationReader);

            Artifact artifact = new DefaultArtifact(
                    pluginDescriptor.getGroupId(),
                    pluginDescriptor.getArtifactId(),
                    pluginDescriptor.getVersion(),
                    null,
                    "jar",
                    null,
                    new DefaultArtifactHandler("jar"));

            artifact.setFile(getPluginArtifactFile());
            pluginDescriptor.setPluginArtifact(artifact);
            pluginDescriptor.setArtifacts(Arrays.asList(artifact));

            for (ComponentDescriptor<?> desc : pluginDescriptor.getComponents()) {
                getContainer().addComponentDescriptor(desc);
            }

            mojoDescriptors = new HashMap<>();
            for (MojoDescriptor mojoDescriptor : pluginDescriptor.getMojos()) {
                mojoDescriptors.put(mojoDescriptor.getGoal(), mojoDescriptor);
            }
        }
    }

    /**
     * Returns best-effort plugin artifact file.
     * <p>
     * First, attempts to determine parent directory of META-INF directory holding the plugin descriptor. If META-INF
     * parent directory cannot be determined, falls back to test basedir.
     */
    private File getPluginArtifactFile() throws IOException {
        final String pluginDescriptorLocation = getPluginDescriptorLocation();
        final URL resource = getClass().getResource("/" + pluginDescriptorLocation);

        File file = null;

        // attempt to resolve relative to META-INF/maven/plugin.xml first
        if (resource != null) {
            if ("file".equalsIgnoreCase(resource.getProtocol())) {
                String path = resource.getPath();
                if (path.endsWith(pluginDescriptorLocation)) {
                    file = new File(path.substring(0, path.length() - pluginDescriptorLocation.length()));
                }
            } else if ("jar".equalsIgnoreCase(resource.getProtocol())) {
                // TODO is there a helper for this somewhere?
                try {
                    URL jarfile = new URL(resource.getPath());
                    if ("file".equalsIgnoreCase(jarfile.getProtocol())) {
                        String path = jarfile.getPath();
                        if (path.endsWith(pluginDescriptorLocation)) {
                            file = new File(path.substring(0, path.length() - pluginDescriptorLocation.length() - 2));
                        }
                    }
                } catch (MalformedURLException e) {
                    // not jar:file:/ URL, too bad
                }
            }
        }

        // fallback to test project basedir if couldn't resolve relative to META-INF/maven/plugin.xml
        if (file == null || !file.exists()) {
            file = new File(getBasedir());
        }

        return file.getCanonicalFile();
    }

    protected InputStream getPublicDescriptorStream() throws Exception {
        return new FileInputStream(new File(getPluginDescriptorPath()));
    }

    protected String getPluginDescriptorPath() {
        return getBasedir() + "/target/classes/META-INF/maven/plugin.xml";
    }

    protected String getPluginDescriptorLocation() {
        return "META-INF/maven/plugin.xml";
    }

    @Override
    protected void setupContainer() {
        ContainerConfiguration cc = setupContainerConfiguration();
        try {
            List<Module> modules = new ArrayList<>();
            addGuiceModules(modules);
            container = new DefaultPlexusContainer(cc, modules.toArray(new Module[0]));
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            fail("Failed to create plexus container.");
        }
    }

    /**
     * @since 3.0.0
     */
    protected void addGuiceModules(List<Module> modules) {
        // no custom guice modules by default
    }

    protected ContainerConfiguration setupContainerConfiguration() {
        ClassWorld classWorld =
                new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());

        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setName("maven");

        return cc;
    }

    @Override
    protected PlexusContainer getContainer() {
        if (container == null) {
            setupContainer();
        }

        return container;
    }

    /**
     * Lookup the mojo leveraging the subproject pom
     *
     * @param goal
     * @param pluginPom
     * @return a Mojo instance
     * @throws Exception
     */
    protected <T extends Mojo> T lookupMojo(String goal, String pluginPom) throws Exception {
        return lookupMojo(goal, new File(pluginPom));
    }

    /**
     * Lookup an empty mojo
     *
     * @param goal
     * @param pluginPom
     * @return a Mojo instance
     * @throws Exception
     */
    protected <T extends Mojo> T lookupEmptyMojo(String goal, String pluginPom) throws Exception {
        return lookupEmptyMojo(goal, new File(pluginPom));
    }

    /**
     * Lookup the mojo leveraging the actual subprojects pom
     *
     * @param goal
     * @param pom
     * @return a Mojo instance
     * @throws Exception
     */
    protected <T extends Mojo> T lookupMojo(String goal, File pom) throws Exception {
        File pluginPom = new File(getBasedir(), "pom.xml");

        Xpp3Dom pluginPomDom = Xpp3DomBuilder.build(new XmlStreamReader(pluginPom));

        String artifactId = pluginPomDom.getChild("artifactId").getValue();

        String groupId = resolveFromRootThenParent(pluginPomDom, "groupId");

        String version = resolveFromRootThenParent(pluginPomDom, "version");

        PlexusConfiguration pluginConfiguration = extractPluginConfiguration(artifactId, pom);

        return lookupMojo(groupId, artifactId, version, goal, pluginConfiguration);
    }

    /**
     * Lookup the mojo leveraging the actual subprojects pom
     *
     * @param goal
     * @param pom
     * @return a Mojo instance
     * @throws Exception
     */
    protected <T extends Mojo> T lookupEmptyMojo(String goal, File pom) throws Exception {
        File pluginPom = new File(getBasedir(), "pom.xml");

        Xpp3Dom pluginPomDom = Xpp3DomBuilder.build(new XmlStreamReader(pluginPom));

        String artifactId = pluginPomDom.getChild("artifactId").getValue();

        String groupId = resolveFromRootThenParent(pluginPomDom, "groupId");

        String version = resolveFromRootThenParent(pluginPomDom, "version");

        return lookupMojo(groupId, artifactId, version, goal, null);
    }

    /*
    protected Mojo lookupMojo( String groupId, String artifactId, String version, String goal, File pom )
    throws Exception
    {
    PlexusConfiguration pluginConfiguration = extractPluginConfiguration( artifactId, pom );

    return lookupMojo( groupId, artifactId, version, goal, pluginConfiguration );
    }
    */
    /**
     * lookup the mojo while we have all of the relavent information
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @param goal
     * @param pluginConfiguration
     * @return a Mojo instance
     * @throws Exception
     */
    protected <T extends Mojo> T lookupMojo(
            String groupId, String artifactId, String version, String goal, PlexusConfiguration pluginConfiguration)
            throws Exception {
        validateContainerStatus();

        // pluginkey = groupId : artifactId : version : goal

        T mojo = (T) lookup(Mojo.class, groupId + ":" + artifactId + ":" + version + ":" + goal);

        if (pluginConfiguration != null) {
            /* requires v10 of plexus container for lookup on expression evaluator
            ExpressionEvaluator evaluator = (ExpressionEvaluator) getContainer().lookup( ExpressionEvaluator.ROLE,
                                                                                        "stub-evaluator" );
            */
            ExpressionEvaluator evaluator = new ResolverExpressionEvaluatorStub();

            configurator.configureComponent(
                    mojo, pluginConfiguration, evaluator, getContainer().getContainerRealm());
        }

        return mojo;
    }

    /**
     *
     * @param project
     * @param goal
     * @return
     * @throws Exception
     * @since 2.0
     */
    protected <T extends Mojo> T lookupConfiguredMojo(MavenProject project, String goal) throws Exception {
        return lookupConfiguredMojo(newMavenSession(project), newMojoExecution(goal));
    }

    /**
     *
     * @param session
     * @param execution
     * @return
     * @throws Exception
     * @throws ComponentConfigurationException
     * @since 2.0
     */
    protected <T extends Mojo> T lookupConfiguredMojo(MavenSession session, MojoExecution execution)
            throws Exception, ComponentConfigurationException {
        MavenProject project = session.getCurrentProject();
        MojoDescriptor mojoDescriptor = execution.getMojoDescriptor();

        T mojo = (T) lookup(mojoDescriptor.getRole(), mojoDescriptor.getRoleHint());

        ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, execution);

        Xpp3Dom configuration = null;
        Plugin plugin = project.getPlugin(mojoDescriptor.getPluginDescriptor().getPluginLookupKey());
        if (plugin != null) {
            configuration = (Xpp3Dom) plugin.getConfiguration();
        }
        if (configuration == null) {
            configuration = new Xpp3Dom("configuration");
        }
        configuration = Xpp3Dom.mergeXpp3Dom(configuration, execution.getConfiguration());

        PlexusConfiguration pluginConfiguration = new XmlPlexusConfiguration(configuration);

        if (mojoDescriptor.getComponentConfigurator() != null) {
            configurator =
                    getContainer().lookup(ComponentConfigurator.class, mojoDescriptor.getComponentConfigurator());
        }

        configurator.configureComponent(
                mojo, pluginConfiguration, evaluator, getContainer().getContainerRealm());

        return mojo;
    }

    /**
     *
     * @param project
     * @return
     * @since 2.0
     */
    protected MavenSession newMavenSession(MavenProject project) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        MavenSession session = new MavenSession(container, MavenRepositorySystemUtils.newSession(), request, result);
        session.setCurrentProject(project);
        session.setProjects(Arrays.asList(project));
        return session;
    }

    /**
     *
     * @param goal
     * @return
     * @since 2.0
     */
    protected MojoExecution newMojoExecution(String goal) {
        MojoDescriptor mojoDescriptor = mojoDescriptors.get(goal);
        assertNotNull(String.format("The MojoDescriptor for the goal %s cannot be null.", goal), mojoDescriptor);
        MojoExecution execution = new MojoExecution(mojoDescriptor);
        finalizeMojoConfiguration(execution);
        return execution;
    }

    // copy&paste from o.a.m.l.i.DefaultLifecycleExecutionPlanCalculator.finalizeMojoConfiguration(MojoExecution)
    private void finalizeMojoConfiguration(MojoExecution mojoExecution) {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        Xpp3Dom executionConfiguration = mojoExecution.getConfiguration();
        if (executionConfiguration == null) {
            executionConfiguration = new Xpp3Dom("configuration");
        }

        Xpp3Dom defaultConfiguration = new Xpp3Dom(MojoDescriptorCreator.convert(mojoDescriptor));

        Xpp3Dom finalConfiguration = new Xpp3Dom("configuration");

        if (mojoDescriptor.getParameters() != null) {
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                Xpp3Dom parameterConfiguration = executionConfiguration.getChild(parameter.getName());

                if (parameterConfiguration == null) {
                    parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
                }

                Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());

                parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults, Boolean.TRUE);

                if (parameterConfiguration != null) {
                    parameterConfiguration = new Xpp3Dom(parameterConfiguration, parameter.getName());

                    if (StringUtils.isEmpty(parameterConfiguration.getAttribute("implementation"))
                            && StringUtils.isNotEmpty(parameter.getImplementation())) {
                        parameterConfiguration.setAttribute("implementation", parameter.getImplementation());
                    }

                    finalConfiguration.addChild(parameterConfiguration);
                }
            }
        }

        mojoExecution.setConfiguration(finalConfiguration);
    }

    /**
     * @param artifactId
     * @param pom
     * @return the plexus configuration
     * @throws Exception
     */
    protected PlexusConfiguration extractPluginConfiguration(String artifactId, File pom) throws Exception {

        try (Reader reader = new XmlStreamReader(pom)) {
            Xpp3Dom pomDom = Xpp3DomBuilder.build(reader);
            return extractPluginConfiguration(artifactId, pomDom);
        }
    }

    /**
     * @param artifactId
     * @param pomDom
     * @return the plexus configuration
     * @throws Exception
     */
    protected PlexusConfiguration extractPluginConfiguration(String artifactId, Xpp3Dom pomDom) throws Exception {
        Xpp3Dom pluginConfigurationElement = null;

        Xpp3Dom buildElement = pomDom.getChild("build");
        if (buildElement != null) {
            Xpp3Dom pluginsRootElement = buildElement.getChild("plugins");

            if (pluginsRootElement != null) {
                Xpp3Dom[] pluginElements = pluginsRootElement.getChildren();

                for (Xpp3Dom pluginElement : pluginElements) {
                    String pluginElementArtifactId =
                            pluginElement.getChild("artifactId").getValue();

                    if (pluginElementArtifactId.equals(artifactId)) {
                        pluginConfigurationElement = pluginElement.getChild("configuration");

                        break;
                    }
                }

                if (pluginConfigurationElement == null) {
                    throw new ConfigurationException("Cannot find a configuration element for a plugin with an "
                            + "artifactId of " + artifactId + ".");
                }
            }
        }

        if (pluginConfigurationElement == null) {
            throw new ConfigurationException(
                    "Cannot find a configuration element for a plugin with an artifactId of " + artifactId + ".");
        }

        return new XmlPlexusConfiguration(pluginConfigurationElement);
    }

    /**
     * Configure the mojo
     *
     * @param mojo
     * @param artifactId
     * @param pom
     * @return a Mojo instance
     * @throws Exception
     */
    protected <T extends Mojo> T configureMojo(T mojo, String artifactId, File pom) throws Exception {
        validateContainerStatus();

        PlexusConfiguration pluginConfiguration = extractPluginConfiguration(artifactId, pom);

        ExpressionEvaluator evaluator = new ResolverExpressionEvaluatorStub();

        configurator.configureComponent(
                mojo, pluginConfiguration, evaluator, getContainer().getContainerRealm());

        return mojo;
    }

    /**
     * Configure the mojo with the given plexus configuration
     *
     * @param mojo
     * @param pluginConfiguration
     * @return a Mojo instance
     * @throws Exception
     */
    protected <T extends Mojo> T configureMojo(T mojo, PlexusConfiguration pluginConfiguration) throws Exception {
        validateContainerStatus();

        ExpressionEvaluator evaluator = new ResolverExpressionEvaluatorStub();

        configurator.configureComponent(
                mojo, pluginConfiguration, evaluator, getContainer().getContainerRealm());

        return mojo;
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
    protected <T> T getVariableValueFromObject(Object object, String variable) throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());

        field.setAccessible(true);

        return (T) field.get(object);
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     *
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @param object
     * @return map of variable names and values
     */
    protected Map<String, Object> getVariablesAndValuesFromObject(Object object) throws IllegalAccessException {
        return getVariablesAndValuesFromObject(object.getClass(), object);
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
    protected Map<String, Object> getVariablesAndValuesFromObject(Class<?> clazz, Object object)
            throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();

        Field[] fields = clazz.getDeclaredFields();

        AccessibleObject.setAccessible(fields, true);

        for (Field field : fields) {
            map.put(field.getName(), field.get(object));
        }

        Class<?> superclass = clazz.getSuperclass();

        if (!Object.class.equals(superclass)) {
            map.putAll(getVariablesAndValuesFromObject(superclass, object));
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
    protected <T> void setVariableValueToObject(Object object, String variable, T value) throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());

        field.setAccessible(true);

        field.set(object, value);
    }

    /**
     * sometimes the parent element might contain the correct value so generalize that access
     *
     * TODO find out where this is probably done elsewhere
     *
     * @param pluginPomDom
     * @param element
     * @return
     * @throws Exception
     */
    private String resolveFromRootThenParent(Xpp3Dom pluginPomDom, String element) throws Exception {
        Xpp3Dom elementDom = pluginPomDom.getChild(element);

        // parent might have the group Id so resolve it
        if (elementDom == null) {
            Xpp3Dom pluginParentDom = pluginPomDom.getChild("parent");

            if (pluginParentDom != null) {
                elementDom = pluginParentDom.getChild(element);

                if (elementDom == null) {
                    throw new Exception("unable to determine " + element);
                }

                return elementDom.getValue();
            }

            throw new Exception("unable to determine " + element);
        }

        return elementDom.getValue();
    }

    /**
     * We should make sure this is called in each method that makes use of the container,
     * otherwise we throw ugly NPE's
     *
     * crops up when the subclassing code defines the setUp method but doesn't call super.setUp()
     *
     * @throws Exception
     */
    private void validateContainerStatus() throws Exception {
        if (getContainer() != null) {
            return;
        }

        throw new Exception("container is null, make sure super.setUp() is called");
    }
}
