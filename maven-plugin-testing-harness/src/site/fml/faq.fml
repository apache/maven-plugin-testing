<?xml version="1.0" encoding="UTF-8"?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<faqs id="FAQ" title="Frequently Asked Questions">
  <part id="General">
    <faq id="What is a Mojo Testing Harness">
      <question>What is a Mojo Testing Harness?</question>
      <answer>
        <p>
          A unit test attempts to verify a mojo as an isolated unit, by mocking out the rest of the Maven environment.
          A mojo unit test does not attempt to run your plugin in the context of a real Maven build.
          Unit tests are designed to be fast.
        </p>
        <p>
          This testing library is <b>NOT</b> designed for integration or functional testing:
          <a href="/plugins/maven-invoker-plugin/"><code>maven-invoker-plugin</code></a> is the way to go if you need it,
          which gives you a complete Maven environment at the cost of more resources and time consumption.
        </p>
      </answer>
    </faq>
    <faq id="What kinds of unit tests are supported">
      <question>What kind of unit tests are supported?</question>
      <answer>
        <p>
          <dl>
            <dt>JUnit 5 Extension - <i>@MojoTest</i> annotation</dt>
            <dd>The preferred way to test Mojos is to use the JUnit 5 extension provided by the
              <i>maven-plugin-testing-harness</i>. You can annotate your test class with <code>@MojoTest</code>
              to have the extension set up the necessary Maven components for you. You can then inject your Mojo
              and any required Maven components directly into your test class.
              See <a href="./apidocs/org/apache/maven/api/plugin/testing/package-summary.html">javadocs</a> for examples.
            </dd>
            <dt>TestCase from JUnit - deprecated</dt>
            <dd>You could use the <a href="http://junit.org/">JUnit framework</a> to test your plugin in
              the same way you'd write any other JUnit test cases, i.e. by writing a test class which extends
              <i>TestCase</i>.</dd>
            <dt>TestCase from Plexus - deprecated</dt>
            <dd>Mojos are written to take specific advantage of the <i>Plexus container</i>.
              If you need Plexus container services, you could write your class which extends <i>PlexusTestCase</i>,
              instead of <i>TestCase</i>.</dd>
            <dt>TestCase from Testing Harness - deprecated</dt>
            <dd>If you need to inject Maven objects into your mojo, you could use the <i>maven-plugin-testing-harness</i>.
              The <i>maven-plugin-testing-harness</i> is explicitly intended to test the
              <i>org.apache.maven.reporting.AbstractMavenReport#execute()</i> implementation.</dd>
          </dl>
        </p>
      </answer>
    </faq>
  </part>
</faqs>
