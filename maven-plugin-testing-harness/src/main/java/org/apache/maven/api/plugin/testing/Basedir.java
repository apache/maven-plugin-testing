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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the base directory for test resources in Maven plugin tests.
 * This annotation can be applied to test methods to define where test resources are located.
 *
 ** <p>Example usage:</p>
 * <pre>
 * {@code
 * @MojoTest
 * class MyMojoTest {
 *     @Test
 *     @Basedir("src/test/resources/specific-test-case")
 *     @InjectMojo(goal = "compile")
 *     void testSpecificCase(MyMojo mojo) {
 *         // Test resources will be loaded from src/test/resources/specific-test-case
 *         mojo.execute();
 *     }
 * }
 * }
 * </pre>
 *
 * @see MojoTest
 * @see MojoExtension
 * @since 3.4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.METHOD)
public @interface Basedir {
    String value() default "";
}
