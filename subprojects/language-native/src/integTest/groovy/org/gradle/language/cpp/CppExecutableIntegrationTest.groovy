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

package org.gradle.language.cpp

import org.gradle.nativeplatform.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativeplatform.fixtures.app.CppApp
import org.gradle.nativeplatform.fixtures.app.CppAppWithLibraries
import org.gradle.nativeplatform.fixtures.app.CppCompilerDetectingTestApp
import org.gradle.nativeplatform.fixtures.app.CppHelloWorldApp
import org.junit.Assume

import static org.gradle.util.Matchers.containsText

class CppExecutableIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {
    def setup() {
        // TODO - currently the customizations to the tool chains are ignored by the plugins, so skip these tests until this is fixed
        Assume.assumeTrue(toolChain.id != "mingw" && toolChain.id != "gcccygwin")
    }

    def "build fails when compilation fails"() {
        given:
        buildFile << """
            apply plugin: 'cpp-executable'
         """

        and:
        file("src/main/cpp/broken.cpp") << """
        #include <iostream>

        'broken
"""

        expect:
        fails "assemble"
        failure.assertHasDescription("Execution failed for task ':compileDebugCpp'.");
        failure.assertHasCause("A build operation failed.")
        failure.assertThatCause(containsText("C++ compiler failed while compiling broken.cpp"))
    }

    def "sources are compiled and linked with with C++ tools"() {
        settingsFile << "rootProject.name = 'app'"
        def app = new CppCompilerDetectingTestApp()

        given:
        app.writeSources(file('src/main'))

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")

        executable("build/exe/main/debug/app").assertExists()
        installation("build/install/app").exec().out == app.expectedOutput(AbstractInstalledToolChainIntegrationSpec.toolChain)
    }

