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

package org.gradle.api.initialization.sources.internal;

import com.google.common.collect.Lists;
import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencySubstitutions;
import org.gradle.api.initialization.sources.ScmBuild;

import java.util.Collection;
import java.util.List;

abstract class AbstractScmBuild implements ScmBuild {
    private final String name;
    private final List<Action<? super DependencySubstitutions>> dependencySubstitutionActions = Lists.newArrayList();

    protected AbstractScmBuild(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void dependencySubstitution(Action<? super DependencySubstitutions> action) {
        dependencySubstitutionActions.add(action);
    }

    @Override
    public Collection<Action<? super DependencySubstitutions>> getSubstitutions() {
        return dependencySubstitutionActions;
    }
}
