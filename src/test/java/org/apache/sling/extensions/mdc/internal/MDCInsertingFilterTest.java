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
package org.apache.sling.extensions.mdc.internal;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class MDCInsertingFilterTest {

    @Rule
    public final OsgiContext context = new OsgiContext();
    private MDCInsertingFilter mdcInsertingFilter;

    @Before
    public void setUp() throws Exception {
        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put("headers", new String[]{"mdc-test-header"});
        properties.put("parameters", new String[]{"mdc-test-param"});
        properties.put("cookies", new String[]{"mdc-test-cookie"});

        mdcInsertingFilter = context.registerInjectActivateService(MDCInsertingFilter.class, new MDCInsertingFilter(), properties);
    }

    @Test
    public void testIfServiceActive() {
        assertNotNull(mdcInsertingFilter);
    }
}