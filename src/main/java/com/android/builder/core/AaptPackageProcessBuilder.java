/*
 * Copyright (C) 2014 The Android Open Source Project
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
import com.android.annotations.Nullable;
import com.android.builder.dependency.SymbolFileProvider;
import com.android.builder.model.AaptOptions;
import com.android.ide.common.process.ProcessEnvBuilder;
import com.android.ide.common.process.ProcessInfo;
import com.android.ide.common.process.ProcessInfoBuilder;
import com.android.resources.Density;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.ILogger;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builds the ProcessInfo necessary for an aapt package invocation
 */
public class AaptPackageProcessBuilder extends ProcessEnvBuilder<AaptPackageProcessBuilder> {

    /**
     * The separator used to separate multiple values when passing lists of values to aapt.
     */
    public static final String COMMA_SEP = "\",\"";
    /**
     * The manifest file to package.
     */
    @NonNull
    private final File mManifestFile;
    /**
     * The options to use when invoking aapt.
     */
    @NonNull
    private final AaptOptions mOptions;
    /**
     * The splits collection
     */
    @Nullable
    Collection<String> mSplits;
    /**
     * The list of files to include as resources.
     */
    @Nullable
    String mPackageForR;
    /**
     * The preferred density.
     */
    @Nullable
    String mPreferredDensity;
    /**
     * The {@code resources} folder.
     */
    @Nullable
    private File mResFolder;
    /**
     * The {@code assets} folder.
     */
    @Nullable
    private File mAssetsFolder;
    /**
     * Whether to use Verbose or not.
     */
    private boolean mVerboseExec = false;
    /**
     * The source directory.
     */
    @Nullable
    private String mSourceOutputDir;
    /**
     * The symbol output directory
     */
    @Nullable
    private String mSymbolOutputDir;
    /**
     * The symbol libraries
     */
    @Nullable
    private List<? extends SymbolFileProvider> mLibraries;
    /**
     * The resource package output directory
     */
    @Nullable
    private String mResPackageOutput;
    /**
     * The proguard output directory
     */
    @Nullable
    private String mProguardOutput;
    /**
     * The VariantType in use.
     */
    @Nullable
    private VariantType mType;
    /**
     * Debugging flag
     */
    private boolean mDebuggable = false;
    /**
     * Flag for {@code pseudolocales} state.
     */
    private boolean mPseudoLocalesEnabled = false;
    /**
     * The resource configurations to include. If {@code null}, all configurations are included.
     */
    @Nullable
    private Collection<String> mResourceConfigs;

    /**
     * @param manifestFile the location of the manifest file
     * @param options      the {@link com.android.builder.model.AaptOptions}
     */
    public AaptPackageProcessBuilder(
            @NonNull File manifestFile,
            @NonNull AaptOptions options) {
        checkNotNull(manifestFile, "manifestFile cannot be null.");
        checkNotNull(options, "options cannot be null.");
        mManifestFile = manifestFile;
        mOptions = options;
    }

    /**
     * Check if input is null or empty.
     *
     * @param collection the collection to check for {@code null} or empty.
     * @return {@code true} if the collection is {@code null} or empty.
     */
    private static boolean isNullOrEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Gets the Density Resource Configs from the given resource configs.
     *
     * @param resourceConfigs the resource configurations to include. If {@code null}, all
     *                        configurations are included.
     * @return a collection of resource configurations that are density values
     */
    @NonNull
    private static Collection<String> getDensityResConfigs(Collection<String> resourceConfigs) {
        return Collections2.filter(new ArrayList<>(resourceConfigs),
                input -> Density.getEnum(input) != null);
    }

    /**
     * Gets the manifest file.
     *
     * @return the ProcessInfo for the aapt package invocation
     */
    @NonNull
    public File getManifestFile() {
        return mManifestFile;
    }

    /**
     * @param resFolder the merged res folder
     * @return itself
     */
    public AaptPackageProcessBuilder setResFolder(@NonNull File resFolder) {
        if (!resFolder.isDirectory()) {
            throw new RuntimeException("resFolder parameter is not a directory");
        }
        mResFolder = resFolder;
        return this;
    }

    /**
     * @param assetsFolder the merged asset folder
     * @return itself
     */
    public AaptPackageProcessBuilder setAssetsFolder(@NonNull File assetsFolder) {
        if (!assetsFolder.isDirectory()) {
            throw new RuntimeException("assetsFolder parameter is not a directory");
        }
        mAssetsFolder = assetsFolder;
        return this;
    }

