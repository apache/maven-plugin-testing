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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.maven.api.plugin.testing.MojoExtension;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 * An annotation for test methods that do not require the {@link MojoRule} to create and tear down the instance.
 *
 * @deprected As of version 3.4.0, it is advised to work with JUnit5 tests which do not
 * use rules but extensions {@link MojoExtension}
 * instead.
 *
 * @author Mirko Friedenhagen
 */
@Deprecated
@Retention(RUNTIME)
@Documented
@Target(METHOD)
public @interface WithoutMojo {}
