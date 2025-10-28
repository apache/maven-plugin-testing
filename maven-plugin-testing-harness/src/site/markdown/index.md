title: Introduction
author: Vincent Siveton
date: February 2008

<!--  Licensed to the Apache Software Foundation (ASF) under one -->
<!--  or more contributor license agreements.  See the NOTICE file -->
<!--  distributed with this work for additional information -->
<!--  regarding copyright ownership.  The ASF licenses this file -->
<!--  to you under the Apache License, Version 2.0 (the -->
<!--  "License"); you may not use this file except in compliance -->
<!--  with the License.  You may obtain a copy of the License at -->
<!--  -->
<!--    http://www.apache.org/licenses/LICENSE-2.0 -->
<!--  -->
<!--  Unless required by applicable law or agreed to in writing, -->
<!--  software distributed under the License is distributed on an -->
<!--  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY -->
<!--  KIND, either express or implied.  See the License for the -->
<!--  specific language governing permissions and limitations -->
<!--  under the License. -->
## Maven Plugin Testing Harness


 The Maven Plugin Testing Harness provides mechanisms to manage tests on Mojos, i.e. by pre-constructing the [Plexus](http://plexus.codehaus.org) components, providing stub objects for Maven functionality such as projects, and populating fields from an XML file that resembles the plugin configuration in the POM.

 The best way to start is to read the cookbook [How to use Maven Plugin Testing Harness](./getting-started/index.html).

### Migration to 3.4.0

Since version `3.4.0`, the Maven Plugin Testing Harness has been migrated to use JUnit 5 as the testing framework. 
This change allows for more modern testing practices and improved integration with other tools.

JUnit 5 extension `MojoExtension` and annotation `@MojoTest` have similar functionalities 
as in [Maven 4](https://maven.apache.org/ref/4-LATEST/maven-impl-modules/maven-testing/apidocs/index.html)
for easier migration of tests for Maven 4.

Project still supports JUnit 3/4 `AbstractMojoTestCase` and JUnit 4 `MojoRule` tests for backward compatibility 
but new tests should be written using JUnit 5.

Therefore, some project dependencies have been set as optional to avoid conflicts with existing JUnit 3/4 and JUnit 5 tests.

Your project should depend on the following artifacts, if needed:

- `org.codehaus.plexus:plexus-archiver` - used by `ArtifactStubFactory` 
- `org.apache.maven:maven-compat` - used by `MojoRule`
- `org.junit.jupiter:junit-jupiter-api` - used for JUnit 5 tests
- `junit:junit` - used for JUnit 3/4 tests

### Examples


 The following examples shows how to use the Testing Harness in more advanced use cases:


 - [Testing Complex Mojo Parameters](./examples/complex-mojo-parameters.html)

 - [Testing Multiproject](./examples/multiproject.html)

 - [Testing Using Repositories](./examples/repositories.html)

 - [Testing Project Artifact](./examples/artifact.html)

 - [Plugins testing summary](https://maven.apache.org/plugin-developers/plugin-testing.html)


