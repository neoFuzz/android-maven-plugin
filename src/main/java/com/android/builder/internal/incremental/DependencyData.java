/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.builder.internal.incremental;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Holds dependency information, including the main compiled file, secondary input files
 * (usually headers), and output files.
 */
public class DependencyData {

    /**
     * List of secondary output files
     */
    @NonNull
    List<String> mSecondaryOutputFiles = Lists.newArrayList();
    /**
     * Main input file
     */
    @NonNull
    private String mMainFile;
    /**
     * List of secondary input files
     */
    @NonNull
    private List<String> mSecondaryFiles = Lists.newArrayList();
    /**
     * List of output files
     */
    @NonNull
    private List<String> mOutputFiles = Lists.newArrayList();

    /**
     * Initializes a new instance of the {@link DependencyData} class
     */
    DependencyData() {
    }

    /**
     * Parses the given dependency file and returns the parsed data
     *
     * @param dependencyFile the dependency file
     * @return the parsed data, or null if the file does not exist
     * @throws IOException if the dependency file cannot be read
     */
    @Nullable
    public static DependencyData parseDependencyFile(@NonNull File dependencyFile)
            throws IOException {
        // first check if the dependency file is here.
        if (!dependencyFile.isFile()) {
            return null;
        }

        // Read in our dependency file
        List<String> content = Files.readLines(dependencyFile, Charsets.UTF_8);
        return processDependencyData(content);
    }

    /**
     * @param content
     * @return
     */
    @VisibleForTesting
    @Nullable
    static DependencyData processDependencyData(@NonNull List<String> content) {
        // The format is technically:
        // output1 output2 [...]: dep1 dep2 [...]
        // However, the current tools generating those files guarantee that each file path
        // is on its own line, making it simpler to handle windows paths as well as path
        // with spaces in them.

        DependencyData data = new DependencyData();

        ParseMode parseMode = ParseMode.OUTPUT;

        for (String line : content) {
            line = line.trim();

            // check for separator at the beginning
            if (line.startsWith(":")) {
                parseMode = ParseMode.MAIN;
                line = line.substring(1).trim();
            }

            ParseMode nextMode = parseMode;

            // remove the \ at the end.
            if (line.endsWith("\\")) {
                line = line.substring(0, line.length() - 1).trim();
            }

            // detect : at the end indicating a parse mode change *after* we process this line.
            if (line.endsWith(":")) {
                if (parseMode == ParseMode.SECONDARY) {
                    nextMode = ParseMode.DONE;
                } else {
                    nextMode = ParseMode.MAIN;
                }
                line = line.substring(0, line.length() - 1).trim();
            }

            if (nextMode == ParseMode.DONE) {
                break;
            }

            if (!line.isEmpty()) {
                switch (parseMode) {
                    case OUTPUT:
                        data.addOutputFile(line);
                        break;
                    case SECONDARY:
                        data.addSecondaryFile(line);
                        break;
                    case MAIN:
                    default:
                        data.setMainFile(line);
                        nextMode = ParseMode.SECONDARY;
                        break;
                }
            }

            parseMode = nextMode;
        }

        if (data.getMainFile() == null) {
            return null;
        }

        return data;
    }

    /**
     * @return the main input file
     */
    @NonNull
    public String getMainFile() {
        return mMainFile;
    }

    /**
     * @param path the main input file
     */
    void setMainFile(@NonNull String path) {
        mMainFile = path;
    }

    /**
     * @return the list of secondary input files
     */
    @NonNull
    public List<String> getSecondaryFiles() {
        return mSecondaryFiles;
    }

    /**
     * @param path the secondary input file
     */
    void addSecondaryFile(@NonNull String path) {
        mSecondaryFiles.add(path);
    }

    /**
     * @return the list of output files
     */
    @NonNull
    public List<String> getOutputFiles() {
        return mOutputFiles;
    }

    /**
     * @param path the output file
     */
    void addOutputFile(@NonNull String path) {
        mOutputFiles.add(path);
    }

    /**
     * @param path the secondary output file
     */
    public void addSecondaryOutputFile(@NonNull String path) {
        mSecondaryOutputFiles.add(path);
    }

    /**
     * @return the list of secondary output files
     */
    @NonNull
    public List<String> getSecondaryOutputFiles() {
        return mSecondaryOutputFiles;
    }

    /**
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "DependencyData{" +
                "mMainFile='" + mMainFile + '\'' +
                ", mSecondaryFiles=" + mSecondaryFiles +
                ", mOutputFiles=" + mOutputFiles +
                '}';
    }

    /**
     * Enum representing the parsing mode
     */
    private enum ParseMode {
        /**
         * Parsing output files
         */
        OUTPUT,
        /**
         * Parsing main input file
         */
        MAIN,
        /**
         * Parsing secondary input files
         */
        SECONDARY,
        /**
         * Parsing is done
         */
        DONE
    }
}