    def "can build release variant of executable"() {
        settingsFile << "rootProject.name = 'app'"
        def app = new CppApp()

        given:
        app.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
         """

        expect:
        succeeds "linkRelease"
        result.assertTasksExecuted(":compileReleaseCpp", ":linkRelease")

        executable("build/exe/main/release/app").assertExists()
        executable("build/exe/main/release/app").exec().out == app.expectedOutput
    }

    def "ignores non-C++ source files in source directory"() {
        settingsFile << "rootProject.name = 'app'"
        def app = new CppApp()

        given:
        app.writeToProject(testDirectory)
        file("src/main/cpp/ignore.swift") << 'broken!'
        file("src/main/cpp/ignore.c") << 'broken!'
        file("src/main/cpp/ignore.m") << 'broken!'
        file("src/main/cpp/ignore.h") << 'broken!'
        file("src/main/cpp/ignore.java") << 'broken!'

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")

        executable("build/exe/main/debug/app").assertExists()
        installation("build/install/app").exec().out == app.expectedOutput
    }

    def "build logic can change source layout convention"() {
        settingsFile << "rootProject.name = 'app'"
        def app = new CppApp()

        given:
        app.sources.writeToSourceDir(file("srcs"))
        app.headers.writeToSourceDir(file("include"))
        file("src/main/headers/${app.greeter.header.sourceFile.name}") << 'broken!'
        file("src/main/cpp/broken.cpp") << "ignore me!"

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
            executable {
                source.from 'srcs'
                privateHeaders.from 'include'
            }
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")

        file("build/obj/main/debug").assertIsDir()
        executable("build/exe/main/debug/app").assertExists()
        installation("build/install/app").exec().out == app.expectedOutput
    }

    def "build logic can add individual source files"() {
        settingsFile << "rootProject.name = 'app'"
        def app = new CppApp()

        given:
        app.headers.writeToProject(testDirectory)
        app.main.writeToSourceDir(file("srcs/main.cpp"))
        app.greeter.writeToSourceDir(file("srcs/one.cpp"))
        app.sum.writeToSourceDir(file("srcs/two.cpp"))
        file("src/main/cpp/broken.cpp") << "ignore me!"

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
            executable {
                source {
                    from('srcs/main.cpp')
                    from('srcs/one.cpp')
                    from('srcs/two.cpp')
                }
            }
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")

        file("build/obj/main/debug").assertIsDir()
        executable("build/exe/main/debug/app").assertExists()
        installation("build/install/app").exec().out == app.expectedOutput
    }

    def "build logic can change buildDir"() {
        settingsFile << "rootProject.name = 'app'"
        def app = new CppApp()

        given:
        app.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
            buildDir = 'output'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")

        !file("build").exists()
        file("output/obj/main/debug").assertIsDir()
        executable("output/exe/main/debug/app").assertExists()
        installation("output/install/app").exec().out == app.expectedOutput
    }

    def "build logic can define the base name"() {
        def app = new CppApp()

        given:
        app.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
            executable.baseName = 'test_app'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")

        file("build/obj/main/debug").assertIsDir()
        executable("build/exe/main/debug/test_app").assertExists()
        installation("build/install/test_app").exec().out == app.expectedOutput
    }

    def "build logic can change task output locations"() {
        settingsFile << "rootProject.name = 'app'"
        def app = new CppApp()

        given:
        app.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-executable'
            compileDebugCpp.objectFileDirectory = layout.buildDirectory.dir("object-files")
            linkDebug.binaryFile = layout.buildDirectory.file("exe/some-app.exe")
            installMain.installDirectory = layout.buildDirectory.dir("some-app")
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")

        file("build/object-files").assertIsDir()
        file("build/exe/some-app.exe").assertIsFile()
        installation("build/some-app").exec().out == app.expectedOutput
    }

    def "can compile and link against a library"() {
        settingsFile << "include 'app', 'hello'"
        def app = new CppHelloWorldApp()

        given:
        buildFile << """
            project(':app') {
                apply plugin: 'cpp-executable'
                dependencies {
                    implementation project(':hello')
                }
            }
            project(':hello') {
                apply plugin: 'cpp-library'
            }
"""
        app.library.headerFiles.each { it.writeToFile(file("hello/src/main/public/$it.name")) }
        app.library.sourceFiles.each { it.writeToFile(file("hello/src/main/cpp/$it.name")) }
        app.executable.sourceFiles.each { it.writeToDir(file('app/src/main')) }

        expect:
        succeeds ":app:assemble"
        result.assertTasksExecuted(":hello:compileDebugCpp", ":hello:linkDebug", ":app:compileDebugCpp", ":app:linkDebug", ":app:installMain", ":app:assemble")
        executable("app/build/exe/main/debug/app").assertExists()
        sharedLibrary("hello/build/lib/main/debug/hello").assertExists()
        installation("app/build/install/app").exec().out == app.englishOutput
        sharedLibrary("app/build/install/app/lib/hello").file.assertExists()
    }

    def "can compile and link against library with dependencies"() {
        settingsFile << "include 'app', 'lib1', 'lib2'"
        def app = new CppAppWithLibraries()

        given:
        buildFile << """
            project(':app') {
                apply plugin: 'cpp-executable'
                dependencies {
                    implementation project(':lib1')
                }
            }
            project(':lib1') {
                apply plugin: 'cpp-library'
                dependencies {
                    implementation project(':lib2')
                }
            }
            project(':lib2') {
                apply plugin: 'cpp-library'
            }
"""
        app.greeterLib.writeToProject(file("lib1"))
        app.loggerLib.writeToProject(file("lib2"))
        app.main.writeToProject(file("app"))

        expect:
        succeeds ":app:assemble"

        result.assertTasksExecuted(":lib1:compileDebugCpp", ":lib1:linkDebug", ":lib2:compileDebugCpp", ":lib2:linkDebug", ":app:compileDebugCpp", ":app:linkDebug", ":app:installMain", ":app:assemble")
        sharedLibrary("lib1/build/lib/main/debug/lib1").assertExists()
        sharedLibrary("lib2/build/lib/main/debug/lib2").assertExists()
        executable("app/build/exe/main/debug/app").assertExists()
        installation("app/build/install/app").exec().out == app.expectedOutput
        sharedLibrary("app/build/install/app/lib/lib1").file.assertExists()
        sharedLibrary("app/build/install/app/lib/lib2").file.assertExists()

        succeeds(":app:linkRelease")

        result.assertTasksExecuted(":lib1:compileReleaseCpp", ":lib1:linkRelease", ":lib2:compileReleaseCpp", ":lib2:linkRelease", ":app:compileReleaseCpp", ":app:linkRelease")
        sharedLibrary("lib1/build/lib/main/release/lib1").assertExists()
        sharedLibrary("lib2/build/lib/main/release/lib2").assertExists()
        executable("app/build/exe/main/release/app").assertExists()
    }

    def "honors changes to library buildDir"() {
        settingsFile << "include 'app', 'lib1', 'lib2'"
        def app = new CppAppWithLibraries()

        given:
        buildFile << """
            project(':app') {
                apply plugin: 'cpp-executable'
                dependencies {
                    implementation project(':lib1')
                }
            }
            project(':lib1') {
                apply plugin: 'cpp-library'
                dependencies {
                    implementation project(':lib2')
                }
            }
            project(':lib2') {
                apply plugin: 'cpp-library'
                buildDir = 'out'
            }
"""
        app.greeterLib.writeToProject(file("lib1"))
        app.loggerLib.writeToProject(file("lib2"))
        app.main.writeToProject(file("app"))

        expect:
        succeeds ":app:assemble"
        result.assertTasksExecuted(":lib1:compileDebugCpp", ":lib1:linkDebug", ":lib2:compileDebugCpp", ":lib2:linkDebug", ":app:compileDebugCpp", ":app:linkDebug", ":app:installMain", ":app:assemble")

        !file("lib2/build").exists()
        sharedLibrary("lib1/build/lib/main/debug/lib1").assertExists()
        sharedLibrary("lib2/out/lib/main/debug/lib2").assertExists()
        executable("app/build/exe/main/debug/app").assertExists()
        installation("app/build/install/app").exec().out == app.expectedOutput
        sharedLibrary("app/build/install/app/lib/lib1").file.assertExists()
        sharedLibrary("app/build/install/app/lib/lib2").file.assertExists()
    }

    def "honors changes to library public header location"() {
        settingsFile << "include 'app', 'lib1', 'lib2'"
        def app = new CppAppWithLibraries()

        given:
        buildFile << """
            project(':app') {
                apply plugin: 'cpp-executable'
                dependencies {
                    implementation project(':lib1')
                }
            }
            project(':lib1') {
                apply plugin: 'cpp-library'
                dependencies {
                    implementation project(':lib2')
                }
                library {
                    publicHeaders.from('include')
                }
            }
            project(':lib2') {
                apply plugin: 'cpp-library'
                library {
                    publicHeaders.from('include')
                }
            }
"""
        app.greeterLib.publicHeaders.writeToSourceDir(file("lib1/include"))
        app.greeterLib.privateHeaders.writeToProject(file("lib1"))
        app.greeterLib.sources.writeToProject(file("lib1"))
        app.loggerLib.publicHeaders.writeToSourceDir(file("lib2/include"))
        app.loggerLib.sources.writeToProject(file("lib2"))
        app.main.writeToProject(file("app"))

        expect:
        succeeds ":app:assemble"
        result.assertTasksExecuted(":lib1:compileDebugCpp", ":lib1:linkDebug", ":lib2:compileDebugCpp", ":lib2:linkDebug", ":app:compileDebugCpp", ":app:linkDebug", ":app:installMain", ":app:assemble")

        sharedLibrary("lib1/build/lib/main/debug/lib1").assertExists()
        sharedLibrary("lib2/build/lib/main/debug/lib2").assertExists()
        executable("app/build/exe/main/debug/app").assertExists()
        installation("app/build/install/app").exec().out == app.expectedOutput
        sharedLibrary("app/build/install/app/lib/lib1").file.assertExists()
        sharedLibrary("app/build/install/app/lib/lib2").file.assertExists()
    }

    def "multiple components can share the same source directory"() {
        settingsFile << "include 'app', 'greeter', 'logger'"
        def app = new CppAppWithLibraries()

        given:
        buildFile << """
            project(':app') {
                apply plugin: 'cpp-executable'
                dependencies {
                    implementation project(':greeter')
                }
                executable {
                    source.from '../Sources/main.cpp'
                }
            }
            project(':greeter') {
                apply plugin: 'cpp-library'
                dependencies {
                    implementation project(':logger')
                }
                library {
                    source.from '../Sources/greeter.cpp'
                }
            }
            project(':logger') {
                apply plugin: 'cpp-library'
                library {
                    source.from '../Sources/logger.cpp'
                }
            }
"""
        app.main.writeToSourceDir(file("Sources"))
        app.greeterLib.sources.writeToSourceDir(file("Sources"))
        app.greeterLib.headers.writeToProject(file("greeter"))
        app.loggerLib.sources.writeToSourceDir(file("Sources"))
        app.loggerLib.headers.writeToProject(file("logger"))

        expect:
        succeeds ":app:assemble"
        result.assertTasksExecuted(":greeter:compileDebugCpp", ":greeter:linkDebug", ":logger:compileDebugCpp", ":logger:linkDebug", ":app:compileDebugCpp", ":app:linkDebug", ":app:installMain", ":app:assemble")

        sharedLibrary("greeter/build/lib/main/debug/greeter").assertExists()
        sharedLibrary("logger/build/lib/main/debug/logger").assertExists()
        executable("app/build/exe/main/debug/app").assertExists()
        installation("app/build/install/app").exec().out == app.expectedOutput
        sharedLibrary("app/build/install/app/lib/greeter").file.assertExists()
        sharedLibrary("app/build/install/app/lib/logger").file.assertExists()
    }

    def "can compile and link against libraries in included builds"() {
        settingsFile << """
            rootProject.name = 'app'
            includeBuild 'lib1'
            includeBuild 'lib2'
        """
        file("lib1/settings.gradle") << "rootProject.name = 'lib1'"
        file("lib2/settings.gradle") << "rootProject.name = 'lib2'"

        def app = new CppAppWithLibraries()

        given:
        buildFile << """
            apply plugin: 'cpp-executable'
            dependencies {
                implementation 'test:lib1:1.2'
            }
        """
        file("lib1/build.gradle") << """
            apply plugin: 'cpp-library'
            group = 'test'
            dependencies {
                implementation 'test:lib2:1.4'
            }
        """
        file("lib2/build.gradle") << """
            apply plugin: 'cpp-library'
            group = 'test'
        """

        app.greeterLib.writeToProject(file("lib1"))
        app.loggerLib.writeToProject(file("lib2"))
        app.main.writeToProject(testDirectory)

        expect:
        succeeds ":assemble"
        result.assertTasksExecuted(":lib1:compileDebugCpp", ":lib1:linkDebug",  ":lib2:compileDebugCpp", ":lib2:linkDebug", ":compileDebugCpp", ":linkDebug", ":installMain", ":assemble")
        sharedLibrary("lib1/build/lib/main/debug/lib1").assertExists()
        sharedLibrary("lib2/build/lib/main/debug/lib2").assertExists()
        executable("build/exe/main/debug/app").assertExists()
        installation("build/install/app").exec().out == app.expectedOutput
        sharedLibrary("build/install/app/lib/lib1").file.assertExists()
        sharedLibrary("build/install/app/lib/lib2").file.assertExists()
    }
}
