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
package org.apache.maven.api.plugin.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.internal.ProviderMethodsModule;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.di.Provides;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoLogWrapper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.testing.PlexusExtension;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;

/**
 * JUnit Jupiter extension that provides support for testing Maven plugins (Mojos).
 * This extension handles the lifecycle of Mojo instances in tests, including instantiation,
 * configuration, and dependency injection.
 *
 * <p>The extension is automatically registered when using the {@link MojoTest} annotation
 * on a test class. It provides the following features:</p>
 * <ul>
 *   <li>Automatic Mojo instantiation based on {@link InjectMojo} annotations</li>
 *   <li>Parameter injection using {@link MojoParameter} annotations</li>
 *   <li>POM configuration handling</li>
 *   <li>Project stub creation and configuration</li>
 *   <li>Maven session and build context setup</li>
 *   <li>Component dependency injection</li>
 * </ul>
 *
 * <p>Example usage in a test class:</p>
 * <pre>
 * {@code
 * @MojoTest
 * class MyMojoTest {
 *     @Test
 *     @InjectMojo(goal = "my-goal")
 *     @MojoParameter(name = "outputDirectory", value = "${project.build.directory}/generated")
 *     void testMojoExecution(MyMojo mojo) throws Exception {
 *         mojo.execute();
 *         // verify execution results
 *     }
 * }
 * }
 * </pre>
 **
 * <p>For custom POM configurations, you can specify a POM file using the {@link InjectMojo#pom()}
 * attribute. The extension will merge this configuration with default test project settings.</p>*
 *
 * @see MojoTest
 * @see InjectMojo
 * @see MojoParameter
 * @see Basedir
 * @since 3.4.0
 */
public class MojoExtension extends PlexusExtension implements ParameterResolver {

    // Namespace for storing/retrieving data related to MojoExtension
    private static final ExtensionContext.Namespace MOJO_EXTENSION = ExtensionContext.Namespace.create("MojoExtension");

