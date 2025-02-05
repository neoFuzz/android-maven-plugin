/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.builder.core;

import com.android.annotations.NonNull;
import com.android.ide.common.process.JavaProcessInfo;
import com.android.ide.common.process.ProcessEnvBuilder;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessInfoBuilder;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.repository.FullRevision;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * A builder to create a dex-specific ProcessInfoBuilder
 */
public class DexProcessBuilder extends ProcessEnvBuilder<DexProcessBuilder> {
    /**
     * Minimum build tools revision for dex input list.
     */
    private static final FullRevision MIN_BUILD_TOOLS_REVISION_FOR_DEX_INPUT_LIST = new FullRevision(21, 0, 0);
    /**
     * Minimum build tools revision for multi-dex support.
     */
    private static final FullRevision MIN_MULTIDEX_BUILD_TOOLS_REV = new FullRevision(21, 0, 0);
    /**
     * Minimum build tools revision for multi-dex support with native runtime.
     */
    private static final FullRevision MIN_MULTI_THREADED_DEX_BUILD_TOOLS_REV = new FullRevision(22, 0, 2);

    /**
     * the output file for the dex process.
     */
    @NonNull
    private final File mOutputFile;
    /**
     * the input files for the dex process.
     */
    private final Set<File> mInputs = Sets.newHashSet();
    /**
     * the input files for the dex process.
     */
    private boolean mVerbose = false;
    /**
     * the input files for the dex process.
     */
    private boolean mIncremental = false;
    /**
     * the input files for the dex process.
     */
    private boolean mNoOptimize = false;
    /**
     * the input files for the dex process.
     */
    private boolean mMultiDex = false;
    /**
     * the input files for the dex process.
     */
    private File mMainDexList = null;
    /**
     * the input files for the dex process.
     */
    private File mTempInputFolder = null;
    /**
     * the input files for the dex process.
     */
    private List<String> mAdditionalParams = null;

    /**
     * @param outputFile the output file for the dex process.
     */
    public DexProcessBuilder(@NonNull File outputFile) {
        mOutputFile = outputFile;
    }

    /**
     * @param verbose if true, verbose mode is enabled.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder setVerbose(boolean verbose) {
        mVerbose = verbose;
        return this;
    }

    /**
     * @param incremental if true, incremental mode is enabled.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder setIncremental(boolean incremental) {
        mIncremental = incremental;
        return this;
    }

    /**
     * @param noOptimize if true, no optimization is done.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder setNoOptimize(boolean noOptimize) {
        mNoOptimize = noOptimize;
        return this;
    }

    /**
     * @param multiDex if true, multi-dex is enabled.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder setMultiDex(boolean multiDex) {
        mMultiDex = multiDex;
        return this;
    }

    /**
     * @param mainDexList the main dex list file.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder setMainDexList(File mainDexList) {
        mMainDexList = mainDexList;
        return this;
    }

    /**
     * @param input the input file to add to the dex process.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder addInput(File input) {
        mInputs.add(input);
        return this;
    }

    /**
     * @param inputs the input files to add to the dex process.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder addInputs(@NonNull Collection<File> inputs) {
        mInputs.addAll(inputs);
        return this;
    }

    /**
     * @param tempInputFolder the temp input folder to use for the dex process.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder setTempInputFolder(File tempInputFolder) {
        mTempInputFolder = tempInputFolder;
        return this;
    }

    /**
     * @param params the additional parameters to pass to the dex process.
     * @return this builder
     */
    @NonNull
    public DexProcessBuilder additionalParameters(@NonNull List<String> params) {
        if (mAdditionalParams == null) {
            mAdditionalParams = Lists.newArrayListWithExpectedSize(params.size());
        }

        mAdditionalParams.addAll(params);

        return this;
    }

