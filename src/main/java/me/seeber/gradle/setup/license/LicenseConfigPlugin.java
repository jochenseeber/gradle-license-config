/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.seeber.gradle.setup.license;

import java.util.Optional;

import org.gradle.api.Task;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.Hidden;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectConfig;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import nl.javadude.gradle.plugins.license.License;
import nl.javadude.gradle.plugins.license.LicenseExtension;
import nl.javadude.gradle.plugins.license.LicensePlugin;

/**
 * Project license configuration
 */
public class LicenseConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide the license extension
         *
         * @param extensions Extension container to look up extension
         * @return License extension
         */
        @Model
        @Hidden
        public LicenseExtension licenseExtension(ExtensionContainer extensions) {
            return extensions.getByType(LicenseExtension.class);
        }

        /**
         * Initialize license extension
         *
         * @param licenseExtension License extension to initialize
         * @param projectConfig Project configuration
         * @param files File operations object to resolve file names
         */
        @Defaults
        public void initializeLicenseExtension(LicenseExtension licenseExtension, ProjectConfig projectConfig,
                FileOperations files) {
            licenseExtension.setHeader(files.file("LICENSE.txt"));
            licenseExtension.exclude("**/*.json");

            Optional.ofNullable(projectConfig.getLicense().getExcludes()).ifPresent(excludes -> {
                for (String exclude : excludes) {
                    licenseExtension.exclude(exclude);
                }
            });
        }

        /**
         * Configure license tasks
         *
         * @param license License task to configure
         */
        @Mutate
        public void configureLicenseTask(@Each License license) {
            // This seems to be the only way...
            license.doFirst(t -> {
                license.setSource(
                        license.getSource().minus(t.getProject().fileTree(license.getProject().getBuildDir())));
            });
        }

        /**
         * Create task to update license file
         *
         * @param tasks Task container to create task
         * @param config Project configuration for general project data
         * @param licenseExtension License extension for license data
         */
        @Mutate
        public void createLicenseUpdateTask(ModelMap<Task> tasks, ProjectConfig config,
                LicenseExtension licenseExtension) {
            tasks.create("licenseUpdate", UpdateLicenseTask.class, t -> {
                t.setDescription("Download configured license into license file.");
                t.setGroup("license");

                ConventionMapping parameters = t.getConventionMapping();
                parameters.map("copyrightName", () -> config.getOrganization().getName());
                parameters.map("copyrightYear", () -> config.getInceptionYear());
                parameters.map("licenseUrl", () -> config.getLicense().getSourceUrl());
                parameters.map("licenseFile", () -> licenseExtension.getHeader());
            });
        }
    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    public void initialize() {
        getProject().getPluginManager().apply(ProjectConfigPlugin.class);
        getProject().getPluginManager().apply(LicensePlugin.class);
    }

}
