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

package org.gradle.language.cpp.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.internal.file.FileCollectionInternal
import org.gradle.api.internal.file.TestFiles
import org.gradle.api.internal.provider.DefaultProviderFactory
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.TestUtil
import org.junit.Rule
import spock.lang.Specification

class DefaultCppLibraryTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    def fileOperations = TestFiles.fileOperations(tmpDir.testDirectory)
    def providerFactory = new DefaultProviderFactory()
    def api = Stub(TestConfiguration)
    def configurations = Stub(ConfigurationContainer)
    DefaultCppLibrary library

    def setup() {
        _ * configurations.create("api") >> api
        _ * configurations.create(_) >> Stub(TestConfiguration)
        library = new DefaultCppLibrary("main", TestUtil.objectFactory(), fileOperations, providerFactory, configurations)
    }

    def "has api configuration"() {
        expect:
        library.apiDependencies == api
    }

    def "has debug and release shared libraries"() {
        expect:
        library.debugSharedLibrary.name == "mainDebug"
        library.debugSharedLibrary.debuggable
        library.releaseSharedLibrary.name == "mainRelease"
        !library.releaseSharedLibrary.debuggable
        library.developmentBinary == library.debugSharedLibrary
    }

    def "uses convention for public headers when nothing specified"() {
        def d = tmpDir.file("src/main/public")

        expect:
        library.publicHeaderDirs.files == [d] as Set
    }

    def "does not include the convention for public headers when some other location specified"() {
        def d = tmpDir.file("other")

        expect:
        library.publicHeaders.from(d)
        library.publicHeaderDirs.files == [d] as Set
    }

    def "compile include path includes public and private header dirs"() {
        def defaultPrivate = tmpDir.file("src/main/headers")
        def defaultPublic = tmpDir.file("src/main/public")
        def d1 = tmpDir.file("src/main/d1")
        def d2 = tmpDir.file("src/main/d2")
        def d3 = tmpDir.file("src/main/d3")
        def d4 = tmpDir.file("src/main/d4")

        expect:
        library.debugSharedLibrary.compileIncludePath.files as List == [defaultPublic, defaultPrivate]
        library.releaseSharedLibrary.compileIncludePath.files as List == [defaultPublic, defaultPrivate]

        library.publicHeaders.from(d1)
        library.privateHeaders.from(d2)
        library.debugSharedLibrary.compileIncludePath.files as List == [d1, d2]
        library.releaseSharedLibrary.compileIncludePath.files as List == [d1, d2]

        library.publicHeaders.setFrom(d3)
        library.privateHeaders.from(d4)
        library.debugSharedLibrary.compileIncludePath.files as List == [d3, d2, d4]
        library.releaseSharedLibrary.compileIncludePath.files as List == [d3, d2, d4]
    }

    def "can query the header files of the library"() {
        def d1 = tmpDir.createDir("d1")
        def f1 = d1.createFile("a.h")
        def f2 = d1.createFile("nested/b.h")
        d1.createFile("ignore-me.cpp")
        def f3 = tmpDir.createFile("src/main/public/c.h")
        def f4 = tmpDir.createFile("src/main/headers/c.h")
        tmpDir.createFile("src/main/headers/ignore.cpp")
        tmpDir.createFile("src/main/public/ignore.cpp")

        expect:
        library.headerFiles.files == [f3, f4] as Set

        library.privateHeaders.from(d1)
        library.headerFiles.files == [f3, f1, f2] as Set
    }

    def "uses component name to determine header directories"() {
        def h1 = tmpDir.createFile("src/a/public")
        def h2 = tmpDir.createFile("src/b/public")
        def c1 = new DefaultCppLibrary("a", TestUtil.objectFactory(), fileOperations, providerFactory, configurations)
        def c2 = new DefaultCppLibrary("b", TestUtil.objectFactory(), fileOperations, providerFactory, configurations)

        expect:
        c1.publicHeaderDirs.files == [h1] as Set
        c2.publicHeaderDirs.files == [h2] as Set
    }

    interface TestConfiguration extends Configuration, FileCollectionInternal {
    }
}