    public static final String BASEDIR_IS_SET_KEY = "basedirIsSet";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return Mojo.class.isAssignableFrom(parameterContext.getParameter().getType())
                && (parameterContext.isAnnotated(InjectMojo.class)
                        || parameterContext.getDeclaringExecutable().isAnnotationPresent(InjectMojo.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        try {
            InjectMojo injectMojo = parameterContext
                    .findAnnotation(InjectMojo.class)
                    .orElseGet(() -> parameterContext.getDeclaringExecutable().getAnnotation(InjectMojo.class));

            Set<MojoParameter> mojoParameters = new LinkedHashSet<>();

            extensionContext.getEnclosingTestClasses().forEach(testClass -> {
                mojoParameters.addAll(Arrays.asList(testClass.getAnnotationsByType(MojoParameter.class)));
            });

            extensionContext
                    .getTestClass()
                    .map(c -> c.getAnnotationsByType(MojoParameter.class))
                    .map(Arrays::asList)
                    .ifPresent(mojoParameters::addAll);

            Optional.ofNullable(parameterContext.getDeclaringExecutable().getAnnotation(MojoParameter.class))
                    .ifPresent(mojoParameters::add);

            Optional.ofNullable(parameterContext.getDeclaringExecutable().getAnnotation(MojoParameters.class))
                    .map(MojoParameters::value)
                    .map(Arrays::asList)
                    .ifPresent(mojoParameters::addAll);

            mojoParameters.addAll(parameterContext.findRepeatableAnnotations(MojoParameter.class));

            Class<?> holder = parameterContext.getTarget().get().getClass();
            PluginDescriptor descriptor =
                    extensionContext.getStore(MOJO_EXTENSION).get(PluginDescriptor.class, PluginDescriptor.class);
            return lookupMojo(extensionContext, holder, injectMojo, mojoParameters, descriptor);
        } catch (Exception e) {
            throw new ParameterResolutionException("Unable to resolve parameter", e);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String basedir = AnnotationSupport.findAnnotation(context.getElement().get(), Basedir.class)
                .map(Basedir::value)
                .orElseGet(() -> {
                    return AnnotationSupport.findAnnotation(
                                    context.getRequiredTestClass(), Basedir.class, context.getEnclosingTestClasses())
                            .map(Basedir::value)
                            .orElse(null);
                });

        if (basedir == null) {
            basedir = getBasedir();
        } else {
            context.getStore(MOJO_EXTENSION).put(BASEDIR_IS_SET_KEY, Boolean.TRUE);
        }

        URL resource = context.getRequiredTestClass().getResource(basedir);
        if (resource != null) {
            basedir = Paths.get(resource.toURI()).toString();
        }

        // as PluginParameterExpressionEvaluator changes the basedir to absolute path, we need to normalize it here too
        basedir = new File(basedir).getAbsolutePath();

        setTestBasedir(basedir, context);

        PlexusContainer plexusContainer = getContainer(context);

        context.getRequiredTestInstances().getAllInstances().forEach(testInstance -> ((DefaultPlexusContainer)
                        plexusContainer)
                .addPlexusInjector(Collections.emptyList(), binder -> {
                    binder.install(ProviderMethodsModule.forObject(testInstance));
                    binder.install(new MavenProvidesModule(testInstance));
                }));

        addMock(plexusContainer, Log.class, () -> spy(new MojoLogWrapper(LoggerFactory.getLogger("anonymous"))));
        MavenProject mavenProject = addMock(plexusContainer, MavenProject.class, this::mockMavenProject);
        MojoExecution mojoExecution = addMock(plexusContainer, MojoExecution.class, this::mockMojoExecution);
        MavenSession mavenSession = addMock(plexusContainer, MavenSession.class, this::mockMavenSession);

        // prepare MavenExecutionRequest to be available in BeforeEach methods in test classes
        createMavenExecutionRequest(context);

        SessionScope sessionScope = plexusContainer.lookup(SessionScope.class);
        sessionScope.enter();
        sessionScope.seed(MavenSession.class, mavenSession);

        MojoExecutionScope executionScope = plexusContainer.lookup(MojoExecutionScope.class);
        executionScope.enter();
        executionScope.seed(MavenProject.class, mavenProject);
        executionScope.seed(MojoExecution.class, mojoExecution);

        context.getRequiredTestInstances().getAllInstances().forEach(testInstance -> ((DefaultPlexusContainer)
                        plexusContainer)
                .addPlexusInjector(Collections.emptyList(), binder -> {
                    binder.requestInjection(testInstance);
                }));

        Map<Object, Object> map = plexusContainer.getContext().getContextData();

        ClassLoader classLoader = context.getRequiredTestClass().getClassLoader();
        try (InputStream is = Objects.requireNonNull(
                        classLoader.getResourceAsStream(getPluginDescriptorLocation()),
                        "Unable to find plugin descriptor: " + getPluginDescriptorLocation());
                Reader reader = new BufferedReader(new XmlStreamReader(is));
                InterpolationFilterReader interpolationReader = new InterpolationFilterReader(reader, map, "${", "}")) {

            PluginDescriptor pluginDescriptor = new PluginDescriptorBuilder().build(interpolationReader);
            Plugin plugin = new Plugin();
            plugin.setGroupId(pluginDescriptor.getGroupId());
            plugin.setArtifactId(pluginDescriptor.getArtifactId());
            plugin.setVersion(pluginDescriptor.getVersion());
            pluginDescriptor.setPlugin(plugin);
            context.getStore(MOJO_EXTENSION).put(PluginDescriptor.class, pluginDescriptor);

            for (ComponentDescriptor<?> desc : pluginDescriptor.getComponents()) {
                plexusContainer.addComponentDescriptor(desc);
            }
        }
    }

    private <T> T addMock(PlexusContainer container, Class<T> role, Supplier<T> supplier)
            throws ComponentLookupException {
        if (!container.hasComponent(role)) {
            T mock = supplier.get();
            container.addComponent(mock, role, "default");
            return mock;
        } else {
            return container.lookup(role);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        SessionScope sessionScope = getContainer(context).lookup(SessionScope.class);
        sessionScope.exit();

        MojoExecutionScope executionScope = getContainer(context).lookup(MojoExecutionScope.class);
        executionScope.exit();

        super.afterEach(context);
    }

    /**
     * Default MojoExecution mock
     *
     * @return a MojoExecution mock
     */
    private MojoExecution mockMojoExecution() {
        return spy(new MojoExecution(null));
    }

    /**
     * Default MavenSession mock
     *
     * @return a MavenSession mock
     */
    private MavenSession mockMavenSession() {
        MavenSession session = Mockito.mock(MavenSession.class);
        lenient().when(session.getUserProperties()).thenReturn(new Properties());
        lenient().when(session.getSystemProperties()).thenReturn(new Properties());
        return session;
    }

    /**
     * Default MavenProject mock
     *
     * @return a MavenProject mock
     */
    private MavenProject mockMavenProject() {
        MavenProject mavenProject = spy(new MavenProject());
        Build build = spy(new Build());

        build.setDirectory(Paths.get(getBasedir(), "target").toString());
        build.setOutputDirectory(Paths.get(getBasedir(), "target", "classes").toString());
        build.setTestOutputDirectory(
                Paths.get(getBasedir(), "target", "test-classes").toString());
        build.setSourceDirectory(Paths.get(getBasedir(), "src", "main", "java").toString());
        build.setTestSourceDirectory(
                Paths.get(getBasedir(), "src", "test", "java").toString());

        Resource resource = spy(new Resource());
        resource.setDirectory(Paths.get(getBasedir(), "src", "main", "resource").toString());
        build.setResources(Arrays.asList(resource));

        Resource testResource = spy(new Resource());
        testResource.setDirectory(
                Paths.get(getBasedir(), "src", "test", "resource").toString());
        build.setTestResources(Arrays.asList(resource));

        mavenProject.setBuild(build);
        mavenProject.addCompileSourceRoot(build.getSourceDirectory());
        mavenProject.addTestCompileSourceRoot(build.getTestSourceDirectory());
        return mavenProject;
    }

    protected String getPluginDescriptorLocation() {
        return "META-INF/maven/plugin.xml";
    }

    private Mojo lookupMojo(
            ExtensionContext extensionContext,
            Class<?> holder,
            InjectMojo injectMojo,
            Collection<MojoParameter> mojoParameters,
            PluginDescriptor descriptor)
            throws Exception {
        String goal = injectMojo.goal();
        String pom = injectMojo.pom();
        Path basedir = Paths.get(getTestBasedir(extensionContext));
        String[] coord = mojoCoordinates(goal, descriptor);
        Xpp3Dom pomDom = null;
        Path pomPath = null;
        if (pom.startsWith("file:")) {
            pomPath = basedir.resolve(pom.substring("file:".length()));
        } else if (pom.startsWith("classpath:")) {
            URL url = holder.getResource(pom.substring("classpath:".length()));
            if (url == null) {
                throw new IllegalStateException("Unable to find pom on classpath: " + pom);
            }
            pomPath = Paths.get(url.toURI());
        } else if (pom.contains("<project>")) {
            pomDom = Xpp3DomBuilder.build(new StringReader(pom));
        } else if (!pom.isEmpty()) {
            pomPath = basedir.resolve(pom);
        } else if (isBasedirSet(extensionContext)) {
            // only look for a pom.xml if basedir is explicitly set
            pomPath = basedir.resolve("pom.xml");
        }

        if (pomDom == null) {
            if (pomPath != null && Files.exists(pomPath)) {
                pomDom = Xpp3DomBuilder.build(new XmlStreamReader(pomPath.toFile()));
            } else {
                pomDom = new Xpp3Dom("");
            }
        }

        Xpp3Dom pluginConfiguration = extractPluginConfiguration(coord[1], pomDom);
        if (!mojoParameters.isEmpty()) {
            List<Xpp3Dom> children = mojoParameters.stream()
                    .map(mp -> {
                        Xpp3Dom c = new Xpp3Dom(mp.name());
                        c.setValue(mp.value());
                        return c;
                    })
                    .collect(Collectors.toList());
            Xpp3Dom config = new Xpp3Dom("configuration");
            children.forEach(config::addChild);
            pluginConfiguration = Xpp3Dom.mergeXpp3Dom(config, pluginConfiguration);
        }
        return lookupMojo(extensionContext, coord, pluginConfiguration, descriptor, pomPath);
    }

    private boolean isBasedirSet(ExtensionContext extensionContext) {
        return extensionContext.getStore(MOJO_EXTENSION).getOrDefault(BASEDIR_IS_SET_KEY, Boolean.class, Boolean.FALSE);
    }

    protected String[] mojoCoordinates(String goal, PluginDescriptor pluginDescriptor) throws Exception {
        if (goal.matches(".*:.*:.*:.*")) {
            return goal.split(":");
        } else {
            String artifactId = pluginDescriptor.getArtifactId();
            String groupId = pluginDescriptor.getGroupId();
            String version = pluginDescriptor.getVersion();
            return new String[] {groupId, artifactId, version, goal};
        }
    }

    /**
     * lookup the mojo while we have all the relevent information
     */
    protected Mojo lookupMojo(
            ExtensionContext extensionContext,
            String[] coord,
            Xpp3Dom pluginConfiguration,
            PluginDescriptor descriptor,
            Path pomPath)
            throws Exception {
        PlexusContainer plexusContainer = getContainer(extensionContext);

        MavenExecutionRequest request = setupMavenExecutionRequest(extensionContext);
        plexusContainer.lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
        setupRepositorySession(extensionContext, request);

        // pluginkey = groupId : artifactId : version : goal
        Mojo mojo = plexusContainer.lookup(Mojo.class, coord[0] + ":" + coord[1] + ":" + coord[2] + ":" + coord[3]);

        Optional<MojoDescriptor> mojoDescriptor = descriptor.getMojos().stream()
                .filter(md ->
                        Objects.equals(md.getImplementation(), mojo.getClass().getName()))
                .findFirst();

        if (mojoDescriptor.isPresent()) {
            pluginConfiguration = finalizeConfig(pluginConfiguration, mojoDescriptor.get());
        }

        MavenSession session = plexusContainer.lookup(MavenSession.class);
        MavenProject mavenProject = plexusContainer.lookup(MavenProject.class);
        MojoExecution mojoExecution = plexusContainer.lookup(MojoExecution.class);

        if (mockingDetails(session).isMock()) {
            lenient().doReturn(mavenProject).when(session).getCurrentProject();
            lenient().doReturn(request.getLocalRepository()).when(session).getLocalRepository();
        }

        if (mockingDetails(mavenProject).isMock()) {
            if (mockingDetails(mavenProject).isSpy()) {
                // there is no setter for basedir, so set it via reflection
                setVariableValueToObject(
                        mavenProject, "basedir", Paths.get(getBasedir()).toFile());

                // we only set the pom file
                // setFile also change a basedir, so should not be used here
                mavenProject.setPomFile(
                        Optional.ofNullable(pomPath).map(Path::toFile).orElse(null));
            } else {
                lenient()
                        .doReturn(new File(getTestBasedir(extensionContext)))
                        .when(mavenProject)
                        .getBasedir();
            }
        }

        if (mojoDescriptor.isPresent() && mockingDetails(mojoExecution).isMock()) {
            if (mockingDetails(mojoExecution).isSpy()) {
                mojoExecution.setMojoDescriptor(mojoDescriptor.get());
            } else {
                lenient().doReturn(mojoDescriptor.get()).when(mojoExecution).getMojoDescriptor();
            }
        }

        if (pluginConfiguration != null) {
            ExpressionEvaluator evaluator =
                    new WrapEvaluator(plexusContainer, new PluginParameterExpressionEvaluator(session, mojoExecution));
            ComponentConfigurator configurator = new BasicComponentConfigurator();
            configurator.configureComponent(
                    mojo,
                    new XmlPlexusConfiguration(pluginConfiguration),
                    evaluator,
                    plexusContainer.getContainerRealm());
        }

        mojo.setLog(plexusContainer.lookup(Log.class));

        // clear invocations on mocks to avoid test interference
        if (mockingDetails(session).isMock()) {
            clearInvocations(session);
        }

        if (mockingDetails(mavenProject).isMock()) {
            clearInvocations(mavenProject);
        }

        if (mockingDetails(mojoExecution).isMock()) {
            clearInvocations(mojoExecution);
        }

        return mojo;
    }

    private boolean isRealRepositorySessionNotRequired(ExtensionContext context) {
        return !AnnotationSupport.findAnnotation(
                        context.getRequiredTestClass(), MojoTest.class, context.getEnclosingTestClasses())
                .map(MojoTest::realRepositorySession)
                .orElse(false);
    }

    /**
     * Create a MavenExecutionRequest if not already present in the MavenSession
     */
    private void createMavenExecutionRequest(ExtensionContext context) throws ComponentLookupException {
        PlexusContainer container = getContainer(context);
        MavenSession session = container.lookup(MavenSession.class);
        MavenExecutionRequest request = session.getRequest();

        if (request == null && mockingDetails(session).isMock()) {
            lenient()
                    .doReturn(spy(new DefaultMavenExecutionRequest()))
                    .when(session)
                    .getRequest();
        }
    }

    private MavenExecutionRequest setupMavenExecutionRequest(ExtensionContext context) throws ComponentLookupException {
        PlexusContainer container = getContainer(context);
        MavenSession session = container.lookup(MavenSession.class);
        MavenExecutionRequest request = session.getRequest();

        if (request == null) {
            // user can provide own MavenSession instance without a request
            request = new DefaultMavenExecutionRequest();
        }

        if (request.getStartTime() == null) {
            request.setStartTime(new Date());
        }

        if (request.getUserProperties().isEmpty()) {
            request.setUserProperties(session.getUserProperties());
        }

        if (request.getSystemProperties().isEmpty()) {
            request.setSystemProperties(session.getSystemProperties());
        }

        // set a default local repository path if none is set
        if (request.getLocalRepositoryPath() == null && request.getLocalRepository() == null) {
            request.setLocalRepositoryPath(getTestBasedir(context) + "/target/local-repo");
        }

        if (request.getBaseDirectory() == null) {
            request.setBaseDirectory(new File(getTestBasedir(context)));
        }

        return request;
    }

    private void setupRepositorySession(ExtensionContext context, MavenExecutionRequest request)
            throws ComponentLookupException {

        if (isRealRepositorySessionNotRequired(context)) {
            return;
        }

        PlexusContainer container = getContainer(context);

        MavenProject mavenProject = container.lookup(MavenProject.class);
        if (mockingDetails(mavenProject).isMock()) {
            lenient()
                    .doReturn(request.getRemoteRepositories())
                    .when(mavenProject)
                    .getRemoteArtifactRepositories();
            lenient()
                    .doReturn(request.getPluginArtifactRepositories())
                    .when(mavenProject)
                    .getPluginArtifactRepositories();
            lenient()
                    .doReturn(RepositoryUtils.toRepos(request.getRemoteRepositories()))
                    .when(mavenProject)
                    .getRemoteProjectRepositories();
            lenient()
                    .doReturn(RepositoryUtils.toRepos(request.getPluginArtifactRepositories()))
                    .when(mavenProject)
                    .getRemotePluginRepositories();
        }

        RepositorySystemSession repositorySystemSession =
                container.lookup(DefaultRepositorySystemSessionFactory.class).newRepositorySession(request);

        MavenSession session = container.lookup(MavenSession.class);
        if (mockingDetails(session).isMock()) {
            lenient().doReturn(repositorySystemSession).when(session).getRepositorySession();
        }
    }

    private Xpp3Dom finalizeConfig(Xpp3Dom config, MojoDescriptor mojoDescriptor) {
        List<Xpp3Dom> children = new ArrayList<>();
        if (mojoDescriptor != null && mojoDescriptor.getParameters() != null) {
            Xpp3Dom defaultConfiguration = MojoDescriptorCreator.convert(mojoDescriptor);
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                Xpp3Dom parameterConfiguration = config.getChild(parameter.getName());
                if (parameterConfiguration == null) {
                    parameterConfiguration = config.getChild(parameter.getAlias());
                }
                Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());
                parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults, Boolean.TRUE);
                if (parameterConfiguration != null) {
                    if (isEmpty(parameterConfiguration.getAttribute("implementation"))
                            && !isEmpty(parameter.getImplementation())) {
                        parameterConfiguration.setAttribute("implementation", parameter.getImplementation());
                    }
                    children.add(parameterConfiguration);
                }
            }
        }
        Xpp3Dom c = new Xpp3Dom("configuration");
        children.forEach(c::addChild);
        return c;
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static Optional<Xpp3Dom> child(Xpp3Dom element, String name) {
        return Optional.ofNullable(element.getChild(name));
    }

    private static Stream<Xpp3Dom> children(Xpp3Dom element) {
        return Stream.of(element.getChildren());
    }

    public static Xpp3Dom extractPluginConfiguration(String artifactId, Xpp3Dom pomDom) throws Exception {
        Xpp3Dom pluginConfigurationElement = child(pomDom, "build")
                .flatMap(buildElement -> child(buildElement, "plugins"))
                .map(MojoExtension::children)
                .orElseGet(Stream::empty)
                .filter(e -> e.getChild("artifactId").getValue().equals(artifactId))
                .findFirst()
                .flatMap(buildElement -> child(buildElement, "configuration"))
                .orElse(Xpp3DomBuilder.build(new StringReader("<configuration/>")));
        return pluginConfigurationElement;
    }

    /**
     * Convenience method to obtain the value of a variable on a mojo that might not have a getter.
     * <br>
     * Note: the caller is responsible for casting to what the desired type is.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getVariableValueFromObject(Object object, String variable) throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());
        field.setAccessible(true);
        return (T) field.get(object);
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     * <br>
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     */
    public static Map<String, Object> getVariablesAndValuesFromObject(Object object) throws IllegalAccessException {
        return getVariablesAndValuesFromObject(object.getClass(), object);
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     * <br>
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @return map of variable names and values
     */
    public static Map<String, Object> getVariablesAndValuesFromObject(Class<?> clazz, Object object)
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
     * Gets the base directory for test resources.
     * If not explicitly set via {@link Basedir}, returns the plugin base directory.
     */
    public static String getBasedir() {
        return PlexusExtension.getBasedir();
    }

    /**
     * Gets the file according to base directory for test resources.
     */
    public static File getTestFile(String path) {
        return PlexusExtension.getTestFile(path);
    }

    /**
     * Gets the path according to base directory for test resources.
     */
    public static String getTestPath(String path) {
        return PlexusExtension.getTestPath(path);
    }

    /**
     * Convenience method to set values to variables in objects that don't have setters
     */
    public static void setVariableValueToObject(Object object, String variable, Object value)
            throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());
        Objects.requireNonNull(field, "Field " + variable + " not found");
        field.setAccessible(true);
        field.set(object, value);
    }

