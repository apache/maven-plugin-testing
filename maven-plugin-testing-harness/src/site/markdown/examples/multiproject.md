title: Testing Multiproject
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
## Testing Multiproject

### NOTE

`JUnit 3` based tests are deprecated since `3.4.0`.

Use JUnit 5 annotations, consult [javadocs](../apidocs/org/apache/maven/api/plugin/testing/package-summary.html) for examples.

 **Note**: This example improves the [cookbook](../getting-started/index.html) for multi-project testing.


 Your Mojo should have `@aggregator` parameter, i.e.:



 - with java annotations ([maven-plugin-plugin 3.x](/plugin-tools/)):

```
@Mojo( name = "touch", aggregator = true )
public class MyMojo
    extends AbstractMojo
{
  ...
}
```


 - or with javadoc tags:

```
/**
 * @goal touch
 * @aggregator
 */
public class MyMojo
    extends AbstractMojo
{
  ...
}
```



 To test a Mojo in a multiproject area, you need to define several stubs, i.e. for the main test project and its modules.


### Create Stubs


 Stub for the main test project:



```
public class MyProjectStub
    extends MavenProjectStub
{
    /**
     * Default constructor
     */
    public MyProjectStub()
    {
        ...

        setExecutionRoot( true );
    }

    /** {@inheritDoc} */
    public MavenProject getExecutionProject()
    {
        return this;
    }
}
```

 Stubs for the subprojects:



```
public class SubProject1Stub
    extends MavenProjectStub
{
    /**
     * Default constructor
     */
    public SubProject1Stub()
    {
        ...
    }
}
```


```
public class SubProject2Stub
    extends MavenProjectStub
{
    /**
     * Default constructor
     */
    public SubProject2Stub()
    {
        ...
    }
}
```


### Configure `project-to-test` pom



```
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-my-plugin</artifactId>
        <configuration>
          ...
          <project implementation="org.apache.maven.plugin.my.stubs.MyProjectStub"/>
          <reactorProjects>
            <project implementation="org.apache.maven.plugin.my.stubs.SubProject1Stub"/>
            <project implementation="org.apache.maven.plugin.my.stubs.SubProject2Stub"/>
          </reactorProjects>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```


