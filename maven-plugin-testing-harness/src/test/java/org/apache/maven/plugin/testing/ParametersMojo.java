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

import org.apache.maven.plugin.AbstractMojo;

public class ParametersMojo extends AbstractMojo {

    private String plain;

    private String withProperty;

    private String withDefault;

    private String withPropertyAndDefault;

    @Override
    public void execute() {
        getLog().info("Plain value = " + plain);
    }

    public String getPlain() {
        return plain;
    }

    public void setPlain(String plain) {
        this.plain = plain;
    }

    public String getWithProperty() {
        return withProperty;
    }

    public void setWithProperty(String withProperty) {
        this.withProperty = withProperty;
    }

    public String getWithDefault() {
        return withDefault;
    }

    public void setWithDefault(String withDefault) {
        this.withDefault = withDefault;
    }

    public String getWithPropertyAndDefault() {
        return withPropertyAndDefault;
    }

    public void setWithPropertyAndDefault(String withPropertyAndDefault) {
        this.withPropertyAndDefault = withPropertyAndDefault;
    }
}
