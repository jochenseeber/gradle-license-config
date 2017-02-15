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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import me.seeber.gradle.util.Validate;

/**
 * Task to update the license file
 */
public class UpdateLicenseTask extends ConventionTask {

    /**
     * URL to download license from
     */
    @Input
    private @Nullable String licenseUrl;

    /**
     * Year for copyright information
     */
    @Input
    private int copyrightYear;

    /**
     * Name for copyright information
     */
    @Input
    private @Nullable String copyrightName;

    /**
     * Line length for license formatting
     */
    @Input
    private int lineLength = 76;

    /**
     * File to write license to
     */
    @OutputFile
    private @Nullable File licenseFile;

    /**
     * Download license
     */
    @TaskAction
    public void updateLicense() {
        String url = Validate.notNull(getLicenseUrl(), "The license URL must be set");
        File licenseFile = Validate.notNull(getLicenseFile(), "The license file must be set");

        try {
            String licenseText = Validate.notNull(Resources.toString(URI.create(url).toURL(), Charsets.UTF_8));
            licenseText = licenseText.replaceFirst("(?s)^.*\\R---\\R+", "");
            licenseText = replaceVariables(licenseText);
            licenseText = licenseText.replaceAll("(?m)^[ \\t]+", "");

            Files.write(licenseText, licenseFile, Charsets.UTF_8);
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Replace variables in license text
     *
     * @param licenseText License template text
     * @return License text with replaced variables
     */
    protected String replaceVariables(String licenseText) {
        if (!Strings.isNullOrEmpty(getCopyrightName())) {
            licenseText = licenseText.replaceAll("\\[fullname\\]", getCopyrightName());
        }

        if (getCopyrightYear() > 0) {
            licenseText = licenseText.replaceAll("\\[year\\]", Integer.toString(getCopyrightYear()));
        }

        return licenseText;
    }

    /**
     * Get the URL to download the license from
     *
     * @return URL to download the license from
     */
    public @Nullable String getLicenseUrl() {
        return this.licenseUrl;
    }

    /**
     * Set the URL to download the license from
     *
     * @param url URL to download the license from
     */
    public void setLicenseUrl(@Nullable String url) {
        this.licenseUrl = url;
    }

    /**
     * Get the year for the copyright information
     *
     * @return Year for the copyright information
     */
    public int getCopyrightYear() {
        return this.copyrightYear;
    }

    /**
     * Set the year for the copyright information
     *
     * @param copyrightYear Year for the copyright information
     */
    public void setCopyrightYear(int copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    /**
     * Get the name for the copyright information
     *
     * @return Name for the copyright information
     */
    public @Nullable String getCopyrightName() {
        return this.copyrightName;
    }

    /**
     * Set the name for the copyright information
     *
     * @param copyrightName Name for the copyright information
     */
    public void setCopyrightName(@Nullable String copyrightName) {
        this.copyrightName = copyrightName;
    }

    /**
     * Get the line length for license formatting
     *
     * @return Line length for license formatting
     */
    public int getLineLength() {
        return this.lineLength;
    }

    /**
     * Set the line length for license formatting
     *
     * @param lineLength Line length for license formatting
     */
    public void setLineLength(int lineLength) {
        this.lineLength = lineLength;
    }

    /**
     * Get the file to write license to
     *
     * @return File to write license to
     */
    public @Nullable File getLicenseFile() {
        return this.licenseFile;
    }

    /**
     * Set the file to write license to
     *
     * @param file File to write license to
     */
    public void setLicenseFile(@Nullable File file) {
        this.licenseFile = file;
    }

}
