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


package org.gradle.integtests.tooling

import org.apache.commons.lang3.StringUtils
import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.TestExecutionException
import org.gradle.tooling.model.GradleProject
import spock.lang.IgnoreIf

@IgnoreIf({ AvailableJavaHomes.jdk7 == null })
@TargetGradleVersion("current")
class ToolingApiDeprecatedBuildJvmCrossVersionSpec extends ToolingApiSpecification {
    def setup() {
        projectDir.file("gradle.properties").writeProperties("org.gradle.java.home": AvailableJavaHomes.jdk7.javaHome.absolutePath)
        toolingApi.requireDaemons()
    }

    def "warning running a build when build is configured to use Java 7"() {
        def output = new ByteArrayOutputStream()

        when:
        toolingApi.withConnection { ProjectConnection connection ->
            def build = connection.newBuild()
            build.standardOutput = output
            build.run()
        }

        then:
        StringUtils.countMatches(output.toString(), "Support for running Gradle using Java 7 has been deprecated and is scheduled to be removed in Gradle 5.0") == 1
    }

    def "warning fetching model when build is configured to use Java 7"() {
        def output = new ByteArrayOutputStream()

        when:
        toolingApi.withConnection { ProjectConnection connection ->
            def model = connection.model(GradleProject)
            model.standardOutput = output
            model.get()
        }

        then:
        StringUtils.countMatches(output.toString(), "Support for running Gradle using Java 7 has been deprecated and is scheduled to be removed in Gradle 5.0") == 1
    }

    @ToolingApiVersion(">=1.8")
    def "warning running action when build is configured to use Java 7"() {
        def output = new ByteArrayOutputStream()

        when:
        toolingApi.withConnection { ProjectConnection connection ->
            def action = connection.action(new NullAction())
            action.standardOutput = output
            action.run()
        }

        then:
        StringUtils.countMatches(output.toString(), "Support for running Gradle using Java 7 has been deprecated and is scheduled to be removed in Gradle 5.0") == 1
    }

    @ToolingApiVersion(">=2.6")
    def "warning running tests when build is configured to use Java 7"() {
        def output = new ByteArrayOutputStream()

        when:
        toolingApi.withConnection { ProjectConnection connection ->
            def launcher = connection.newTestLauncher().withJvmTestClasses("SomeTest")
            launcher.standardOutput = output
            launcher.run()
        }

        then:
        TestExecutionException e = thrown()
        e.cause.message.startsWith("No matching tests found in any candidate test task.")
        StringUtils.countMatches(output.toString(), "Support for running Gradle using Java 7 has been deprecated and is scheduled to be removed in Gradle 5.0") == 1
    }
}
