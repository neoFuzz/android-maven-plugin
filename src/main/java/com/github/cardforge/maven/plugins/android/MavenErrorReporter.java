/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.github.cardforge.maven.plugins.android;

import com.android.annotations.NonNull;
import com.android.builder.core.ErrorReporter;
import com.android.builder.model.SyncIssue;
import com.android.ide.common.blame.Message;
import com.android.ide.common.blame.MessageReceiver;
import com.android.utils.ILogger;

import java.util.Arrays;
import java.util.List;

/**
 * Direct implementation of the MessageReceiver interface to handle errors from the Maven plugin.
 *
 * @author kedzie
 */
public class MavenErrorReporter extends ErrorReporter implements MessageReceiver {

    private final ILogger logger;

    @NonNull
    private final EvaluationMode mMode;

    /**
     * @param logger the logger to be used for logging errors
     * @param mMode  the evaluation mode for the error reporter
     */
    public MavenErrorReporter(ILogger logger, @NonNull EvaluationMode mMode) {
        super(mMode);
        this.logger = logger;
        this.mMode = mMode;
    }

    @NonNull
    @Override
    public EvaluationMode getMode() {
        return mMode;
    }

    /**
     * @param data the data associated with the error, e.g. a file path
     * @param type the type of error
     * @param msg  the error message
     * @return a new SyncIssue object representing the error
     */
    @NonNull
    public SyncIssue handleSyncError(@NonNull String data, int type, @NonNull String msg) {
        return new SyncIssueImpl(0, type, data, msg);
    }

    /**
     * @param data the data to be received
     * @param type the type of the issue
     * @param i1   the severity of the issue
     * @param msg  the message to be received
     * @return a new SyncIssue object with the given parameters
     */
    public SyncIssue handleIssue(String data, int type, int i1, String msg) {
        logger.info("Sync Error.  Data: " + data + "\tmsg: " + msg);
        return new SyncIssueImpl(0, type, data, msg);
    }

    /**
     * @param message the message to be received
     */
    public void receiveMessage(@NonNull Message message) {
        logger.info(message.toString());
    }

}

/**
 * Implementation of the SyncIssue interface to represent a sync issue.
 */
class SyncIssueImpl implements SyncIssue {
    private int severity;
    private int type;
    private String data;
    private String message;

    /**
     * @param severity the severity of the issue
     * @param type     the type of the issue
     * @param data     the data associated with the issue
     * @param message  the message describing the issue
     */
    SyncIssueImpl(int severity, int type, String data, String message) {
        this.severity = severity;
        this.type = type;
        this.data = data;
        this.message = message;
    }

    @Override
    public int getSeverity() {
        return severity;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    @NonNull
    public String getMessage() {
        return message;
    }

    @Override
    public List<String> getMultiLineMessage() {
        return Arrays.asList(message.split("\\s"));
    }
}
