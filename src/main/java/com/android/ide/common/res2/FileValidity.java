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

package com.android.ide.common.res2;

import java.io.File;

/**
 * @param <S> the type of the data set associated with this file validity.
 */
public class FileValidity<S extends DataSet> {

    /**
     * The current file status.
     */
    FileStatus status = FileStatus.UNKNOWN_FILE;
    /**
     * Data set associated with this file validity.
     */
    S dataSet;
    /**
     * Absolute path of the source file.
     */
    File sourceFile;

    /**
     * Returns the file validity status.
     * <p>
     * The status is one of the following:
     *   <ul>
     *     <li>{@link FileStatus#VALID_FILE}</li>
     *     <li>{@link FileStatus#IGNORED_FILE}</li>
     *     <li>{@link FileStatus#UNKNOWN_FILE}</li>
     *   </ul>
     *
     * @return a {@link FileStatus} indicating the status of the file.
     */
    public FileStatus getStatus() {
        return status;
    }

    /**
     * @return the data set associated with this file validity.
     */
    public S getDataSet() {
        return dataSet;
    }

    /**
     * @return the absolute path of the source file.
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Enum to represent the status of a file
     */
    public enum FileStatus {
        /**
         * File is valid and can be used
         */
        VALID_FILE,
        /**
         * File is not valid and should be ignored
         */
        IGNORED_FILE,
        /**
         * File is unknown and should be ignored
         */
        UNKNOWN_FILE
    }
}
