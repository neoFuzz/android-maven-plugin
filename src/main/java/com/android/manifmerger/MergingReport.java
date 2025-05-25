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

package com.android.manifmerger;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.concurrency.Immutable;
import com.android.ide.common.blame.SourceFile;
import com.android.ide.common.blame.SourceFilePosition;
import com.android.ide.common.blame.SourcePosition;
import com.android.utils.ILogger;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Contains the result of 2 files merging.
 * <p>
 * TODO: more work necessary, this is pretty raw as it stands.
 */
@Immutable
public class MergingReport {

    /**
     * the merged documents, keyed by their state.
     */
    @NonNull
    private final Map<MergedManifestKind, String> mergedDocuments;
    /**
     * the merged xml documents, keyed by their state.
     */
    @NonNull
    private final Map<MergedManifestKind, XmlDocument> mergedXmlDocuments;
    /**
     * the result of the merging process.
     */
    @NonNull
    private final Result result;
    /**
     * list of logging events, ordered by their recording time.
     */
    @NonNull
    private final ImmutableList<Record> records;
    /**
     * the actions that were performed during the merging process.
     */
    @NonNull
    private final Actions actions;

    /**
     * @param mergedDocuments    the merged documents, keyed by their state.
     * @param mergedXmlDocuments the merged xml documents, keyed by their state.
     * @param result             the result of the merging process.
     * @param records            the list of logging events, ordered by their recording time.
     * @param actions            the actions that were performed during the merging process.
     */
    private MergingReport(@NonNull Map<MergedManifestKind, String> mergedDocuments,
                          @NonNull Map<MergedManifestKind, XmlDocument> mergedXmlDocuments,
                          @NonNull Result result,
                          @NonNull ImmutableList<Record> records,
                          @NonNull Actions actions) {
        this.mergedDocuments = mergedDocuments;
        this.mergedXmlDocuments = mergedXmlDocuments;
        this.result = result;
        this.records = records;

        this.actions = actions;

    }

    /**
     * dumps all logging records to a logger.
     *
     * @param logger the logger to dump the records to.
     */
    public void log(@NonNull ILogger logger) {
        for (Record rec : records) {
            switch (rec.mSeverity) {
                case WARNING:
                    logger.warning(rec.toString());
                    break;
                case ERROR:
                    logger.error(null /* throwable */, rec.toString());
                    break;
                case INFO:
                    logger.verbose(rec.toString());
                    break;
                default:
                    logger.error(null /* throwable */, "Unhandled record type " + rec.mSeverity);
            }
        }
        actions.log(logger);

        if (!result.isSuccess()) {
            logger.warning("\nSee http://g.co/androidstudio/manifest-merger for more information about the manifest merger.\n");
        }
    }

    /**
     * @param state the state of the merged document
     * @return the merged document, or null if not found
     */
    @Nullable
    public String getMergedDocument(@NonNull MergedManifestKind state) {
        return mergedDocuments.get(state);
    }

    /**
     * @param state the state of the merged document
     * @return the merged document, or null if not found
     */
    @Nullable
    public XmlDocument getMergedXmlDocument(@NonNull MergedManifestKind state) {
        return mergedXmlDocuments.get(state);
    }

    /**
     * @return the result of the merging process.
     */
    @NonNull
    public Result getResult() {
        return result;
    }

    /**
     * @return the logging records, ordered by their recording time.
     */
    @NonNull
    public ImmutableList<Record> getLoggingRecords() {
        return records;
    }

    /**
     * @return the actions that were performed during the merging process.
     */
    @NonNull
    public Actions getActions() {
        return actions;
    }

    /**
     * @return a string describing the result of the merging process.
     */
    @NonNull
    public String getReportString() {
        return switch (result) {
            case SUCCESS -> "Manifest merger executed successfully";
            case WARNING -> records.size() > 1
                    ? "Manifest merger exited with warnings, see logs"
                    : "Manifest merger warning : " + records.get(0).mLog;
            case ERROR -> records.size() > 1
                    ? "Manifest merger failed with multiple errors, see logs"
                    : "Manifest merger failed : " + records.get(0).mLog;
            default -> "Manifest merger returned an invalid result " + result;
        };
    }

