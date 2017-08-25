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

package org.gradle.language.cpp.plugins

import org.gradle.internal.os.OperatingSystem
import org.gradle.language.cpp.CppBinary
import org.gradle.language.cpp.CppExecutable
import org.gradle.language.cpp.CppSharedLibrary
import org.gradle.language.cpp.tasks.CppCompile
import org.gradle.nativeplatform.tasks.LinkExecutable
import org.gradle.nativeplatform.tasks.LinkSharedLibrary
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import spock.lang.Specification

class CppBasePluginTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    def projectDir = tmpDir.createDir("project")
    def project = ProjectBuilder.builder().withProjectDir(projectDir).withName("test").build()

    def "adds compile task for binary"() {
        def binary = Stub(CppBinary)
        binary.name >> name

        when:
        project.pluginManager.apply(CppBasePlugin)
        project.components.add(binary)

        then:
        def compileCpp = project.tasks[taskName]
        compileCpp instanceof CppCompile
        compileCpp.objectFileDirectory.get().asFile == projectDir.file("build/obj/${objDir}")

        where:
        name        | taskName              | objDir
        "main"      | "compileCpp"          | "main"
        "mainDebug" | "compileDebugCpp"     | "main/debug"
        "test"      | "compileTestCpp"      | "test"
        "testDebug" | "compileTestDebugCpp" | "test/debug"
    }

    def "adds link task for executable"() {
        def baseName = project.providers.property(String)
        baseName.set("test_app")
        def executable = Stub(CppExecutable)
        executable.name >> name
        executable.baseName >> baseName

        when:
        project.pluginManager.apply(CppBasePlugin)
        project.components.add(executable)

        then:
        def link = project.tasks[taskName]
        link instanceof LinkExecutable
        link.binaryFile.get().asFile == projectDir.file("build/exe/$exeDir" + OperatingSystem.current().getExecutableName("test_app"))

        where:
        name        | taskName        | exeDir
        "main"      | "link"          | "main/"
        "mainDebug" | "linkDebug"     | "main/debug/"
        "test"      | "linkTest"      | "test/"
        "testDebug" | "linkTestDebug" | "test/debug/"
    }

    def "adds link task for shared library"() {
        def baseName = project.providers.property(String)
        baseName.set("test_lib")
        def library = Stub(CppSharedLibrary)
        library.name >> name
        library.baseName >> baseName

        when:
        project.pluginManager.apply(CppBasePlugin)
        project.components.add(library)

        then:
        def link = project.tasks[taskName]
        link instanceof LinkSharedLibrary
        link.binaryFile.get().asFile == projectDir.file("build/lib/${libDir}" + OperatingSystem.current().getSharedLibraryName("test_lib"))

        where:
        name        | taskName        | libDir
        "main"      | "link"          | "main/"
        "mainDebug" | "linkDebug"     | "main/debug/"
        "test"      | "linkTest"      | "test/"
        "testDebug" | "linkTestDebug" | "test/debug/"
    }
}
