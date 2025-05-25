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

package com.android.sdklib;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.repository.Revision;
import com.android.repository.api.LocalPackage;
import com.android.sdklib.repository.FullRevision;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.Map;

import static com.android.SdkConstants.*;
import static com.android.sdklib.BuildToolInfo.PathId.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Information on a specific build-tool folder.
 * <p>
 * For unit tests, see:
 * - sdklib/src/test/.../LocalSdkTest
 * - sdklib/src/test/.../SdkManagerTest
 * - sdklib/src/test/.../BuildToolInfoTest
 * </p>
 */
public class BuildToolInfo {

    /**
     * The build-tool revision.
     */
    @NonNull
    private final FullRevision mRevision;
    /**
     * The path to the build-tool folder specific to this revision.
     */
    @NonNull
    private final File mPath;
    /**
     * The path to the build-tool folder specific to this revision.
     */
    private final Map<PathId, String> mPaths = Maps.newEnumMap(PathId.class);

    /**
     * @param revision the build-tool revision.
     * @param path     the path to the build-tool folder specific to this revision.
     */
    public BuildToolInfo(@NonNull FullRevision revision, @NonNull File path) {
        mRevision = revision;
        mPath = path;

        add(AAPT, FN_AAPT);
        add(AIDL, FN_AIDL);
        add(DX, FN_DX);
        add(DX_JAR, FD_LIB + File.separator + FN_DX_JAR);
        add(LLVM_RS_CC, FN_RENDERSCRIPT);
        add(ANDROID_RS, OS_FRAMEWORK_RS);
        add(ANDROID_RS_CLANG, OS_FRAMEWORK_RS_CLANG);
        add(DEXDUMP, FN_DEXDUMP);
        add(BCC_COMPAT, FN_BCC_COMPAT);
        add(LD_ARM, FN_LD_ARM);
        add(LD_X86, FN_LD_X86);
        add(LD_MIPS, FN_LD_MIPS);
        add(ZIP_ALIGN, FN_ZIPALIGN);
        add(JACK, FN_JACK);
        add(JILL, FN_JILL);
        add(SPLIT_SELECT, FN_SPLIT_SELECT);
    }

    /**
     * Creates a {@link BuildToolInfo} from a {@link LocalPackage}.
     *
     * @param localPackage The package to create the {@link BuildToolInfo} from.
     * @return The created {@link BuildToolInfo}.
     */
    @NonNull
    public static BuildToolInfo fromLocalPackage(@NonNull LocalPackage localPackage) {
        checkNotNull(localPackage, "localPackage");
        checkArgument(
                localPackage.getPath().contains(SdkConstants.FD_BUILD_TOOLS),
                "%s package required.",
                SdkConstants.FD_BUILD_TOOLS);
        return fromStandardDirectoryLayout(localPackage.getVersion(), localPackage.getLocation());
    }

    /**
     * Creates a {@link BuildToolInfo} from a directory which follows the standard layout
     * convention.
     *
     * @param revision The revision of the build-tool.
     * @param path     The path to the build-tool folder specific to this revision.
     * @return The created {@link BuildToolInfo}.
     */
    @NonNull
    public static BuildToolInfo fromStandardDirectoryLayout(
            @NonNull Revision revision,
            @NonNull File path) {
        FullRevision fr = FullRevision.parseRevision(revision.toString());
        return new BuildToolInfo(fr, path);
    }

    /**
     * @param id   the path-id to add.
     * @param leaf the path to add, relative to the build-tool folder.
     */
    private void add(PathId id, String leaf) {
        add(id, new File(mPath, leaf));
    }

    /**
     * @param id   the path-id to add.
     * @param path the path to add.
     */
    private void add(PathId id, @NonNull File path) {
        String str = path.getAbsolutePath();
        if (path.isDirectory() && str.charAt(str.length() - 1) != File.separatorChar) {
            str += File.separatorChar;
        }
        mPaths.put(id, str);
    }

    /**
     * Returns the revision.
     *
     * @return The revision of the build-tool.
     */
    @NonNull
    public FullRevision getRevision() {
        return mRevision;
    }

