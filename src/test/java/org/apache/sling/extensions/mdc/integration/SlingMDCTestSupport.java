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
package org.apache.sling.extensions.mdc.integration;

import org.apache.sling.extensions.mdc.integration.servlet.MDCStateServlet;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.inject.Inject;

import static org.apache.sling.testing.paxexam.SlingOptions.sling;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public abstract class SlingMDCTestSupport extends TestSupport {

    @Inject
    protected BundleContext bundleContext;

    protected ModifiableCompositeOption baseConfiguration() {
        return composite(
                super.baseConfiguration(),
                slingQuickstart(),
                // Sling slf4j mdc bundle
                testBundle("bundle.filename"),
                junitBundles(),
                mavenBundle("org.apache.sling", "org.apache.sling.testing.tools").versionAsInProject(),
                // slf4j mdc test bundle
                mdcTestBundle()
        );
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory();
        return composite(
                slingQuickstartOakTar(workingDirectory, findFreePort()),
                sling()
        );
    }

    protected Option mdcTestBundle() {
        TinyBundle testBundle = bundle()
                .add(MDCStateServlet.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.sling.extensions.slf4j.mdc.testbundle")
                .set(Constants.BUNDLE_ACTIVATOR, MDCStateServlet.class.getName());
        return provision(testBundle.build(withBnd()));
    }
}
