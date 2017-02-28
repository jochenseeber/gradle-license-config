/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.Task;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.Copy;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.Hidden;

import groovy.lang.GroovyObject;
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
            licenseExtension.setHeader(files.file("src/doc/templates/LICENSE.txt"));
            licenseExtension.exclude("**/*.json");

            Optional.ofNullable(projectConfig.getLicense().getExcludes()).ifPresent(excludes -> {
                for (String exclude : excludes) {
                    licenseExtension.exclude(exclude);
                }
            });

            ExtraPropertiesExtension properties = (ExtraPropertiesExtension) ((GroovyObject) licenseExtension)
                    .getProperty("ext");
            properties.set("year", LocalDate.now().getYear());
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
         * Create task to update license template file
         *
         * @param tasks Task container to create task
         * @param projectConfig Project configuration for general project data
         * @param licenseExtension License extension for license data
         * @param files Resolver for file names
         */
        @Mutate
        public void createLicenseTemplateUpdateTask(ModelMap<Task> tasks, ProjectConfig projectConfig,
                LicenseExtension licenseExtension, FileOperations files) {
            tasks.create("licenseTemplateUpdate", UpdateLicenseTask.class, t -> {
                t.setDescription("Download configured license into license template file.");
                t.setGroup("license");
                t.setLicenseFile(files.file("src/doc/templates/LICENSE.txt"));

                ConventionMapping parameters = t.getConventionMapping();
                parameters.map("copyrightName", () -> projectConfig.getOrganization().getName());
                parameters.map("copyrightYear", () -> getCopyrightYearTemplate(projectConfig));
                parameters.map("licenseUrl", () -> projectConfig.getLicense().getSourceUrl());
            });
        }

        /**
         * Create task to update license file
         *
         * @param tasks Task container to create task
         * @param projectConfig Project configuration for general project data
         * @param files Resolver for file names
         */
        @Mutate
        public void createLicenseUpdateTask(ModelMap<Task> tasks, ProjectConfig projectConfig, FileOperations files) {
            tasks.create("licenseUpdate", Copy.class, t -> {
                t.setDescription("Update license file from template.");
                t.setGroup("license");
                t.from(files.file("src/doc/templates/LICENSE.txt"));
                t.into(t.getProject().getProjectDir());

                Map<String, Object> properties = new HashMap<>();
                properties.put("year", LocalDate.now().getYear());
                t.expand(properties);
            });
        }

        /**
         * Configure the task dependencies
         *
         * @param tasks Assemble task to configure
         */
        @Mutate
        public void configureTaskDependencies(ModelMap<Task> tasks) {
            tasks.named("assemble", t -> {
                t.dependsOn("licenseUpdate");
            });
        }

        /**
         * Get the copyright years for the project
         *
         * @param projectConfig Project configuration
         * @return Copyright years
         */
        private static String getCopyrightYearTemplate(ProjectConfig projectConfig) {
            String year = "\\${year}";
            Integer inceptionYear = projectConfig.getInceptionYear();

            if (inceptionYear != null && inceptionYear != LocalDate.now().getYear()) {
                year = inceptionYear.toString() + "-" + year;
            }

            return year;
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
