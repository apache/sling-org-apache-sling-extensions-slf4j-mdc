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

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.ops4j.pax.exam.CoreOptions.options;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingMDCFilterTestIT extends SlingMDCTestSupport {

    private static Logger log = LoggerFactory.getLogger(SlingMDCFilterTestIT.class);

    private DefaultHttpClient httpClient = new DefaultHttpClient();
    private RequestExecutor executor = new RequestExecutor(httpClient);
    private String localHostUrl;

    @Before
    public void setUp() throws IOException, InterruptedException {
        localHostUrl = String.format("http://localhost:%s", httpPort());

        new RetryLoop(new RetryLoop.Condition() {
            public String getDescription() {
                return "Check if MDCTestServlet is up";
            }

            public boolean isTrue() throws Exception {
                RequestBuilder requestBuilder = new RequestBuilder(localHostUrl);
                executor.execute(requestBuilder.buildGetRequest("/mdc")).assertStatus(200);
                requestBuilder = new RequestBuilder(localHostUrl);

                //Create test config via servlet
                executor.execute(requestBuilder.buildGetRequest("/mdc", "createTestConfig", "true"));
                TimeUnit.SECONDS.sleep(1);
                return true;
            }
        }, 5, 100);

    }

    @Configuration
    public Option[] configuration() throws IOException {
        return options(
                baseConfiguration()
        );
    }

    @Test
    public void testDefault() throws Exception {
        RequestBuilder rb = new RequestBuilder(localHostUrl);
        // Add Sling POST options
        RequestExecutor result = executor.execute(rb.buildGetRequest("/mdc", "foo", "bar"));
        JsonObject jsonObject = Json.createReader(new StringReader(result.getContent())).readObject();

        assertEquals("/mdc", jsonObject.getString("req.requestURI"));
        assertEquals("foo=bar", jsonObject.getString("req.queryString"));
        assertEquals(localHostUrl + "/mdc", jsonObject.getString("req.requestURL"));
        log.info("Response  {}", result.getContent());
    }

    @Test
    public void testWihCustomData() throws Exception {
        RequestBuilder rb = new RequestBuilder(localHostUrl);

        //Create test config via servlet
        executor.execute(rb.buildGetRequest("/mdc", "createTestConfig", "true"));
        TimeUnit.SECONDS.sleep(1);

        //Pass custom cookie
        BasicClientCookie cookie = new BasicClientCookie("mdc-test-cookie", "foo-test-cookie");
        cookie.setPath("/");
        cookie.setDomain("localhost");
        httpClient.getCookieStore().addCookie(cookie);

        //Execute request
        RequestExecutor result = executor.execute(
                rb.buildGetRequest("/mdc", "mdc-test-param", "foo-test-param", "ignored-param", "ignored-value")
                        .withHeader("X-Forwarded-For", "foo-forwarded-for")
                        .withHeader("mdc-test-header", "foo-test-header")
        );

        JsonObject jb = Json.createReader(new StringReader(result.getContent())).readObject();
        log.info("Response  {}", result.getContent());

        assertEquals("/mdc", jb.getString("req.requestURI"));
        assertEquals(localHostUrl + "/mdc", jb.getString("req.requestURL"));
        assertEquals("foo-forwarded-for", jb.getString("req.xForwardedFor"));
        assertEquals("foo-test-header", jb.getString("mdc-test-header"));
        assertEquals("foo-test-param", jb.getString("mdc-test-param"));
        assertEquals("foo-test-cookie", jb.getString("mdc-test-cookie"));

        //Only configured params must be returned
        assertFalse(jb.containsKey("ignored-param"));
    }
}
