/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.util.Requires

@Requires(adhoc = { AvailableJavaHomes.getJdks("1.7") })
class DeprecatedBuildIntegrationTest extends AbstractIntegrationSpec {
    def "warns of deprecated java version when running under java 7"() {
        def jdk = AvailableJavaHomes.jdk7

        given:
        executer.withJavaHome(jdk.javaHome)

        expect:
        run("help")
        result.output.count("Support for running Gradle using Java 7 has been deprecated and is scheduled to be removed in Gradle 5.0") == 1
    }

    def "warns of deprecate java version when build is configured to use java 7"() {
        given:
        file("gradle.properties").writeProperties("org.gradle.java.home": AvailableJavaHomes.jdk7.javaHome.canonicalPath)

        expect:
        run("help")
        result.output.count("Support for running Gradle using Java 7 has been deprecated and is scheduled to be removed in Gradle 5.0") == 1
    }
}
