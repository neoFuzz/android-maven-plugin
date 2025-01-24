/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.android.builder.internal.BaseConfigImpl;
import com.android.builder.model.BuildType;
import com.android.builder.model.SigningConfig;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serial;

/**
 * Default implementation of the {@link BuildType} interface.
 */
public class DefaultBuildType extends BaseConfigImpl implements BuildType {
    /**
     * Serial version UID for serialization purposes.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Name of this build type.
     */
    private final String mName;
    /**
     * Whether this build type should generate a debuggable apk.
     */
    private boolean mDebuggable = false;
    /**
     * Whether this build type should generate a pseudo-localized apk.
     */
    private boolean mPseudoLocalesEnabled = false;
    /**
     * Whether this build type should generate a debuggable apk.
     */
    private boolean mTestCoverageEnabled = false;
    /**
     * Whether this build type should generate a debuggable apk.
     */
    private boolean mJniDebuggable = false;
    /**
     * Whether this build type should generate a debuggable apk.
     */
    private boolean mRenderscriptDebuggable = false;
    /**
     * The optimization level to use when generating a debuggable apk.
     */
    private int mRenderscriptOptimLevel = 3;
    /**
     * The application id suffix
     */
    private String mApplicationIdSuffix = null;
    /**
     * The version name suffix
     */
    private String mVersionNameSuffix = null;
    /**
     * Whether this build type should be minified.
     */
    private boolean mMinifyEnabled = false;
    /**
     * The signing configuration
     */
    private transient SigningConfig mSigningConfig = null;
    /**
     * Whether this build type should be embedded with micro app
     */
    private boolean mEmbedMicroApp = true;

    /**
     * Whether this build type should be zip-aligned
     */
    private boolean mZipAlignEnabled = true;

    /**
     * @param name the name of the build type
     */
    public DefaultBuildType(@NonNull String name) {
        mName = name;
    }

    /**
     * @param that the build type to copy from
     * @return a new build type with the same data as <code>that</code>
     */
    public DefaultBuildType initWith(DefaultBuildType that) {
        _initWith(that);

        setDebuggable(that.isDebuggable());
        setTestCoverageEnabled(that.isTestCoverageEnabled());
        setJniDebuggable(that.isJniDebuggable());
        setRenderscriptDebuggable(that.isRenderscriptDebuggable());
        setRenderscriptOptimLevel(that.getRenderscriptOptimLevel());
        setApplicationIdSuffix(that.getApplicationIdSuffix());
        setVersionNameSuffix(that.getVersionNameSuffix());
        setMinifyEnabled(that.isMinifyEnabled());
        setZipAlignEnabled(that.isZipAlignEnabled());
        setSigningConfig(that.getSigningConfig());
        setEmbedMicroApp(that.isEmbedMicroApp());
        setPseudoLocalesEnabled(that.isPseudoLocalesEnabled());

        return this;
    }

    /**
     * Name of this build type.
     *
     * @return the name
     */
    @Override
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Whether this build type should generate a debuggable apk.
     *
     * @param debuggable whether this build type should generate a debuggable apk.
     * @return true if debuggable, false otherwise
     */
    @NonNull
    public BuildType setDebuggable(boolean debuggable) {
        mDebuggable = debuggable;
        return this;
    }

    /**
     * Whether this build type should generate a debuggable apk.
     *
     * @return true if debuggable, false otherwise
     */
    @Override
    public boolean isDebuggable() {
        // Accessing coverage data requires a debuggable package.
        return mDebuggable || mTestCoverageEnabled;
    }

    /**
     * Whether test coverage is enabled for this build type.
     *
     * <p>If enabled this uses Jacoco to capture coverage and creates a report in the build
     * directory.
     *
     * <p>The version of Jacoco can be configured with:
     * <pre>
     * android {
     *   jacoco {
     *     version = '0.6.2.201302030002'
     *   }
     * }
     * </pre>
     *
     * @return true if test coverage is enabled, false otherwise
     */
    @Override
    public boolean isTestCoverageEnabled() {
        return mTestCoverageEnabled;
    }

    /**
     * @param testCoverageEnabled whether test coverage is enabled for this build type
     */
    public void setTestCoverageEnabled(boolean testCoverageEnabled) {
        mTestCoverageEnabled = testCoverageEnabled;
    }