    /**
     * Get the source output directory.
     *
     * @return the source output directory.
     */
    @Nullable
    public String getSourceOutputDir() {
        return mSourceOutputDir;
    }

    /**
     * @param sourceOutputDir optional source folder to generate R.java
     * @return itself
     */
    public AaptPackageProcessBuilder setSourceOutputDir(@Nullable String sourceOutputDir) {
        mSourceOutputDir = sourceOutputDir;
        return this;
    }

    /**
     * Get the symbol output directory.
     *
     * @return the symbol output directory
     */
    @Nullable
    public String getSymbolOutputDir() {
        return mSymbolOutputDir;
    }

    /**
     * @param symbolOutputDir the folder to write symbols into itself
     * @return itself
     */
    public AaptPackageProcessBuilder setSymbolOutputDir(@Nullable String symbolOutputDir) {
        mSymbolOutputDir = symbolOutputDir;
        return this;
    }

    /**
     * Get the symbol libraries.
     *
     * @return The list of the symbol libraries.
     */
    @NonNull
    public List<? extends SymbolFileProvider> getLibraries() {
        return mLibraries == null ? ImmutableList.of() : mLibraries;
    }

    /**
     * @param libraries the flat list of libraries
     * @return itself
     */
    public AaptPackageProcessBuilder setLibraries(
            @NonNull List<? extends SymbolFileProvider> libraries) {
        mLibraries = libraries;
        return this;
    }

    /**
     * @param resPackageOutput optional filepath for packaged resources
     * @return itself
     */
    public AaptPackageProcessBuilder setResPackageOutput(@Nullable String resPackageOutput) {
        mResPackageOutput = resPackageOutput;
        return this;
    }

    /**
     * @param proguardOutput optional filepath for proguard file to generate
     * @return itself
     */
    public AaptPackageProcessBuilder setProguardOutput(@Nullable String proguardOutput) {
        mProguardOutput = proguardOutput;
        return this;
    }

    /**
     * Gets the type in use.
     *
     * @return the type of the variant being built
     */
    @Nullable
    public VariantType getType() {
        return mType;
    }

    /**
     * @param type the type of the variant being built
     * @return itself
     */
    public AaptPackageProcessBuilder setType(@NonNull VariantType type) {
        this.mType = type;
        return this;
    }

    /**
     * @param debuggable whether the app is debuggable
     * @return itself
     */
    public AaptPackageProcessBuilder setDebuggable(boolean debuggable) {
        this.mDebuggable = debuggable;
        return this;
    }

    /**
     * @param resourceConfigs a list of resource config filters to pass to aapt.
     * @return itself
     */
    public AaptPackageProcessBuilder setResourceConfigs(@NonNull Collection<String> resourceConfigs) {
        this.mResourceConfigs = resourceConfigs;
        return this;
    }

    /**
     * @param splits optional list of split dimensions values (like a density or an abi). This
     *               will be used by aapt to generate the corresponding pure split apks.
     * @return itself
     */
    public AaptPackageProcessBuilder setSplits(@NonNull Collection<String> splits) {
        this.mSplits = splits;
        return this;
    }

    /**
     * @return the verbose mode
     */
    public AaptPackageProcessBuilder setVerbose() {
        mVerboseExec = true;
        return this;
    }

    /**
     * @param pseudoLocalesEnabled whether to generate pseudo-locale strings
     * @return itself
     */
    public AaptPackageProcessBuilder setPseudoLocalesEnabled(boolean pseudoLocalesEnabled) {
        mPseudoLocalesEnabled = pseudoLocalesEnabled;
        return this;
    }

    /**
     * Specifies a preference for a particular density. Resources that do not match this density
     * and have variants that are a closer match are removed.
     *
     * @param density the preferred density
     * @return itself
     */
    public AaptPackageProcessBuilder setPreferredDensity(String density) {
        mPreferredDensity = density;
        return this;
    }

    /**
     * @return the package to generate the R class in.
     */
    @Nullable
    String getPackageForR() {
        return mPackageForR;
    }

    /**
     * @param packageForR Package override to generate the R class in a different package.
     * @return itself
     */
    public AaptPackageProcessBuilder setPackageForR(@NonNull String packageForR) {
        this.mPackageForR = packageForR;
        return this;
    }