    /**
     * @param buildToolInfo the build tool info to use for the dex process.
     * @param dexOptions    the dex options to use for the dex process.
     * @return the process info for the dex process.
     * @throws ProcessException if the process cannot be created.
     */
    @NonNull
    public JavaProcessInfo build(
            @NonNull BuildToolInfo buildToolInfo,
            @NonNull DexOptions dexOptions) throws ProcessException {

        checkState(!mMultiDex ||
                        buildToolInfo.getRevision().compareTo(MIN_MULTIDEX_BUILD_TOOLS_REV) >= 0,
                "Multi dex requires Build Tools " +
                        MIN_MULTIDEX_BUILD_TOOLS_REV +
                        " / Current: " +
                        buildToolInfo.getRevision().toShortString());


        ProcessInfoBuilder builder = new ProcessInfoBuilder();
        builder.addEnvironments(mEnvironment);

        String dx = buildToolInfo.getPath(BuildToolInfo.PathId.DX_JAR);
        if (dx == null || !new File(dx).isFile()) {
            throw new IllegalStateException("dx.jar is missing");
        }

        builder.setClasspath(dx);
        builder.setMain("com.android.dx.command.Main");

        if (dexOptions.getJavaMaxHeapSize() != null) {
            builder.addJvmArg("-Xmx" + dexOptions.getJavaMaxHeapSize());
        } else {
            builder.addJvmArg("-Xmx1024M");
        }

        builder.addArgs("--dex");

        if (mVerbose) {
            builder.addArgs("--verbose");
        }

        if (dexOptions.getJumboMode()) {
            builder.addArgs("--force-jumbo");
        }

        if (mIncremental) {
            builder.addArgs("--incremental", "--no-strict");
        }

        if (mNoOptimize) {
            builder.addArgs("--no-optimize");
        }

        // only change thread count is build tools is 22.0.2+
        if (buildToolInfo.getRevision().compareTo(MIN_MULTI_THREADED_DEX_BUILD_TOOLS_REV) >= 0) {
            Integer threadCount = dexOptions.getThreadCount();
            if (threadCount == null) {
                builder.addArgs("--num-threads=4");
            } else {
                builder.addArgs("--num-threads=" + threadCount);
            }
        }

        if (mMultiDex) {
            builder.addArgs("--multi-dex");

            if (mMainDexList != null) {
                builder.addArgs("--main-dex-list", mMainDexList.getAbsolutePath());
            }
        }

        if (mAdditionalParams != null) {
            for (String arg : mAdditionalParams) {
                builder.addArgs(arg);
            }
        }


        builder.addArgs("--output", mOutputFile.getAbsolutePath());

        // input
        builder.addArgs(getFilesToAdd(buildToolInfo));

        return builder.createJavaProcess();
    }

    /**
     * @param buildToolInfo the build tool info to use for the dex process.
     * @return the list of files to add to the dex process.
     * @throws ProcessException if the process cannot be created.
     */
    @NonNull
    private List<String> getFilesToAdd(@NonNull BuildToolInfo buildToolInfo) throws
            ProcessException {
        // remove non-existing files.
        Set<File> existingFiles = Sets.filter(mInputs, input -> input != null && input.exists());

        if (existingFiles.isEmpty()) {
            throw new ProcessException("No files to pass to dex.");
        }

        // sort the inputs
        List<File> sortedList = Lists.newArrayList(existingFiles);
        Collections.sort(sortedList, (file, file2) -> {
            boolean file2IsDir = file2.isDirectory();
            if (file.isDirectory()) {
                return file2IsDir ? 0 : -1;
            } else if (file2IsDir) {
                return 1;
            }

            long diff = file.length() - file2.length();
            return diff > 0 ? 1 : (diff < 0 ? -1 : 0);
        });

        // convert to String-based paths.
        List<String> filePathList = Lists.newArrayListWithCapacity(sortedList.size());
        for (File f : sortedList) {
            filePathList.add(f.getAbsolutePath());
        }

        if (mTempInputFolder != null && buildToolInfo.getRevision()
                .compareTo(MIN_BUILD_TOOLS_REVISION_FOR_DEX_INPUT_LIST) >= 0) {
            File inputListFile = new File(mTempInputFolder, "inputList.txt");
            // Write each library line by line to file
            try {
                Files.asCharSink(inputListFile, Charsets.UTF_8).writeLines(filePathList);
            } catch (IOException e) {
                throw new ProcessException(e);
            }
            return Collections.singletonList("--input-list=" + inputListFile.getAbsolutePath());
        } else {
            return filePathList;
        }
    }
}
