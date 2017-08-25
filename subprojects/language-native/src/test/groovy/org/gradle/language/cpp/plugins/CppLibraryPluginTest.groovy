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
import org.gradle.language.cpp.CppLibrary
import org.gradle.language.cpp.tasks.CppCompile
import org.gradle.nativeplatform.tasks.LinkSharedLibrary
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import spock.lang.Specification

class CppLibraryPluginTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    def projectDir = tmpDir.createDir("project")
    def project = ProjectBuilder.builder().withProjectDir(projectDir).withName("testLib").build()

    def "adds extension with convention for source layout and base name"() {
        given:
        def src = projectDir.file("src/main/cpp/main.cpp").createFile()
        def publicHeaders = projectDir.file("src/main/public").createDir()

        when:
        project.pluginManager.apply(CppLibraryPlugin)

        then:
        project.library instanceof CppLibrary
        project.library.baseName.get() == "testLib"
        project.library.cppSource.files == [src] as Set
        project.library.publicHeaderDirs.files == [publicHeaders] as Set
    }

    def "registers a component for the library"() {
        when:
        project.pluginManager.apply(CppLibraryPlugin)

        then:
        project.components.main == project.library
        project.components.mainDebug == project.library.debugSharedLibrary
        project.components.mainRelease == project.library.releaseSharedLibrary
    }

    def "adds compile and link tasks"() {
        given:
        def src = projectDir.file("src/main/cpp/lib.cpp").createFile()
        def publicHeaders = projectDir.file("src/main/public").createDir()
        def privateHeaders = projectDir.file("src/main/headers").createDir()

        when:
        project.pluginManager.apply(CppLibraryPlugin)

        then:
        def compileDebugCpp = project.tasks.compileDebugCpp
        compileDebugCpp instanceof CppCompile
        compileDebugCpp.includes.files as List == [publicHeaders, privateHeaders]
        compileDebugCpp.source.files as List == [src]
        compileDebugCpp.objectFileDirectory.get().asFile == projectDir.file("build/obj/main/debug")

        def linkDebug = project.tasks.linkDebug
        linkDebug instanceof LinkSharedLibrary
        linkDebug.binaryFile.get().asFile == projectDir.file("build/lib/main/debug/" + OperatingSystem.current().getSharedLibraryName("testLib"))

        def compileReleaseCpp = project.tasks.compileReleaseCpp
        compileReleaseCpp instanceof CppCompile
        compileReleaseCpp.includes.files as List == [publicHeaders, privateHeaders]
        compileReleaseCpp.source.files as List == [src]
        compileReleaseCpp.objectFileDirectory.get().asFile == projectDir.file("build/obj/main/release")

        def linkRelease = project.tasks.linkRelease
        linkRelease instanceof LinkSharedLibrary
        linkRelease.binaryFile.get().asFile == projectDir.file("build/lib/main/release/" + OperatingSystem.current().getSharedLibraryName("testLib"))
    }

    def "output locations are calculated using base name defined on extension"() {
        when:
        project.pluginManager.apply(CppLibraryPlugin)
        project.library.baseName = "test_lib"

        then:
        def link = project.tasks.linkDebug
        link.binaryFile.get().asFile == projectDir.file("build/lib/main/debug/" + OperatingSystem.current().getSharedLibraryName("test_lib"))
    }

    def "output locations reflects changes to buildDir"() {
        given:
        project.pluginManager.apply(CppLibraryPlugin)

        when:
        project.buildDir = "output"

        then:
        def compileCpp = project.tasks.compileDebugCpp
        compileCpp.objectFileDir == project.file("output/obj/main/debug")

        def link = project.tasks.linkDebug
        link.outputFile == projectDir.file("output/lib/main/debug/" + OperatingSystem.current().getSharedLibraryName("testLib"))
    }
}
