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
package com.android.utils;

import com.android.annotations.NonNull;
import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utilities for file operations.
 */
public class FileUtils {
    /**
     * A function that returns the name of a file.
     */
    private static final Function<File, String> GET_NAME = File::getName;

    /**
     * This is a utility class, so it should not be instantiated.
     */
    private FileUtils() {
        // empty
    }

    /**
     * @param folder The folder to delete. Must exist and be a directory.
     * @throws IOException If an error occurs while deleting the folder.
     */
    public static void deleteFolder(@NonNull final File folder) throws IOException {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) { // i.e. is a directory.
            for (final File file : files) {
                deleteFolder(file);
            }
        }
        if (!folder.delete()) {
            throw new IOException(String.format("Could not delete folder %s", folder));
        }
    }

    /**
     * @param folder The folder to empty. Must exist and be a directory.
     * @throws IOException If an error occurs while emptying the folder.
     */
    public static void emptyFolder(final File folder) throws IOException {
        deleteFolder(folder);
        if (!folder.mkdirs()) {
            throw new IOException(String.format("Could not create empty folder %s", folder));
        }
    }

    /**
     * @param from The file to copy.
     * @param to   The destination to copy to. If the file is a directory, the file's name will be
     *             appended to the destination.
     * @throws IOException If an error occurs while copying the file.
     */
    public static void copyFile(@NonNull File from, File to) throws IOException {
        to = new File(to, from.getName());
        if (from.isDirectory()) {
            if (!to.exists() && !to.mkdirs()) {
                throw new IOException(String.format("Could not create directory %s", to));
            }

            File[] children = from.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyFile(child, to);
                }
            }
        } else if (from.isFile()) {
            Files.copy(from, to);
        }
    }

    /**
     * @param dir   The directory to join to.
     * @param paths The paths to join.
     * @return A new file that is the result of joining the paths to the directory.
     */
    @NonNull
    public static File join(File dir, String... paths) {
        return new File(dir, Joiner.on(File.separatorChar).join(paths));
    }

    /**
     * @param file The file to get the relative path for.
     * @param dir  The directory to get the relative path relative to.
     * @return The relative path of the file with respect to the directory.
     */
    public static String relativePath(@NonNull File file, @NonNull File dir) {
        checkArgument(file.isFile(), "%s is not a file.", file.getPath());
        checkArgument(dir.isDirectory(), "%s is not a directory.", dir.getPath());
        return dir.toURI().relativize(file.toURI()).getPath();
    }

    /**
     * @param file The file to hash.
     * @return The SHA-1 of the file's content.
     * @throws IOException If an error occurs while reading the file.
     */
    @NonNull
    public static String sha1(@NonNull File file) throws IOException {
        return Hashing.sha1().hashBytes(Files.toByteArray(file)).toString();
    }
}