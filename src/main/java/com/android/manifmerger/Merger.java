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

package com.android.manifmerger;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.utils.ILogger;
import com.android.utils.StdLogger;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Command line interface to the {@link ManifestMerger2}
 */
public class Merger {

    private static final Logger log = LoggerFactory.getLogger(Merger.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            System.exit(new Merger().process(args));
        } catch (FileNotFoundException e) {
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Display the usage for this tool
     */
    public static void usage() {
        String sb = "Android Manifest Merger Tool Version 2\n" +
                "Usage:\n" +
                "Merger --main mainAndroidManifest.xml\n" +
                "\t--log [VERBOSE, INFO, WARNING, ERROR]\n" +
                "\t--libs [path separated list of lib's manifests]\n" +
                "\t--overlays [path separated list of overlay's manifests]\n" +
                "\t--property [" +
                Joiner.on(" | ").join(ManifestSystemProperty.values()) +
                "=value]\n" +
                "\t--placeholder [name=value]\n" +
                "\t--out [path of the output file]";
        System.out.println(sb); // NOSONAR - command line output
    }

    private static void tryWriteFile(@NonNull File outFile, String mergedDocument) {
        try {
            java.nio.file.Files.writeString(outFile.toPath(), mergedDocument, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param args the command line arguments
     * @return 0 if all went well, non-zero otherwise
     * @throws FileNotFoundException if the main manifest file does not exist
     */
    public int process(String[] args) throws FileNotFoundException {

        Iterator<String> arguments = Arrays.asList(args).iterator();
        // first pass to get all mandatory parameters.
        String mainManifest = null;
        StdLogger.Level logLevel = StdLogger.Level.INFO;
        ILogger logger = new StdLogger(logLevel);
        while (arguments.hasNext()) {
            String selector = arguments.next();
            if (!selector.startsWith("--")) {
                logger.error(null /* throwable */,
                        "Invalid parameter " + selector + ", expected a command switch");
                return 1;
            }
            if ("--usage".equals(selector)) {
                usage();
                return 0;
            }
            if (!arguments.hasNext()) {
                logger.error(null /* throwable */,
                        "Command switch " + selector + " has no value associated");
                return 1;
            }
            String value = arguments.next();

            if ("--main".equals(selector)) {
                mainManifest = value;
            }
            if ("--log".equals(selector)) {
                logLevel = StdLogger.Level.valueOf(value);
            }
        }

        if (mainManifest == null) {
            logger.error(null /* throwable */,
                    "--main command switch not provided.");
            return 1;
        }

        // recreate the logger with the provided log level for the rest of the processing.
        logger = createLogger(logLevel);
        File mainManifestFile = checkPath(mainManifest);
        ManifestMerger2.Invoker invoker = createInvoker(
                mainManifestFile, logger);

        // second pass, get optional parameters and store them in the invoker.
        arguments = Arrays.asList(args).iterator();
        File outFile = null;

        // first pass to get all mandatory parameters.
        while (arguments.hasNext()) {
            String selector = arguments.next();
            String value = arguments.next();
            if (Strings.isNullOrEmpty(value)) {
                logger.error(null /* throwable */,
                        "Empty value for switch " + selector);
                return 1;
            }
            if ("--libs".equals(selector)) {
                StringTokenizer stringTokenizer = new StringTokenizer(value, File.pathSeparator);
                while (stringTokenizer.hasMoreTokens()) {
                    File library = checkPath(stringTokenizer.nextToken());
                    invoker.addLibraryManifest(library);
                }
            }
            if ("--overlays".equals(selector)) {
                StringTokenizer stringTokenizer = new StringTokenizer(value, File.pathSeparator);
                while (stringTokenizer.hasMoreTokens()) {
                    File library = checkPath(stringTokenizer.nextToken());
                    invoker.addFlavorAndBuildTypeManifest(library);
                }

            }
            if ("--property".equals(selector)) {
                if (!value.contains("=")) {
                    logger.error(null /* throwable */,
                            "Invalid property setting, should be NAME=VALUE format");
                    return 1;
                }
                try {
                    ManifestSystemProperty manifestSystemProperty = ManifestSystemProperty
                            .valueOf(value.substring(0, value.indexOf('='))
                                    .toUpperCase(Locale.ENGLISH));
                    invoker.setOverride(manifestSystemProperty, value.substring(value.indexOf('=') + 1));
                } catch (IllegalArgumentException e) {
                    logger.error(e, "Invalid property name " + value.substring(0, value.indexOf('='))
                            + ", allowed properties are : " + Joiner
                            .on(',').join(ManifestSystemProperty.values()));
                    return 1;
                }
            }
            if ("--placeholder".equals(selector)) {
                if (!value.contains("=")) {
                    logger.error(null /* throwable */,
                            "Invalid placeholder setting, should be NAME=VALUE format");
                    return 1;
                }
                invoker.setPlaceHolderValue(value.substring(0, value.indexOf('=')),
                        value.substring(value.indexOf('=') + 1));
            }
            if ("--out".equals(selector)) {
                outFile = new File(value);
            }
        }
        try {
            MergingReport merge = invoker.merge();
            if (merge.getResult().isSuccess()) {
                String mergedDocument = merge.getMergedDocument(MergingReport.MergedManifestKind.MERGED);
                if (mergedDocument != null) {
                    if (outFile != null) {
                        tryWriteFile(outFile, mergedDocument);
                    } else {
                        logger.info("Merged manifest:\n%s", mergedDocument);
                    }
                }
            } else {
                for (MergingReport.Record rec : merge.getLoggingRecords()) {
                    logger.error(null, rec.getMessage());
                }
            }
        } catch (ManifestMerger2.MergeFailureException e) {
            logger.error(e, "Exception while merging manifests");
            return 1;
        }
        return 0;
    }

    /**
     * @param mainManifestFile the main manifest file to merge
     * @param logger           the logger to use for logging
     * @return a new invoker instance for merging manifests
     */
    protected ManifestMerger2.Invoker createInvoker(@NonNull File mainManifestFile,
                                                    @NonNull ILogger logger) {
        return ManifestMerger2.newMerger(mainManifestFile, logger, ManifestMerger2.MergeType.APPLICATION);
    }

    /**
     * @param path the path to check for existence
     * @return the file corresponding to the path
     * @throws FileNotFoundException if the file does not exist
     */
    @VisibleForTesting
    protected File checkPath(@NonNull String path) throws FileNotFoundException {
        @NonNull File file = new File(path);
        if (!file.exists()) {
            log.error("{} does not exist", path);
            throw new FileNotFoundException(path);
        }
        return file;
    }

    /**
     * @param level the log level to use for the logger
     * @return a new logger instance with the specified log level
     */
    @VisibleForTesting
    protected ILogger createLogger(@NonNull StdLogger.Level level) {
        return new StdLogger(level);
    }

}
