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

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
@Basedir("class-basedir")
@MojoParameter(name = "plain", value = "class-value")
class AnnotationLevelMojoTest {

    @Test
    @InjectMojo(goal = "parameters")
    void classLevelValues(ParametersMojo mojo) {
        assertEquals("class-value", mojo.getPlain());
        assertTrue(
                MojoExtension.getBasedir().endsWith("class-basedir"),
                "Basedir value did not came from class annotation");
    }

    @Test
    @InjectMojo(goal = "parameters")
    @Basedir("method-basedir")
    @MojoParameter(name = "plain", value = "method-value")
    void methodLevelValues(ParametersMojo mojo) {
        assertEquals("method-value", mojo.getPlain());
        assertTrue(
                MojoExtension.getBasedir().endsWith("method-basedir"),
                "Basedir value did not came from method annotation");
    }

    @Test
    void parameterLevelValues(
            @InjectMojo(goal = "parameters") @MojoParameter(name = "plain", value = "param-level-param-value")
                    ParametersMojo mojo) {
        assertEquals("param-level-param-value", mojo.getPlain());
    }

    @Test
    @MojoParameter(name = "plain", value = "method-value")
    void mojoParameterOnMethod(
            @InjectMojo(goal = "parameters") ParametersMojo mojo,
            @InjectMojo(goal = "parameters") @MojoParameter(name = "plain", value = "param-value")
                    ParametersMojo alternateMojo) {
        assertEquals("method-value", mojo.getPlain());
        assertEquals("param-value", alternateMojo.getPlain());
    }
}
