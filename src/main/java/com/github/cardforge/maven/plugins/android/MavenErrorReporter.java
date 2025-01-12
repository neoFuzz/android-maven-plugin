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

    public MavenErrorReporter(ILogger logger, @NonNull EvaluationMode mMode) {
        super(mMode);
        this.logger = logger;
        this.mMode = mMode;
    }

    @NonNull
    public EvaluationMode getMode() {
        return mMode;
    }

    @NonNull
    public SyncIssue handleSyncError(@NonNull String data, int type, @NonNull String msg) {
        return new SyncIssueImpl(0, type, data, msg);
    }

    public SyncIssue handleIssue(String data, int type, int i1, String msg) {
        logger.info("Sync Error.  Data: " + data + "\tmsg: " + msg);
        return new SyncIssueImpl(0, type, data, msg);
    }

    public boolean hasSyncIssue(int i) {
        return false;
    }

    public void receiveMessage(Message message) {
        logger.info(message.toString());
    }

}

class SyncIssueImpl implements SyncIssue {
    private int severity;
    private int type;
    private String data;
    private String message;

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
    public String getMessage() {
        return message;
    }

    @Override
    public List<String> getMultiLineMessage() {
        return Arrays.asList(message.split("\\s"));
    }
}
