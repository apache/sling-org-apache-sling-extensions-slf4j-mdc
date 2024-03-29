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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Filter is based on ch.qos.logback.classic.helpers.MDCInsertingServletFilter
 */
@Component(immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
            "pattern=/.*"
        })
@Designate(ocd = MDCInsertingFilter.Config.class)
public class MDCInsertingFilter implements Filter {
    public static final String REQUEST_REMOTE_HOST_MDC_KEY = "req.remoteHost";
    public static final String REQUEST_USER_AGENT_MDC_KEY = "req.userAgent";
    public static final String REQUEST_REQUEST_URI = "req.requestURI";
    public static final String REQUEST_QUERY_STRING = "req.queryString";
    public static final String REQUEST_REQUEST_URL = "req.requestURL";
    public static final String REQUEST_X_FORWARDED_FOR = "req.xForwardedFor";

    private static final String[] DEFAULT_KEY_NAMES = {
            REQUEST_REMOTE_HOST_MDC_KEY,
            REQUEST_USER_AGENT_MDC_KEY,
            REQUEST_REQUEST_URI,
            REQUEST_QUERY_STRING,
            REQUEST_REQUEST_URL,
            REQUEST_X_FORWARDED_FOR
    };

    private Set<String> keyNames = new CopyOnWriteArraySet<>();

    private Set<String> headerNames = new CopyOnWriteArraySet<>();

    private Set<String> parameterNames = new CopyOnWriteArraySet<>();

    private Set<String> cookieNames = new CopyOnWriteArraySet<>();

    private ServiceRegistration filterReg;

    @ObjectClassDefinition(name = "%mdc.label", description = "%mdc.description")
    public @interface Config {

        @AttributeDefinition
        String[] headers() default {};

        @AttributeDefinition
        String[] parameters() default {};

        @AttributeDefinition
        String[] cookies() default {};
    }


    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        insertIntoMDC(request);
        try {
            chain.doFilter(request, response);
        } finally {
            clearMDC();
        }
    }

    public void destroy() {

    }

    private void insertIntoMDC(ServletRequest request) {
        nullSafePut(REQUEST_REMOTE_HOST_MDC_KEY, request.getRemoteHost());

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            nullSafePut(REQUEST_REQUEST_URI, httpRequest.getRequestURI());

            StringBuffer requestURL = httpRequest.getRequestURL();
            if (requestURL != null) {
                nullSafePut(REQUEST_REQUEST_URL, requestURL.toString());
            }

            nullSafePut(REQUEST_QUERY_STRING, httpRequest.getQueryString());
            nullSafePut(REQUEST_USER_AGENT_MDC_KEY, httpRequest.getHeader("User-Agent"));
            nullSafePut(REQUEST_X_FORWARDED_FOR, httpRequest.getHeader("X-Forwarded-For"));

            for(String paramName : parameterNames){
                nullSafePut(paramName,httpRequest.getParameter(paramName));
            }

            for(String headerName :headerNames){
                nullSafePut(headerName, httpRequest.getHeader(headerName));
            }

            Cookie[] cookies = httpRequest.getCookies();
            if(cookies != null){
                for(Cookie c : cookies){
                    if(cookieNames.contains(c.getName())){
                        nullSafePut(c.getName(),c.getValue());
                    }
                }
            }
        }
    }

    private void clearMDC() {
        for (String key : keyNames) {
            MDC.remove(key);
        }
    }

    @Activate
    private void activate(BundleContext context, Config config) {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("filter.scope", "REQUEST");
        //The MDC Filter might be running in a non Sling container. Hence, to avoid
        //direct dependency on Sling we use a ServiceFactory
        filterReg = context.registerService(Filter.class.getName(),new ServiceFactory() {
            private Object instance;

            public synchronized Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
                if(instance == null){
                    instance = new SlingMDCFilter();
                }
                return instance;
            }

            public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o) {

            }
        }, properties);

        modified(config);
    }

    @Modified
    private void modified(Config config){
        Set<String> headers = new HashSet<>(Arrays.asList(config.headers()));
        headerNames.clear();
        headerNames.addAll(headers);

        Set<String> cookies = new HashSet<>(Arrays.asList(config.cookies()));
        cookieNames.clear();
        cookieNames.addAll(cookies);

        Set<String> params = new HashSet<>(Arrays.asList(config.parameters()));
        parameterNames.clear();
        parameterNames.addAll(params);

        List<String> keyList = new ArrayList<>();
        keyList.addAll(headerNames);
        keyList.addAll(cookieNames);
        keyList.addAll(parameterNames);
        keyList.addAll(Arrays.asList(DEFAULT_KEY_NAMES));

        this.keyNames.clear();
        this.keyNames.addAll(keyList);
    }

    @Deactivate
    private void deactivate(){
        if(filterReg != null){
            filterReg.unregister();
        }
    }

    private void nullSafePut(String key,String value){
        if(key != null && value != null){
            MDC.put(key,value);
        }
    }
}