    /**
     * Whether to generate pseudo locale in the APK.
     *
     * <p>If enabled, 2 fake pseudo locales (en-XA and ar-XB) will be added to the APK to help
     * test internationalization support in the app.
     *
     * @return true if pseudo locales are enabled, false otherwise
     */
    @Override
    public boolean isPseudoLocalesEnabled() {
        return mPseudoLocalesEnabled;
    }

    /**
     * @param pseudoLocalesEnabled whether to generate pseudo locale in the APK
     */
    public void setPseudoLocalesEnabled(boolean pseudoLocalesEnabled) {
        mPseudoLocalesEnabled = pseudoLocalesEnabled;
    }

    /**
     * Whether this build type is configured to generate an APK with debuggable native code.
     *
     * @param jniDebugBuild whether jni debug is enabled
     * @return the build type with jni debug enabled
     */
    @NonNull
    public BuildType setJniDebuggable(boolean jniDebugBuild) {
        mJniDebuggable = jniDebugBuild;
        return this;
    }

    /**
     * Whether this build type is configured to generate an APK with debuggable native code.
     *
     * @return if jni debug is enabled
     */
    @Override
    public boolean isJniDebuggable() {
        return mJniDebuggable;
    }

    /**
     * Whether the build type is configured to generate an apk with debuggable RenderScript code.
     *
     * @return if renderscript debug is enabled
     */
    @Override
    public boolean isRenderscriptDebuggable() {
        return mRenderscriptDebuggable;
    }

    /**
     * Whether the build type is configured to generate an apk with debuggable RenderScript code.
     *
     * @param renderscriptDebugBuild whether renderscript debug is enabled
     * @return the build type with renderscript debug enabled
     */
    public BuildType setRenderscriptDebuggable(boolean renderscriptDebugBuild) {
        mRenderscriptDebuggable = renderscriptDebugBuild;
        return this;
    }

    /**
     * Optimization level to use by the renderscript compiler.
     *
     * @return the optimization level
     */
    @Override
    public int getRenderscriptOptimLevel() {
        return mRenderscriptOptimLevel;
    }

    /**
     * Optimization level to use by the renderscript compiler.
     *
     * @param renderscriptOptimLevel the optimization level
     */
    public void setRenderscriptOptimLevel(int renderscriptOptimLevel) {
        mRenderscriptOptimLevel = renderscriptOptimLevel;
    }

    /**
     * Application id suffix applied to this build type.
     *
     * @param applicationIdSuffix the application id suffix
     * @return the build type with the application id suffix
     */
    @NonNull
    public BuildType setApplicationIdSuffix(@Nullable String applicationIdSuffix) {
        mApplicationIdSuffix = applicationIdSuffix;
        return this;
    }

    /**
     * Application id suffix applied to this build type.
     *
     * @return the application id suffix
     */
    @Override
    @Nullable
    public String getApplicationIdSuffix() {
        return mApplicationIdSuffix;
    }

    /**
     * Version name suffix.
     *
     * @param versionNameSuffix the version name suffix
     * @return the build type with the version name suffix
     */
    @NonNull
    public BuildType setVersionNameSuffix(@Nullable String versionNameSuffix) {
        mVersionNameSuffix = versionNameSuffix;
        return this;
    }

    /**
     * Version name suffix.
     *
     * @return the version name suffix
     */
    @Override
    @Nullable
    public String getVersionNameSuffix() {
        return mVersionNameSuffix;
    }

    /**
     * Whether Minify is enabled for this build type.
     *
     * @param enabled whether minify is enabled for this build type
     * @return the build type with minify enabled
     */
    @NonNull
    public BuildType setMinifyEnabled(boolean enabled) {
        mMinifyEnabled = enabled;
        return this;
    }

    /**
     * Whether Minify is enabled for this build type.
     *
     * @return if minify is enabled
     */
    @Override
    public boolean isMinifyEnabled() {
        return mMinifyEnabled;
    }


    /**
     * Whether zipalign is enabled for this build type.
     *
     * @param zipAlign whether zipalign is enabled for this build type
     * @return the build type with zipAlign enabled
     */
    @NonNull
    public BuildType setZipAlignEnabled(boolean zipAlign) {
        mZipAlignEnabled = zipAlign;
        return this;
    }