    /**
     * Builds the ProcessInfo necessary for an aapt package invocation
     *
     * @param buildToolInfo the {@link BuildToolInfo} to use for the build.
     * @param target        the {@link IAndroidTarget} to use for the build.
     * @param logger        the logger to use for logging errors and warnings.
     * @return the ProcessInfo for the aapt package invocation
     */
    public ProcessInfo build(
            @NonNull BuildToolInfo buildToolInfo,
            @NonNull IAndroidTarget target,
            @NonNull ILogger logger) {

        // if both output types are empty, then there's nothing to do and this is an error
        checkArgument(mSourceOutputDir != null || mResPackageOutput != null,
                "No output provided for aapt task");
        if (mSymbolOutputDir != null || mSourceOutputDir != null) {
            checkNotNull(mLibraries,
                    "libraries cannot be null if symbolOutputDir or sourceOutputDir is non-null");
        }

        // check resConfigs and split settings coherence.
        checkResConfigsVersusSplitSettings(logger);

        ProcessInfoBuilder builder = new ProcessInfoBuilder();
        builder.addEnvironments(mEnvironment);

        String aapt = buildToolInfo.getPath(BuildToolInfo.PathId.AAPT);
        if (aapt == null || !new File(aapt).isFile()) {
            throw new IllegalStateException("aapt is missing");
        }

        builder.setExecutable(aapt);
        builder.addArgs("package");

        if (mVerboseExec) {
            builder.addArgs("-v");
        }

        builder.addArgs("-f");
        builder.addArgs("--no-crunch");

        // inputs
        builder.addArgs("-I", target.getPath(IAndroidTarget.ANDROID_JAR));

        builder.addArgs("-M", mManifestFile.getAbsolutePath());

        if (mResFolder != null) {
            builder.addArgs("-S", mResFolder.getAbsolutePath());
        }

        if (mAssetsFolder != null) {
            builder.addArgs("-A", mAssetsFolder.getAbsolutePath());
        }

        // outputs
        if (mSourceOutputDir != null) {
            builder.addArgs("-m");
            builder.addArgs("-J", mSourceOutputDir);
        }

        if (mResPackageOutput != null) {
            builder.addArgs("-F", mResPackageOutput);
        }

        if (mProguardOutput != null) {
            builder.addArgs("-G", mProguardOutput);
        }

        if (mSplits != null) {
            for (String split : mSplits) {

                builder.addArgs("--split", split);
            }
        }

        // options controlled by build variants

        if (mDebuggable) {
            builder.addArgs("--debug-mode");
        }

        if (mType != VariantType.ANDROID_TEST &&
                mPackageForR != null) {
            builder.addArgs("--custom-package", mPackageForR);
            logger.verbose("Custom package for R class: '%s'", mPackageForR);
        }


        if (mPseudoLocalesEnabled) {
            if (buildToolInfo.getRevision().getMajor() >= 21) {
                builder.addArgs("--pseudo-localize");
            } else {
                throw new RuntimeException(
                        "Pseudo-localization is only available since Build Tools version 21.0.0,"
                                + " please upgrade or turn it off.");
            }
        }

        // library specific options
        if (mType == VariantType.LIBRARY) {
            builder.addArgs("--non-constant-id");
        }

        // AAPT options
        String ignoreAssets = mOptions.getIgnoreAssets();
        if (ignoreAssets != null) {
            builder.addArgs("--ignore-assets", ignoreAssets);
        }

        if (mOptions.getFailOnMissingConfigEntry()) {
            if (buildToolInfo.getRevision().getMajor() > 20) {
                builder.addArgs("--error-on-missing-config-entry");
            } else {
                throw new IllegalStateException("aaptOptions:failOnMissingConfigEntry cannot be used"
                        + " with SDK Build Tools revision earlier than 21.0.0");
            }
        }

        // never compress apks.
        builder.addArgs("-0", "apk");

        // add custom no-compress extensions
        Collection<String> noCompressList = mOptions.getNoCompress();
        if (noCompressList != null) {
            for (String noCompress : noCompressList) {
                builder.addArgs("-0", noCompress);
            }
        }
        List<String> additionalParameters = mOptions.getAdditionalParameters();
        if (!isNullOrEmpty(additionalParameters)) {
            builder.addArgs(additionalParameters);
        }

        List<String> resourceConfigs = new ArrayList<>();
        if (!isNullOrEmpty(mResourceConfigs)) {
            resourceConfigs.addAll(mResourceConfigs);
        }
        if (buildToolInfo.getRevision().getMajor() < 21 && mPreferredDensity != null) {
            resourceConfigs.add(mPreferredDensity);
            // when adding a density filter, also always add the nodpi option.
            resourceConfigs.add(Density.NODPI.getResourceValue());
        }


        // separate the density and language resource configs, since starting in 21, the
        // density resource configs should be passed with --preferred-density to ensure packaging
        // of scalable resources when no resource for the preferred density is present.
        List<String> otherResourceConfigs = new ArrayList<>();
        List<String> densityResourceConfigs = new ArrayList<>();
        if (!resourceConfigs.isEmpty()) {
            if (buildToolInfo.getRevision().getMajor() >= 21) {
                for (String resourceConfig : resourceConfigs) {
                    if (Density.getEnum(resourceConfig) != null) {
                        densityResourceConfigs.add(resourceConfig);
                    } else {
                        otherResourceConfigs.add(resourceConfig);
                    }
                }
            } else {
                // before 21, everything is passed with -c option.
                otherResourceConfigs = resourceConfigs;
            }
        }
        if (!otherResourceConfigs.isEmpty()) {
            Joiner joiner = Joiner.on(',');
            builder.addArgs("-c", joiner.join(otherResourceConfigs));
        }
        for (String densityResourceConfig : densityResourceConfigs) {
            builder.addArgs("--preferred-density", densityResourceConfig);
        }

        if (buildToolInfo.getRevision().getMajor() >= 21 && mPreferredDensity != null) {
            if (!isNullOrEmpty(mResourceConfigs)) {
                Collection<String> densityResConfig = getDensityResConfigs(mResourceConfigs);
                if (!densityResConfig.isEmpty()) {
                    throw new RuntimeException(String.format(
                            "When using splits in tools 21 and above, resConfigs should not contain "
                                    + "any densities. Right now, it contains \"%1$s\"\n"
                                    + "Suggestion: remove these from resConfigs from build.gradle",
                            Joiner.on(COMMA_SEP).join(densityResConfig)));
                }
            }
            builder.addArgs("--preferred-density", mPreferredDensity);
        }

        if (buildToolInfo.getRevision().getMajor() < 21 && mPreferredDensity != null) {
            logger.warning(String.format("Warning : Project is building density based multiple APKs"
                            + " but using tools version %1$s, you should upgrade to build-tools 21 or above"
                            + " to ensure proper packaging of resources.",
                    buildToolInfo.getRevision().getMajor()));
        }

        if (mSymbolOutputDir != null &&
                (mType == VariantType.LIBRARY || !mLibraries.isEmpty())) {
            builder.addArgs("--output-text-symbols", mSymbolOutputDir);
        }

        return builder.createProcess();
    }

