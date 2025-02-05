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
import com.google.common.collect.Sets;

import java.io.File;
import java.util.Set;

/**
 * Source Searcher processor, gathering a list of folders containing processed source files.
 */
public class LeafFolderGatherer implements SourceSearcher.SourceFileProcessor {

    /**
     * Set of folders containing source files.
     */
    @NonNull
    private final Set<File> mFolders = Sets.newHashSet();

    /**
     * @param sourceFolder the root folder where the file is located.
     * @param sourceFile   the file to process.
     */
    @Override
    public void processFile(@NonNull File sourceFolder, @NonNull File sourceFile) {
        mFolders.add(sourceFile.getParentFile());
    }

    /**
     * @return the set of folders containing source files.
     */
    @NonNull
    public Set<File> getFolders() {
        return mFolders;
    }
}