    private static class WrapEvaluator implements TypeAwareExpressionEvaluator {

        private final PlexusContainer container;

        private final TypeAwareExpressionEvaluator evaluator;

        WrapEvaluator(PlexusContainer container, TypeAwareExpressionEvaluator evaluator) {
            this.container = container;
            this.evaluator = evaluator;
        }

        @Override
        public Object evaluate(String expression) throws ExpressionEvaluationException {
            return evaluate(expression, null);
        }

        @Override
        public Object evaluate(String expression, Class<?> type) throws ExpressionEvaluationException {
            Object value = evaluator.evaluate(expression, type);
            if (value == null) {
                String expr = stripTokens(expression);
                if (expr != null) {
                    try {
                        value = container.lookup(type, expr);
                    } catch (ComponentLookupException e) {
                        // nothing
                    }
                }
            }
            return value;
        }

        private String stripTokens(String expr) {
            if (expr.startsWith("${") && expr.endsWith("}")) {
                return expr.substring(2, expr.length() - 1);
            }
            return null;
        }

        @Override
        public File alignToBaseDirectory(File path) {
            return evaluator.alignToBaseDirectory(path);
        }
    }

    private static class MavenProvidesModule implements Module {
        private final Object testInstance;

        MavenProvidesModule(Object testInstance) {
            this.testInstance = testInstance;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void configure(Binder binder) {
            List<Method> providesMethods = AnnotationSupport.findAnnotatedMethods(
                    testInstance.getClass(), Provides.class, HierarchyTraversalMode.BOTTOM_UP);

            for (Method method : providesMethods) {
                if (method.getParameterCount() > 0) {
                    throw new IllegalArgumentException("Parameterized method are not supported " + method);
                }
                try {
                    method.setAccessible(true);
                    Object value = method.invoke(testInstance);
                    if (value == null) {
                        throw new IllegalArgumentException("Provides method returned null: " + method);
                    }
                    binder.bind((Class<Object>) method.getReturnType()).toInstance(value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }
}
