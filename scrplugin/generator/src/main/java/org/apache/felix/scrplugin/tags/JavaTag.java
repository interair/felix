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
package org.apache.felix.scrplugin.tags;

import java.util.Map;

/**
 * <code>JavaTag.java</code>...
 *
 */
public interface JavaTag {

    /**
     * The name of the tag, e.g. scr.component etc.
     */
    String getName();

    /**
     * The name of the tag as used in the source code.
     * For javadoc tags this is like {@link #getName()}, for annoations this is different.
     */
    String getSourceName();

    String[] getParameters();

    String getNamedParameter(String arg0);

    String getSourceLocation();

    int getLineNumber();

    JavaClassDescription getJavaClassDescription();

    JavaField getField();

    Map<String, String> getNamedParameterMap();
}
