/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.ivyresolve

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.ComponentMetadataProcessor
import org.gradle.api.internal.artifacts.ImmutableModuleIdentifierFactory
import org.gradle.api.internal.artifacts.configurations.dynamicversion.CachePolicy
import org.gradle.api.internal.artifacts.ivyservice.dynamicversions.ModuleVersionsCache
import org.gradle.api.internal.artifacts.ivyservice.modulecache.ModuleArtifactsCache
import org.gradle.api.internal.artifacts.ivyservice.modulecache.ModuleMetaDataCache
import org.gradle.api.internal.component.ArtifactType
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetadata
import org.gradle.internal.component.external.model.ModuleComponentResolveMetadata
import org.gradle.internal.component.model.ComponentArtifactMetadata
import org.gradle.internal.component.model.ComponentArtifacts
import org.gradle.internal.component.model.ComponentOverrideMetadata
import org.gradle.internal.component.model.ComponentResolveMetadata
import org.gradle.internal.component.model.DependencyMetadata
import org.gradle.internal.component.model.ModuleSource
import org.gradle.internal.hash.HashCode
import org.gradle.internal.resolve.result.BuildableArtifactResolveResult
import org.gradle.internal.resolve.result.DefaultBuildableArtifactSetResolveResult
import org.gradle.internal.resolve.result.DefaultBuildableComponentArtifactsResolveResult
import org.gradle.internal.resolve.result.DefaultBuildableModuleComponentMetaDataResolveResult
import org.gradle.internal.resolve.result.DefaultBuildableModuleVersionListingResolveResult
import org.gradle.internal.resource.cached.CachedArtifactIndex
import org.gradle.internal.resource.cached.ivy.ArtifactAtRepositoryKey
import org.gradle.util.BuildCommencedTimeProvider
import spock.lang.Specification
import spock.lang.Unroll

class CachingModuleComponentRepositoryTest extends Specification {
    def realLocalAccess = Mock(ModuleComponentRepositoryAccess)
    def realRemoteAccess = Mock(ModuleComponentRepositoryAccess)
    def realRepo = Stub(ModuleComponentRepository) {
        getId() >> "repo-id"
        getLocalAccess() >> realLocalAccess
        getRemoteAccess() >> realRemoteAccess
    }
    def moduleResolutionCache = Stub(ModuleVersionsCache)
    def moduleDescriptorCache = Mock(ModuleMetaDataCache)
    def moduleArtifactsCache = Mock(ModuleArtifactsCache)
    def artifactAtRepositoryCache = Mock(CachedArtifactIndex)
    def cachePolicy = Stub(CachePolicy)
    def metadataProcessor = Stub(ComponentMetadataProcessor)
    def moduleIdentifierFactory = Mock(ImmutableModuleIdentifierFactory)
    def repo = new CachingModuleComponentRepository(realRepo, moduleResolutionCache, moduleDescriptorCache, moduleArtifactsCache, artifactAtRepositoryCache,
            cachePolicy, new BuildCommencedTimeProvider(), metadataProcessor, moduleIdentifierFactory)

    @Unroll
    def "artifact last modified date is cached - lastModified = #lastModified"() {
        given:
        def artifactId = Stub(ModuleComponentArtifactIdentifier)
        def artifact = Stub(ModuleComponentArtifactMetadata) {
            getId() >> artifactId
        }

        def file = new File("local")
        def result = Stub(BuildableArtifactResolveResult) {
            getFile() >> file
            getFailure() >> null
        }

        def descriptorHash = HashCode.fromInt(1234)
        def moduleSource = Stub(CachingModuleComponentRepository.CachingModuleSource) {
            getDescriptorHash() >> descriptorHash
        }

        ArtifactAtRepositoryKey atRepositoryKey = new ArtifactAtRepositoryKey("repo-id", artifactId)

        when:
        repo.remoteAccess.resolveArtifact(artifact, moduleSource, result)

        then:
        1 * artifactAtRepositoryCache.store(atRepositoryKey, file, descriptorHash)
        0 * moduleDescriptorCache._

        where:
        lastModified << [new Date(), null]
    }

    def "does not use cache when module version listing can be determined locally"() {
        def dependency = Mock(DependencyMetadata)
        def result = new DefaultBuildableModuleVersionListingResolveResult()

        when:
        repo.localAccess.listModuleVersions(dependency, result)

        then:
        realLocalAccess.listModuleVersions(dependency, result) >> {
            result.listed(['a', 'b', 'c'])
        }
        0 * _
    }

    def "does not use cache when component metadata can be determined locally"() {
        def componentId = Mock(ModuleComponentIdentifier)
        def prescribedMetaData = Mock(ComponentOverrideMetadata)
        def result = new DefaultBuildableModuleComponentMetaDataResolveResult()

        when:
        repo.localAccess.resolveComponentMetaData(componentId, prescribedMetaData, result)

        then:
        realLocalAccess.resolveComponentMetaData(componentId, prescribedMetaData, result) >> {
            result.resolved(Mock(ModuleComponentResolveMetadata))
        }
        0 * _
    }

    def "does not use cache when artifacts for type can be determined locally"() {
        def component = Mock(ComponentResolveMetadata)
        def source = Mock(ModuleSource)
        def cachingSource = new CachingModuleComponentRepository.CachingModuleSource(HashCode.fromInt(1), false, source)
        def artifactType = ArtifactType.JAVADOC
        def result = new DefaultBuildableArtifactSetResolveResult()

        when:
        repo.localAccess.resolveArtifactsWithType(component, artifactType, result)

        then:
        1 * component.getSource() >> cachingSource
        1 * component.withSource(source) >> component
        realLocalAccess.resolveArtifactsWithType(component, artifactType, result) >> {
            result.resolved([Mock(ComponentArtifactMetadata)])
        }
        0 * _
    }

    def "does not use cache when component artifacts can be determined locally"() {
        def component = Mock(ComponentResolveMetadata)
        def source = Mock(ModuleSource)
        def cachingSource = new CachingModuleComponentRepository.CachingModuleSource(HashCode.fromInt(1), false, source)
        def result = new DefaultBuildableComponentArtifactsResolveResult()

        when:
        repo.localAccess.resolveArtifacts(component, result)

        then:
        1 * component.getSource() >> cachingSource
        1 * component.withSource(source) >> component
        realLocalAccess.resolveArtifacts(component, result) >> {
            result.resolved(Stub(ComponentArtifacts))
        }
        0 * _
    }
}
