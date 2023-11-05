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

import javax.inject.Named;

import java.nio.file.Paths;
import java.util.Properties;

import com.google.inject.Provides;
import org.apache.maven.api.Session;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.testing.stubs.SessionStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * @author Edwin Punzalan
 */
@MojoTest
public class ExpressionEvaluatorTest {

    private static final String LOCAL_REPO = "target/local-repo/";
    private static final String ARTIFACT_ID = "maven-test-mojo";
    private static final String COORDINATES = "groupId:" + ARTIFACT_ID + ":version:goal";
    private static final String CONFIG = "<project>\n"
            + "    <build>\n"
            + "        <plugins>\n"
            + "            <plugin>\n"
            + "                <artifactId>" + ARTIFACT_ID + "</artifactId>\n"
            + "                <configuration>\n"
            + "                    <basedir>${basedir}</basedir>\n"
            + "                    <workdir>${basedir}/workDirectory</workdir>\n"
            + "                </configuration>\n"
            + "            </plugin>\n"
            + "        </plugins>\n"
            + "    </build>\n"
            + "</project>\n";

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    public void testInjection(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "param", value = "paramValue")
    public void testParam(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertEquals("paramValue", mojo.param);
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = CONFIG)
    @MojoParameter(name = "param", value = "paramValue")
    @MojoParameter(name = "param2", value = "param2Value")
    public void testParams(ExpressionEvaluatorMojo mojo) {
        assertNotNull(mojo.basedir);
        assertNotNull(mojo.workdir);
        assertEquals("paramValue", mojo.param);
        assertEquals("param2Value", mojo.param2);
        assertDoesNotThrow(mojo::execute);
    }

    @Named(COORDINATES)
    public static class ExpressionEvaluatorMojo implements org.apache.maven.api.plugin.Mojo {
        private String basedir;

        private String workdir;

        private String param;

        private String param2;

        /** {@inheritDoc} */
        @Override
        public void execute() throws MojoException {
            if (basedir == null || basedir.isEmpty()) {
                throw new MojoException("basedir was not injected.");
            }

            if (workdir == null || workdir.isEmpty()) {
                throw new MojoException("workdir was not injected.");
            } else if (!workdir.startsWith(basedir)) {
                throw new MojoException("workdir does not start with basedir.");
            }
        }
    }

    @Provides
    @SuppressWarnings("unused")
    Session session() {
        Session session = SessionStub.getMockSession(LOCAL_REPO);
        doReturn(new Properties()).when(session).getSystemProperties();
        doReturn(new Properties()).when(session).getUserProperties();
        doAnswer(iom -> Paths.get(MojoExtension.getBasedir())).when(session).getRootDirectory();
        return session;
    }
}
