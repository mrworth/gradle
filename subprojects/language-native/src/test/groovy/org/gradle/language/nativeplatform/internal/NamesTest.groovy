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

package org.gradle.language.nativeplatform.internal

import spock.lang.Specification

class NamesTest extends Specification {
    def "names for main"() {
        expect:
        def name = Names.of("main")
        name.getCompileTaskName("cpp") == "compileCpp"
        name.getTaskName("link") == "link"
        name.getDirName() == "main/"
        name.withPrefix("compile") == "compile"
        name.withSuffix("implementation") == "implementation"
    }

    def "names for variants of main"() {
        expect:
        def name = Names.of("mainDebug")
        name.getCompileTaskName("cpp") == "compileDebugCpp"
        name.getTaskName("link") == "linkDebug"
        name.getDirName() == "main/debug/"
        name.withPrefix("compile") == "compileDebug"
        name.withSuffix("implementation") == "debugImplementation"
    }

    def "names for main variant of main"() {
        expect:
        def name = Names.of("mainExe")
        name.getCompileTaskName("cpp") == "compileCpp"
        name.getTaskName("link") == "link"
        name.getDirName() == "main/"
        name.withPrefix("compile") == "compile"
        name.withSuffix("implementation") == "implementation"
    }

    def "names for custom"() {
        expect:
        def name = Names.of("custom")
        name.getCompileTaskName("cpp") == "compileCustomCpp"
        name.getTaskName("link") == "linkCustom"
        name.getDirName() == "custom/"
        name.withPrefix("compile") == "compileCustom"
        name.withSuffix("implementation") == "customImplementation"
    }

    def "names for variants of custom"() {
        expect:
        def name = Names.of("customRelease")
        name.getCompileTaskName("cpp") == "compileCustomReleaseCpp"
        name.getTaskName("link") == "linkCustomRelease"
        name.getDirName() == "custom/release/"
        name.withPrefix("compile") == "compileCustomRelease"
        name.withSuffix("implementation") == "customReleaseImplementation"
    }

    def "names for main variants of custom"() {
        expect:
        def name = Names.of("customExe")
        name.getCompileTaskName("cpp") == "compileCustomCpp"
        name.getTaskName("link") == "linkCustom"
        name.getDirName() == "custom/"
        name.withPrefix("compile") == "compileCustom"
        name.withSuffix("implementation") == "customImplementation"
    }
}
