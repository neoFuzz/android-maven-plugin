/*
 * Copyright (C) 2009 Jayway AB
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
package com.github.cardforge.maven.plugins.android.asm;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.objectweb.asm.ClassReader;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Finds Android instrumentation test classes to be run by InstrumentationTestRunner
 * in a directory of compiled Java classes.
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidTestFinder {
    /**
     * The packages to look for in the class files.
     */
    private static final String[] TEST_PACKAGES = {"junit/framework/", "android/test/"};

    /**
     * Private constructor to prevent instantiation.
     */
    private AndroidTestFinder() {
        // hidden
    }

    /**
     * @param classesBaseDirectory class files base directory
     * @return true if the directory contains Android tests, false otherwise
     * @throws MojoExecutionException if the classesBaseDirectory is null or not a directory
     */
    public static boolean containsAndroidTests(File classesBaseDirectory) throws MojoExecutionException {

        if (classesBaseDirectory == null || !classesBaseDirectory.isDirectory()) {
            throw new IllegalArgumentException("classesBaseDirectory must be a valid directory!");
        }

        final List<File> classFiles = findEligibleClassFiles(classesBaseDirectory);
        final DescendantFinder descendantFinder = new DescendantFinder(TEST_PACKAGES);
        final AnnotatedFinder annotationFinder = new AnnotatedFinder(TEST_PACKAGES);

        for (File classFile : classFiles) {
            ClassReader classReader;
            try (FileInputStream inputStream = new FileInputStream(classFile)) {
                classReader = new ClassReader(new java.io.BufferedInputStream(inputStream));
                classReader.accept(descendantFinder,
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
                classReader.accept(annotationFinder,
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
            } catch (IOException e) {
                throw new MojoExecutionException("Error reading " + classFile + ".\nCould not determine whether it "
                        + "contains tests. Please specify with plugin config parameter "
                        + "<enableIntegrationTest>true|false</enableIntegrationTest>.", e);
            }
        }

        return descendantFinder.isDescendantFound() || annotationFinder.isDescendantFound();
    }

    /**
     * @param classesBaseDirectory the directory to search for class files
     * @return a list of class files found in the directory and its subdirectories
     */
    @Nonnull
    private static List<File> findEligibleClassFiles(File classesBaseDirectory) {
        final List<File> classFiles = new LinkedList<>();
        final DirectoryWalker walker = new DirectoryWalker();
        walker.setBaseDir(classesBaseDirectory);
        walker.addSCMExcludes();
        walker.addInclude("**/*.class");
        walker.addDirectoryWalkListener(new DirectoryWalkListener() {
            public void directoryWalkStarting(File basedir) {
                // quick implementation
            }

            public void directoryWalkStep(int percentage, File file) {
                classFiles.add(file);
            }

            public void directoryWalkFinished() {
                // quick implementation
            }

            public void debug(String message) {
                // quick implementation
            }
        });
        walker.scan();
        return classFiles;
    }
}
