/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.builder.packaging;

import com.android.annotations.NonNull;
import com.android.builder.signing.SignedJarBuilder.IZipEntryFilter.ZipAbortException;

import java.io.File;
import java.io.Serial;

/**
 * An exception thrown during packaging of an APK file.
 */
public final class DuplicateFileException extends ZipAbortException {
    /**
     * Serial version UID for serialization
     */
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * The path to the archive that contains the duplicate files
     */
    private final String mArchivePath;
    /**
     * The first file that was detected as a duplicate
     */
    private final File mFile1;
    /**
     * The second file that was detected as a duplicate
     */
    private final File mFile2;

    /**
     * @param archivePath the path to the archive that contains the duplicate files
     * @param file1       the first file that was detected as a duplicate
     * @param file2       the second file that was detected as a duplicate
     */
    public DuplicateFileException(@NonNull String archivePath, @NonNull File file1,
                                  @NonNull File file2) {
        super();
        mArchivePath = archivePath;
        mFile1 = file1;
        mFile2 = file2;
    }

    /**
     * @return the path to the archive that contains the duplicate files
     */
    public String getArchivePath() {
        return mArchivePath;
    }

    /**
     * @return the first file that was detected as a duplicate
     */
    public File getFile1() {
        return mFile1;
    }

    /**
     * @return the second file that was detected as a duplicate
     */
    public File getFile2() {
        return mFile2;
    }

    /**
     * @return a user friendly error message
     */
    @Override
    @NonNull
    public String getMessage() {
        return "Duplicate files copied in APK " + mArchivePath + '\n' +
                "\tFile 1: " + mFile1 + '\n' +
                "\tFile 2: " + mFile2 + '\n';
    }
}