    /**
     * Enum representing the different states of a merged manifest.
     */
    public enum MergedManifestKind {
        /**
         * Merged manifest file
         */
        MERGED,

        /**
         * Merged manifest file with Instant Run related decorations.
         */
        INSTANT_RUN,

        /**
         * Merged manifest file with unresolved placeholders encoded to be AAPT friendly.
         */
        AAPT_SAFE,

        /**
         * Blame file for merged manifest file.
         */
        BLAME,
    }

    /**
     * Overall result of the merging process.
     */
    public enum Result {
        /**
         * Merging completed successfully.
         */
        SUCCESS,
        /**
         * Merging completed with warnings.
         */
        WARNING,
        /**
         * Merging completed with errors.
         */
        ERROR;

        /**
         * @return true if the merging completed successfully, false otherwise.
         */
        public boolean isSuccess() {
            return this == SUCCESS || this == WARNING;
        }

    }

    /**
     * Log record. This is used to give users some information about what is happening and
     * what might have gone wrong.
     */
    public static class Record {
        /**
         * the severity of the problem.
         */
        @NonNull
        private final Severity mSeverity;
        /**
         * the log message.
         */
        @NonNull
        private final String mLog;
        /**
         * the location of the log in the source file.
         */
        @NonNull
        private final SourceFilePosition mSourceLocation;

        /**
         * @param sourceLocation the location of the log in the source file.
         * @param severity       the severity of the problem
         * @param mLog           the log message
         */
        private Record(
                @NonNull SourceFilePosition sourceLocation,
                @NonNull Severity severity,
                @NonNull String mLog) {
            this.mSourceLocation = sourceLocation;
            this.mSeverity = severity;
            this.mLog = mLog;
        }

        /**
         * @return the message from the log.
         */
        @NonNull
        public String getMessage() {
            return mLog;
        }

        @NonNull
        @Override
        public String toString() {
            return mSourceLocation // needs short string.
                    + " "
                    + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, mSeverity.toString())
                    + ":\n\t"
                    + mLog;
        }

