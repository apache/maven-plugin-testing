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

// START SNIPPET: resolve-mojo-test
import javax.inject.Inject;

import java.io.File;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test class for simple artifact resolution mojo with repository system injection.
 */
@MojoTest(realRepositorySession = true)
class SimpleResolveMojoTest {

    @TempDir
    private File tempDir;

    // inject the Maven session to customize it for tests
    @Inject
    private MavenSession session;

    @BeforeEach
    void beforeEach() {
        // optionally customize the session to use a temporary local repository
        // default would be basedir/target/local-repo
        session.getRequest().setLocalRepositoryPath(tempDir);

        // other customizations can be done here, if needed
        // session.getRequest().setRemoteRepositories(customRemoteRepositories());
        // session.getRequest().setOffline(true);
        // session.getUserProperties().setProperty("maven.repo.local.tail", "your local repository");
    }

    @Test
    @InjectMojo(goal = "simple-resolve")
    @MojoParameter(name = "artifact", value = "org.apache.commons:commons-lang3:3.20.0")
    void artifactShouldBeResolved(SimpleResolveMojo mojo) {
        assertDoesNotThrow(mojo::execute);
    }

    @Nested
    class NestedTest {
        @Test
        @InjectMojo(goal = "simple-resolve")
        @MojoParameter(name = "artifact", value = "org.apache.commons:commons-lang3:3.20.0")
        void artifactShouldBeResolved(SimpleResolveMojo mojo) {
            assertDoesNotThrow(mojo::execute);
        }
    }
}
// END SNIPPET: resolve-mojo-test
