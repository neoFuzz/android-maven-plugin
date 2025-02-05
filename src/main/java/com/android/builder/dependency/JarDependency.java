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

package com.android.builder.dependency;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.MavenCoordinates;
import com.google.common.base.Preconditions;

import java.io.File;

/**
 * Represents a Jar dependency. This could be the output of a Java project.
 * <p>
 * This is not meant to include transitive dependencies, as there's no need to record this
 * information when building.
 */
public class JarDependency {

    /**
     * The jar file for this dependency
     */
    @NonNull
    private final File mJarFile;

    /**
     * whether the dependency is compiled or not
     */
    private final boolean mCompiled;
    /**
     * whether the dependency is packaged or not
     */
    private final boolean mPackaged;
    /**
     * whether the dependency is proguarded or not
     */
    private final boolean mProguarded;

    /**
     * if the dependency is a subproject, then the project path
     */
    @Nullable
    private final String mProjectPath;

    /**
     * the resolved maven coordinates for this dependency
     */
    @Nullable
    private final MavenCoordinates mResolvedCoordinates;

    /**
     * @param jarFile             the jar file for this dependency
     * @param compiled            whether the dependency is compiled
     * @param packaged            whether the dependency is packaged
     * @param proguarded          whether the dependency is proguarded
     * @param resolvedCoordinates the resolved maven coordinates for this dependency
     * @param projectPath         the project path for this dependency, if any
     */
    public JarDependency(
            @NonNull File jarFile,
            boolean compiled,
            boolean packaged,
            boolean proguarded,
            @Nullable MavenCoordinates resolvedCoordinates,
            @Nullable String projectPath) {
        Preconditions.checkNotNull(jarFile);
        mJarFile = jarFile;
        mCompiled = compiled;
        mPackaged = packaged;
        mProguarded = proguarded;
        mResolvedCoordinates = resolvedCoordinates;
        mProjectPath = projectPath;
    }

    /**
     * @param jarFile             the jar file for this dependency
     * @param compiled            whether the dependency is compiled
     * @param packaged            whether the dependency is packaged
     * @param resolvedCoordinates the resolved maven coordinates for this dependency
     * @param projectPath         the project path for this dependency, if any
     */
    public JarDependency(
            @NonNull File jarFile,
            boolean compiled,
            boolean packaged,
            @Nullable MavenCoordinates resolvedCoordinates,
            @Nullable String projectPath) {
        this(jarFile, compiled, packaged, true, resolvedCoordinates, projectPath);
    }

    /**
     * @return the jar file for this dependency
     */
    @NonNull
    public File getJarFile() {
        return mJarFile;
    }

    /**
     * @return the compiled status of this dependency
     */
    public boolean isCompiled() {
        return mCompiled;
    }

    /**
     * @return the packaged status of this dependency
     */
    public boolean isPackaged() {
        return mPackaged;
    }

    /**
     * @return a string representation of this dependency
     */
    @Override
    public String toString() {
        return "JarDependency{" +
                "mJarFile=" + mJarFile +
                ", mCompiled=" + mCompiled +
                ", mPackaged=" + mPackaged +
                ", mProguarded=" + mProguarded +
                ", mResolvedCoordinates=" + mResolvedCoordinates +
                '}';
    }
}
