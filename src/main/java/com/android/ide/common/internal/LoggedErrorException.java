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

package com.android.ide.common.internal;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.util.List;

/**
 * Exception thrown when a command line operation fails and the output is logged.
 */
public class LoggedErrorException extends Exception {

    /**
     * The error code returned by the command line operation.
     */
    private final int mCmdLineError;
    /**
     * The output of the command line operation.
     */
    private final List<String> mOutput;
    /**
     * The command line that was executed.
     */
    private final String mCmdLine;

    /**
     * @param error   The error code returned by the command line operation.
     * @param output  The output of the command line operation.
     * @param cmdLine The command line that was executed.
     */
    public LoggedErrorException(
            int error,
            @NonNull List<String> output,
            @Nullable String cmdLine) {
        mCmdLineError = error;
        mOutput = output;
        mCmdLine = cmdLine;
    }

    /**
     * @param output The output of the command line operation.
     */
    public LoggedErrorException(@NonNull List<String> output) {
        this(0, output, null);
    }

    /**
     * @return The error code returned by the command line operation.
     */
    public int getCmdLineError() {
        return mCmdLineError;
    }

    /**
     * @return The output of the command line operation.
     */
    @NonNull
    public List<String> getOutput() {
        return mOutput;
    }

    /**
     * @return The command line that was executed.
     */
    public String getCmdLine() {
        return mCmdLine;
    }

    /**
     * @return A string representation of the exception, including the command line and the output.
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to run command:\n\t").append(mCmdLine).append('\n');
        sb.append("Error Code:\n\t").append(mCmdLineError).append('\n');
        if (!mOutput.isEmpty()) {
            sb.append("Output:\n");
            for (String line : mOutput) {
                sb.append('\t').append(line).append('\n');
            }
        }

        return sb.toString();
    }
}
