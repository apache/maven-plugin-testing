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

import javax.inject.Inject;

import java.io.File;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoParameters;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
public class ParametersMojoTest {

    private static final String POM_DOT_XML_FILE = "pom.xml";

    private static final String DEFAULT_POM_DIR = "src/test/projects/default/";

    private static final String EXPLICIT_POM_DIR = "src/test/projects/explicit/";

    private static final String PROPERTY_POM_DIR = "src/test/projects/property/";

    @Inject
    private Log log;

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = DEFAULT_POM_DIR + POM_DOT_XML_FILE)
    void testDefaultPom(ParametersMojo mojo) {
        assertEquals("default", mojo.getWithDefault());
        assertEquals("default", mojo.getWithPropertyAndDefault());

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = EXPLICIT_POM_DIR + POM_DOT_XML_FILE)
    void testExplicitPom(ParametersMojo mojo) {
        assertEquals("explicitValue", mojo.getPlain());
        assertEquals("explicitWithPropertyValue", mojo.getWithProperty());
        assertEquals("explicitWithDefaultValue", mojo.getWithDefault());
        assertEquals("explicitWithPropertyAndDefaultValue", mojo.getWithPropertyAndDefault());

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = PROPERTY_POM_DIR + POM_DOT_XML_FILE)
    void testPropertyPom(ParametersMojo mojo) {
        assertDoesNotThrow(mojo::execute);
    }

    @Nested
    class TestPropertyPom {

        @Inject
        private MavenSession mavenSession;

        @BeforeEach
        void setup() {
            mavenSession.getUserProperties().setProperty("property", "testPropertyValue");
        }

        @Test
        @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = PROPERTY_POM_DIR + POM_DOT_XML_FILE)
        @MojoParameter(name = "plain", value = "test-${property}")
        void testPropertyPom(ParametersMojo mojo) {
            assertEquals("test-testPropertyValue", mojo.getPlain());
            assertEquals("testPropertyValue", mojo.getWithProperty());
            assertEquals("default", mojo.getWithDefault());
            assertEquals("testPropertyValue", mojo.getWithPropertyAndDefault());

            assertDoesNotThrow(mojo::execute);
        }
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = DEFAULT_POM_DIR + POM_DOT_XML_FILE)
    void simpleMojo(ParametersMojo mojo) {
        assertEquals(log, mojo.getLog());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = DEFAULT_POM_DIR + POM_DOT_XML_FILE)
    @MojoParameter(name = "plain", value = "plainValue")
    @MojoParameter(name = "withDefault", value = "withDefaultValue")
    void simpleMojoWithParameters(ParametersMojo mojo) {
        assertEquals("plainValue", mojo.getPlain());
        assertEquals("withDefaultValue", mojo.getWithDefault());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = DEFAULT_POM_DIR + POM_DOT_XML_FILE)
    @MojoParameters({
        @MojoParameter(name = "plain", value = "plainValue"),
        @MojoParameter(name = "withDefault", value = "withDefaultValue")
    })
    void simpleMojoWithParametersGroupingAnnotation(ParametersMojo mojo) {
        assertEquals("plainValue", mojo.getPlain());
        assertEquals("withDefaultValue", mojo.getWithDefault());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = DEFAULT_POM_DIR + POM_DOT_XML_FILE)
    @MojoParameter(name = "plain", value = "plainValue")
    void simpleMojoWithParameter(ParametersMojo mojo) {
        assertEquals("plainValue", mojo.getPlain());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @MojoParameter(name = "plain", value = "plainValue")
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = EXPLICIT_POM_DIR + POM_DOT_XML_FILE)
    void simpleMojoWithParameterInjectionWinsOverConfig(ParametersMojo mojo) {
        assertEquals("plainValue", mojo.getPlain());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @Basedir("src/test/projects/basedir-set-by-annotation")
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = POM_DOT_XML_FILE)
    void basedirInjectedWithBasedirAnnotation(ParametersMojo mojo) {
        assertEquals("i-have-a-basedir-set-by-annotation", mojo.getPlain());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @Basedir("src/test/projects/basedir-set-by-annotation")
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = POM_DOT_XML_FILE)
    @MojoParameter(name = "withDefault", value = "${basedir}/test-default-value.txt")
    @MojoParameter(name = "withProperty", value = "${project.basedir}/test-default-value.txt")
    void basedirInjectedWithBasedirAnnotationAndParams(ParametersMojo mojo) {
        assertEquals("i-have-a-basedir-set-by-annotation", mojo.getPlain());
        assertEquals(MojoExtension.getBasedir() + "/test-default-value.txt", mojo.getWithDefault());
        assertEquals(MojoExtension.getBasedir() + "/test-default-value.txt", mojo.getWithProperty());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @Basedir("/projects/basedir-set-by-annotation-classpath")
    @InjectMojo(goal = "parameters", pom = POM_DOT_XML_FILE)
    void basedirInjectedWithBasedirFromClasspathAnnotation(ParametersMojo mojo) {
        assertEquals("i-have-a-basedir-set-by-annotation-classpath", mojo.getPlain());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @Basedir("/projects/basedir-set-by-annotation-classpath")
    @InjectMojo(goal = "parameters", pom = POM_DOT_XML_FILE)
    @MojoParameter(name = "withDefault", value = "${basedir}/test-default-value.txt")
    @MojoParameter(name = "withProperty", value = "${project.basedir}/test-default-value.txt")
    void basedirInjectedWithBasedirFromClasspathAnnotationAndParams(ParametersMojo mojo) {
        assertEquals("i-have-a-basedir-set-by-annotation-classpath", mojo.getPlain());
        assertEquals(MojoExtension.getBasedir() + "/test-default-value.txt", mojo.getWithDefault());
        assertEquals(MojoExtension.getBasedir() + "/test-default-value.txt", mojo.getWithProperty());
        assertDoesNotThrow(mojo::execute);
    }

    @Nested
    class BaseDirInBeforeEach {

        @BeforeEach
        void setup() {
            // basedir defined for test should be already visible here
            String fs = File.separator;
            String endWith1 = fs + "src" + fs + "test" + fs + "projects" + fs + "basedir-set-by-annotation";
            String endWith2 = fs + "projects" + fs + "basedir-set-by-annotation-classpath";

            assertTrue(
                    MojoExtension.getBasedir().endsWith(endWith1)
                            || MojoExtension.getBasedir().endsWith(endWith2),
                    "Basedir: " + MojoExtension.getBasedir() + " is not ends with expected value '" + endWith1
                            + "' or '" + endWith2 + "'");
        }

        @Test
        @Basedir("src/test/projects/basedir-set-by-annotation")
        @InjectMojo(goal = "parameters", pom = POM_DOT_XML_FILE)
        void basedirInjectedWithBasedirAnnotation(ParametersMojo mojo) {
            assertDoesNotThrow(mojo::execute);
        }

        @Test
        @Basedir("/projects/basedir-set-by-annotation-classpath")
        @InjectMojo(goal = "parameters", pom = POM_DOT_XML_FILE)
        void basedirInjectedWithBasedirFromClasspathAnnotation(ParametersMojo mojo) {
            assertDoesNotThrow(mojo::execute);
        }
    }

    @Test
    @Basedir("src/test/projects/basedir-set-by-annotation")
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters")
    void basedirInjectedWithBasedirAnnotationDefaultPom(ParametersMojo mojo) {
        assertEquals("i-have-a-basedir-set-by-annotation", mojo.getPlain());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @Basedir("/projects/basedir-set-by-annotation-classpath")
    @InjectMojo(goal = "parameters")
    void basedirInjectedWithBasedirFromClasspathAnnotationDefaultPom(ParametersMojo mojo) {
        assertEquals("i-have-a-basedir-set-by-annotation-classpath", mojo.getPlain());
        assertDoesNotThrow(mojo::execute);
    }
}
