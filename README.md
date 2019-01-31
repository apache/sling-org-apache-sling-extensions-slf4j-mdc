[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-org-apache-sling-extensions-slf4j-mdc/master)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-extensions-slf4j-mdc/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-org-apache-sling-extensions-slf4j-mdc/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-extensions-slf4j-mdc/job/master/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.extensions.slf4j.mdc/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.extensions.slf4j.mdc%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.extensions.slf4j.mdc.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.extensions.slf4j.mdc) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling SLF4J MDC Filter

This module is part of the [Apache Sling](https://sling.apache.org) project.

This filter exposes various request details as part of [MDC][1]. 

Currently it exposes following variables:

1. `req.remoteHost` - Request remote host
2. `req.userAgent` - User Agent Header
3. `req.requestURI` - Request URI
4. `req.queryString` - Query String from request
5. `req.requestURL` -
6. `req.xForwardedFor` -
7. `sling.userId` - UserID associated with the request. Obtained from ResourceResolver
8. `jcr.sessionId` - Session ID of the JCR Session associated with current request.

The filter also allow configuration to extract data from request cookie, header and parameters. Look for
configuration with name 'Apache Sling Logging MDC Inserting Filter' for details on specifying header, cookie,
param names.

[1] http://www.slf4j.org/manual.html#mdc
