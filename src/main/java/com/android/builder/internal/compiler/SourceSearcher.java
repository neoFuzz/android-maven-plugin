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

package com.android.builder.internal.compiler;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.internal.LoggedErrorException;
import com.android.ide.common.internal.WaitableExecutor;
import com.android.ide.common.process.ProcessException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class to search for source files (by extension) in a set of source folders.
 */
public class SourceSearcher {

    /**
     * The source folders to search in.
     */
    @NonNull
    private final List<File> mSourceFolders;
    /**
     * The extensions to search for.
     */
    private final String[] mExtensions;
    /**
     * Executor to use for parallel processing.
     */
    @Nullable
    private WaitableExecutor<Void> mExecutor;

    /**
     * @param sourceFolders the source folders to search in.
     * @param extensions    the extensions to search for.
     */
    public SourceSearcher(@NonNull List<File> sourceFolders, String... extensions) {
        mSourceFolders = sourceFolders;
        mExtensions = extensions;
    }

    /**
     * @param useExecutor whether to use an executor or not.
     */
    public void setUseExecutor(boolean useExecutor) {
        if (useExecutor) {
            mExecutor = new WaitableExecutor<>();
        } else {
            mExecutor = null;
        }
    }

    /**
     * @param processor the processor to use.
     * @throws ProcessException     if the search failed.
     * @throws LoggedErrorException if the search failed.
     * @throws InterruptedException if the search was interrupted.
     * @throws IOException          if the file cannot be read or written to.
     */
    public void search(@NonNull SourceFileProcessor processor)
            throws ProcessException, LoggedErrorException, InterruptedException, IOException {
        for (File file : mSourceFolders) {
            // pass both the root folder (the source folder) and the file/folder to process,
            // in this case the source folder as well.
            processFile(file, file, processor);
        }

        if (mExecutor != null) {
            mExecutor.waitForTasksWithQuickFail(true /*cancelRemaining*/);
        }
    }

    /**
     * @param rootFolder the root folder where the file is located.
     * @param file       the file to process.
     * @param processor  the processor to use.
     * @throws ProcessException if the file cannot be processed.
     * @throws IOException      if the file cannot be read or written to.
     */
    private void processFile(
            @NonNull final File rootFolder,
            @NonNull final File file,
            @NonNull final SourceFileProcessor processor)
            throws ProcessException, IOException {
        if (file.isFile()) {
            // get the extension of the file.
            if (checkExtension(file)) {
                if (mExecutor != null) {
                    mExecutor.execute(() -> {
                        processor.processFile(rootFolder, file);
                        return null;
                    });
                } else {
                    processor.processFile(rootFolder, file);
                }
            }
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    processFile(rootFolder, child, processor);
                }
            }
        }
    }

    /**
     * @param file the file to check.
     * @return true if the file has an extension that matches one of the extensions in the list.
     */
    private boolean checkExtension(File file) {
        if (mExtensions.length == 0) {
            return true;
        }

        String filename = file.getName();
        int pos = filename.indexOf('.');
        if (pos != -1) {
            String extension = filename.substring(pos + 1);
            for (String ext : mExtensions) {
                if (ext.equalsIgnoreCase(extension)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Interface to process a file.
     */
    public interface SourceFileProcessor {
        /**
         * Process a file.
         *
         * @param sourceFolder the root folder where the file is located.
         * @param sourceFile   the file to process.
         * @throws ProcessException if the file cannot be processed.
         * @throws IOException      if the file cannot be read or written to.
         */
        void processFile(@NonNull File sourceFolder, @NonNull File sourceFile)
                throws ProcessException, IOException;
    }
}