    /**
     * Checks that the resConfigs and split settings are coherent.
     *
     * @param ignored the logger to use for logging errors and warnings. Not used.
     */
    private void checkResConfigsVersusSplitSettings(ILogger ignored) {
        if (isNullOrEmpty(mResourceConfigs) || isNullOrEmpty(mSplits)) {
            return;
        }

        // only consider the Density related resConfig settings.
        Collection<String> resConfigs = getDensityResConfigs(mResourceConfigs);
        List<String> splits = new ArrayList<>(mSplits);
        splits.removeAll(resConfigs);
        if (!splits.isEmpty()) {
            // some splits are required, yet the resConfigs do not contain the split density value
            // which mean that the resulting split file would be empty, flag this as an error.
            throw new RuntimeException(String.format(
                    """
                            Splits for densities "%1$s" were configured, yet the resConfigs settings does\
                             not include such splits. The resulting split APKs would be empty.
                            Suggestion : exclude those splits in your build.gradle :\s
                            splits {
                                 density {
                                     enable true
                                     exclude "%2$s"
                                 }
                            }
                            OR add them to the resConfigs list.
                            """,
                    Joiner.on(",").join(splits),
                    Joiner.on(COMMA_SEP).join(splits)));
        }
        resConfigs.removeAll(mSplits);
        if (!resConfigs.isEmpty()) {
            // there are densities present in the resConfig but not in splits, which mean that those
            // densities will be packaged in the main APK
            throw new RuntimeException(String.format(
                    """
                            Inconsistent density configuration, with "%1$s" present on \
                            resConfig settings, while only "%2$s" densities are requested \
                            in splits APK density settings.
                            Suggestion : remove extra densities from the resConfig :\s
                            defaultConfig {
                                 resConfigs "%2$s"
                            }
                            OR remove such densities from the split's exclude list.
                            """
                    ,
                    Joiner.on(",").join(resConfigs),
                    Joiner.on(COMMA_SEP).join(mSplits)));
        }
    }
}