    /**
     * Whether zipalign is enabled for this build type.
     *
     * @return true if zipalign is enabled, false otherwise
     */
    @Override
    public boolean isZipAlignEnabled() {
        return mZipAlignEnabled;
    }

    /**
     * Sets the signing configuration. e.g.: {@code signingConfig signingConfigs.myConfig}
     *
     * @param signingConfig the signing configuration
     * @return the signing configuration
     */
    @NonNull
    public BuildType setSigningConfig(@Nullable SigningConfig signingConfig) {
        mSigningConfig = signingConfig;
        return this;
    }

    /**
     * Sets the signing configuration. e.g.: {@code signingConfig signingConfigs.myConfig}
     *
     * @return the signing configuration
     */
    @Override
    @Nullable
    public SigningConfig getSigningConfig() {
        return mSigningConfig;
    }

    /**
     * Whether a linked Android Wear app should be embedded in variant using this build type.
     *
     * <p>Wear apps can be linked with the following code:
     *
     * <pre>
     * dependencies {
     *   freeWearApp project(:wear:free') // applies to variant using the free flavor
     *   wearApp project(':wear:base') // applies to all other variants
     * }
     * </pre>
     *
     * @return whether a linked Android Wear app should be embedded in variant using this build type
     */
    @Override
    public boolean isEmbedMicroApp() {
        return mEmbedMicroApp;
    }

    /**
     * @param embedMicroApp whether a linked Android Wear app should be embedded in variant using
     *                      this build type
     */
    public void setEmbedMicroApp(boolean embedMicroApp) {
        mEmbedMicroApp = embedMicroApp;
    }

    /**
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DefaultBuildType buildType = (DefaultBuildType) o;

        return Objects.equal(mName, buildType.mName) &&
                mDebuggable == buildType.mDebuggable &&
                mTestCoverageEnabled == buildType.mTestCoverageEnabled &&
                mJniDebuggable == buildType.mJniDebuggable &&
                mPseudoLocalesEnabled == buildType.mPseudoLocalesEnabled &&
                mRenderscriptDebuggable == buildType.mRenderscriptDebuggable &&
                mRenderscriptOptimLevel == buildType.mRenderscriptOptimLevel &&
                mMinifyEnabled == buildType.mMinifyEnabled &&
                mZipAlignEnabled == buildType.mZipAlignEnabled &&
                mEmbedMicroApp == buildType.mEmbedMicroApp &&
                Objects.equal(mApplicationIdSuffix, buildType.mApplicationIdSuffix) &&
                Objects.equal(mVersionNameSuffix, buildType.mVersionNameSuffix) &&
                Objects.equal(mSigningConfig, buildType.mSigningConfig);
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(
                super.hashCode(),
                mName,
                mDebuggable,
                mTestCoverageEnabled,
                mJniDebuggable,
                mPseudoLocalesEnabled,
                mRenderscriptDebuggable,
                mRenderscriptOptimLevel,
                mApplicationIdSuffix,
                mVersionNameSuffix,
                mMinifyEnabled,
                mZipAlignEnabled,
                mSigningConfig,
                mEmbedMicroApp);
    }

    /**
     * @return a string suitable for debugging purposes.
     */
    @Override
    @NonNull
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", mName)
                .add("debuggable", mDebuggable)
                .add("testCoverageEnabled", mTestCoverageEnabled)
                .add("jniDebuggable", mJniDebuggable)
                .add("pseudoLocalesEnabled", mPseudoLocalesEnabled)
                .add("renderscriptDebuggable", mRenderscriptDebuggable)
                .add("renderscriptOptimLevel", mRenderscriptOptimLevel)
                .add("applicationIdSuffix", mApplicationIdSuffix)
                .add("versionNameSuffix", mVersionNameSuffix)
                .add("minifyEnabled", mMinifyEnabled)
                .add("zipAlignEnabled", mZipAlignEnabled)
                .add("signingConfig", mSigningConfig)
                .add("embedMicroApp", mEmbedMicroApp)
                .add("mBuildConfigFields", getBuildConfigFields())
                .add("mResValues", getResValues())
                .add("mProguardFiles", getProguardFiles())
                .add("mConsumerProguardFiles", getConsumerProguardFiles())
                .add("mManifestPlaceholders", getManifestPlaceholders())
                .toString();
    }
}
