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

package org.gradle.internal.sources;

import org.gradle.api.initialization.sources.GitScmBuild;
import org.gradle.api.initialization.sources.ScmBuild;
import org.gradle.api.internal.file.delete.Deleter;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.Cast;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.util.GFileUtils;

import java.io.File;

public class DefaultScmLifecycleHandler implements ScmLifecycleHandler {
    private static final Logger LOGGER = Logging.getLogger(DefaultScmLifecycleHandler.class);

    // TODO:
    private final ExecActionFactory execActionFactory;
    private final Deleter deleter;

    public DefaultScmLifecycleHandler(ExecActionFactory execActionFactory, Deleter deleter) {
        this.execActionFactory = execActionFactory;
        this.deleter = deleter;
    }

    @Override
    public ScmCheckout initialize(File workingDirRoot, ScmBuild scmBuild) {
        // TODO: This should be inside a cache accessor so we don't clone on top of other processes
        GitScmBuild gitScmBuild = Cast.cast(GitScmBuild.class, scmBuild);
        File gitCache = new File(workingDirRoot, "gits");
        File destination = new File(gitCache, gitScmBuild.getName());
        if (destination.exists() && (!destination.isDirectory() || !new File(destination, ".git").exists())) {
            LOGGER.lifecycle("Deleting {}", destination);
            deleter.delete(destination);
        }

        ExecAction action = execActionFactory.newExecAction();
        action.setExecutable("git");

        if (destination.exists()) {
            LOGGER.lifecycle("pulling {}", destination);
            action.setWorkingDir(destination);
            action.args("pull", gitScmBuild.getUrl());
        } else {
            LOGGER.lifecycle("cloning {}", destination);
            if (!destination.mkdirs()) {
                throw new RuntimeException("Couldn't make dirs");
            }
            action.setWorkingDir(workingDirRoot);
            action.args("clone", gitScmBuild.getUrl(), destination);
        }

        LOGGER.lifecycle("executing {}", action.getCommandLine());
        ExecResult result = action.execute();
        result.assertNormalExitValue();
        File settingsFile = new File(destination, "settings.gradle");
        if (!settingsFile.exists()) {
            GFileUtils.touch(settingsFile);
        }
        return new ScmCheckout(destination);
    }
}
