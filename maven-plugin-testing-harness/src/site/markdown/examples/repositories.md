title: Testing Using Repositories
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

## Testing Using Repositories

**Note**: This example improves the [cookbook](../getting-started/index.html) for testing repositories.
 
When developing a Maven plugin you often need to play with repositories. 
Suppose that the Mojo needs to download artifacts into your local repository.

You need annotate unit test with `@MojoTest(realRepositorySession = true)` to enable real repository session.

Then provided mock for `MavenSession` will have a real repository session with local repository configured.

Mock for `MavenProject` will also have mocked methods `getRemote*Repositories`.

### Project dependencies for test

For real repository session you need to add resolver transport dependency in test scope to your `pom.xml`:

<!-- MACRO{snippet|id=resolver-transport|file=maven-plugin-testing-harness/pom.xml} -->

### Example Mojo to test

<!-- MACRO{snippet|id=resolve-mojo|file=maven-plugin-testing-harness/src/test/java/org/apache/maven/plugin/testing/SimpleResolveMojo.java} -->

### Unit test

<!-- MACRO{snippet|id=resolve-mojo-test|file=maven-plugin-testing-harness/src/test/java/org/apache/maven/plugin/testing/SimpleResolveMojoTest.java} -->


#### Execute test

 Calling `mvn test` will create `<temp-directory>/org/apache/commons/commons-lang3/3.20.0/commons-lang3-3.20.0.jar` file.




