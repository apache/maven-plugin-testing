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

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
@MojoTest
public class ProvidesInjectMojoTest {

    private static final String POM = "<project>" + "</project>";

    @Inject
    private MavenSession session;

    @Inject
    private MavenProject project;

    @Inject
    private MojoExecution mojoExecution;

    @Test
    @InjectMojo(pom = POM, goal = "test:test-plugin:0.0.1-SNAPSHOT:provides")
    public void bennShouldBeInjected(ProvidesInjectMojo mojo) {
        assertNotNull(mojo);
        assertSame(session, mojo.getSession());
        assertSame(session, mojo.getSessionFromBean());

        assertSame(project, mojo.getProject());
        assertSame(project, mojo.getProjectFromBean());

        assertSame(mojoExecution, mojo.getMojoExecution());
        assertSame(mojoExecution, mojo.getMojoExecutionFromBean());
    }
}