    /**
     * Returns the build-tool revision-specific folder.
     * <p>
     * For compatibility reasons, use {@link #getPath(PathId)} if you need the path to a
     * specific tool.
     *
     * @return The path to the build-tool folder.
     */
    @NonNull
    public File getLocation() {
        return mPath;
    }

    /**
     * Returns the path of a build-tool component.
     *
     * @param pathId the id representing the path to return.
     * @return The absolute path for that tool, with a / separator if it's a folder.
     * Null if the path-id is unknown.
     */
    public String getPath(@NonNull PathId pathId) {
        assert pathId.isPresentIn(mRevision);

        return mPaths.get(pathId);
    }

    /**
     * Returns a debug representation suitable for unit-tests.
     * Note that unit-tests need to clean up the paths to avoid inconsistent results.
     */
    @Override
    public String toString() {
        return "<BuildToolInfo rev=" + mRevision +    //$NON-NLS-1$
                ", mPath=" + mPath +                   //$NON-NLS-1$
                ", mPaths=" + getPathString() +        //$NON-NLS-1$
                ">";
    }

    @NonNull
    private String getPathString() {
        StringBuilder sb = new StringBuilder("{");

        for (Map.Entry<PathId, String> entry : mPaths.entrySet()) {
            if (entry.getKey().isPresentIn(mRevision)) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append('=').append(entry.getValue());
            }
        }

        sb.append('}');

        return sb.toString();
    }

    /**
     * Enum representing the paths to the build-tools components.
     */
    public enum PathId {
        /**
         * OS Path to the target's version of the aapt tool.
         */
        AAPT(Constants.V1_0_0),
        /**
         * OS Path to the target's version of the aidl tool.
         */
        AIDL(Constants.V1_0_0),
        /**
         * OS Path to the target's version of the dx tool.
         */
        DX(Constants.V1_0_0),
        /**
         * OS Path to the target's version of the dx.jar file.
         */
        DX_JAR(Constants.V1_0_0),
        /**
         * OS Path to the llvm-rs-cc binary for Renderscript.
         */
        LLVM_RS_CC(Constants.V1_0_0),
        /**
         * OS Path to the Renderscript include folder.
         */
        ANDROID_RS(Constants.V1_0_0),
        /**
         * OS Path to the Renderscript(clang) include folder.
         */
        ANDROID_RS_CLANG(Constants.V1_0_0),

        /**
         * OS Path to the zipalign tool.
         */
        DEXDUMP(Constants.V1_0_0),

        // --- NEW IN 18.1.0 ---

        /**
         * OS Path to the bcc_compat tool.
         */
        BCC_COMPAT(Constants.V18_1_0),
        /**
         * OS Path to the ARM linker.
         */
        LD_ARM(Constants.V18_1_0),
        /**
         * OS Path to the X86 linker.
         */
        LD_X86(Constants.V18_1_0),
        /**
         * OS Path to the MIPS linker.
         */
        LD_MIPS(Constants.V18_1_0),

        /**
         * OS Path to the zipalign tool.
         */
        // --- NEW IN 19.1.0 ---
        ZIP_ALIGN("19.1.0"),

        /**
         * OS Path to the jack.jar file.
         */
        // --- NEW IN 21.x.y ---
        JACK("21.1.0"),
        /**
         * OS Path to the jill.jar file.
         */
        JILL("21.1.0"),

        /**
         * OS Path to the split-select tool.
         */
        SPLIT_SELECT("22.0.0");

        /**
         * min revision this element was introduced.
         *
         */
        private final FullRevision mMinRevision;

        /**
         * Creates the enum with a min revision in which this
         * tools appeared in the build tools.
         *
         * @param minRevision the min revision.
         */
        PathId(@NonNull String minRevision) {
            mMinRevision = FullRevision.parseRevision(minRevision);
        }

        /**
         * Returns whether the enum of present in a given rev of the build tools.
         *
         * @param fullRevision the build tools revision.
         * @return true if the tool is present.
         */
        boolean isPresentIn(@NonNull FullRevision fullRevision) {
            return fullRevision.compareTo(mMinRevision) >= 0;
        }

        private static class Constants {
            public static final String V1_0_0 = "1.0.0";
            public static final String V18_1_0 = "18.1.0";
        }
    }
}