        /**
         * Severity of the log message.
         */
        public enum Severity {
            /**
             * Warning message, merging can continue.
             */
            WARNING,
            /**
             * Error message, merging cannot continue.
             */
            ERROR,
            /**
             * Informational message, not an error or warning.
             */
            INFO
        }
    }

    /**
     * This builder is used to accumulate logging, action recording and intermediary results as
     * well as final result of the merging activity.
     * <p>
     * Once the merging is finished, the {@link #build()} is called to return an immutable version
     * of itself with all the logging, action recordings and xml files obtainable.
     */
    public static class Builder {

        /**
         * Logger to use to log messages.
         */
        @NonNull
        private final ILogger mLogger;
        /**
         * Manifest merger, used to retrieve the merged manifest package name.
         */
        @Nullable
        private final ManifestMerger2 mManifestMerger;
        /**
         * Merged documents, keyed by their state.
         */
        private final Map<MergedManifestKind, String> mergedDocuments =
                new EnumMap<>(MergedManifestKind.class);
        /**
         * Merged xml documents, keyed by their state.
         */
        private final Map<MergedManifestKind, XmlDocument> mergedXmlDocuments =
                new EnumMap<>(MergedManifestKind.class);
        /**
         * Record builder instance
         */
        @NonNull
        private final ImmutableList.Builder<Record> mRecordBuilder = new ImmutableList.Builder<>();
        /**
         * List of intermediary stages if
         * {@link com.android.manifmerger.ManifestMerger2.Invoker.Feature#KEEP_INTERMEDIARY_STAGES}
         * is set.
         */
        @NonNull
        private final ImmutableList.Builder<String> mIntermediaryStages = new ImmutableList.Builder<>();
        /**
         * Action recorder instance
         */
        @NonNull
        private final ActionRecorder mActionRecorder = new ActionRecorder();
        /**
         * Overall result of the merging process.
         */
        private boolean mHasWarnings = false;
        /**
         * Overall result of the merging process.
         */
        private boolean mHasErrors = false;
        /**
         * Package name of the merged manifest.
         */
        private String packageName;

        /**
         * @param logger         the logger to use to log messages.
         * @param manifestMerger the manifest merger, used to retrieve the merged manifest package name.
         */
        Builder(@NonNull ILogger logger, @Nullable ManifestMerger2 manifestMerger) {
            mLogger = logger;
            mManifestMerger = manifestMerger;
        }

        /**
         * @param mergedManifestKind the state of the merged document
         * @param mergedDocument     the merged document
         * @return the builder itself.
         */
        Builder setMergedDocument(@NonNull MergedManifestKind mergedManifestKind, @NonNull String mergedDocument) {
            this.mergedDocuments.put(mergedManifestKind, mergedDocument);
            return this;
        }

        /**
         * @param mergedManifestKind the state of the merged document
         * @param mergedDocument     the merged document
         * @return the builder itself.
         */
        Builder setMergedXmlDocument(@NonNull MergedManifestKind mergedManifestKind, @NonNull XmlDocument mergedDocument) {
            this.mergedXmlDocuments.put(mergedManifestKind, mergedDocument);
            return this;
        }

        /**
         * @param sourceFile the location of the log in the source file.
         * @param severity   the severity of the problem
         * @param message    the log message
         * @return the builder itself.
         */
        @NonNull
        Builder addMessage(@NonNull SourceFile sourceFile,
                           @NonNull Record.Severity severity,
                           @NonNull String message) {
            return addMessage(
                    new SourceFilePosition(sourceFile, SourcePosition.UNKNOWN),
                    severity,
                    message);
        }

        /**
         * @param sourceFilePosition the location of the log in the source file.
         * @param severity           the severity of the problem
         * @param message            the log message
         * @return the builder itself.
         */
        @NonNull
        Builder addMessage(@NonNull SourceFilePosition sourceFilePosition,
                           @NonNull Record.Severity severity,
                           @NonNull String message) {
            if (severity == Record.Severity.ERROR) {
                mHasErrors = true;
            }
            if (severity == Record.Severity.WARNING) {
                mHasWarnings = true;
            }
            mRecordBuilder.add(new Record(sourceFilePosition, severity, message));
            return this;
        }

        /**
         * @param xml the intermediary stage of the merging process.
         * @return the builder itself.
         */
        @NonNull
        Builder addMergingStage(@NonNull String xml) {
            mIntermediaryStages.add(xml);
            return this;
        }

        /**
         * Returns true if some fatal errors were reported.
         *
         * @return true if some fatal errors were reported.
         */
        boolean hasErrors() {
            return mHasErrors;
        }

        /**
         * @return the action recorder used to record actions performed during the merging process.
         */
        @NonNull
        ActionRecorder getActionRecorder() {
            return mActionRecorder;
        }

        /**
         * @return an immutable version of the merging report.
         */
        @NonNull
        MergingReport build() {
            Result result;
            if (mHasErrors) {
                result = Result.ERROR;
            } else if (mHasWarnings) {
                result = Result.WARNING;
            } else {
                result = Result.SUCCESS;
            }

            return new MergingReport(
                    mergedDocuments,
                    mergedXmlDocuments,
                    result,
                    mRecordBuilder.build(),
                    mActionRecorder.build()
            );
        }

        /**
         * @return the manifest merger used to create the merged manifest document
         */
        @Nullable
        public ManifestMerger2 getManifestMerger() {
            return mManifestMerger;
        }

        /**
         * @return the logger used to log messages
         */
        @NonNull
        public ILogger getLogger() {
            return mLogger;
        }

        /**
         * @param document the merged manifest document
         * @return the blame log for the merged manifest
         * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the configuration requested.
         * @throws SAXException                 if any parse errors occur.
         * @throws IOException                  if an I/O error occurs.
         */
        public String blame(XmlDocument document)
                throws ParserConfigurationException, SAXException, IOException {
            return mActionRecorder.build().blame(document);
        }

        /**
         * @param finalPackageName the final package name of the merged manifest.
         */
        public void setFinalPackageName(String finalPackageName) {
            this.packageName = finalPackageName;
        }
    }
}
