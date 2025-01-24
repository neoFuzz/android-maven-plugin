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

package com.android.ide.common.process;

import com.android.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Partial implementation of ProcessOutputHandler that creates a ProcessOutput that caches the
 * output in a ByteArrayOutputStream.
 * <p>
 * This does not do anything with it, since it does not implement
 * {@link ProcessOutputHandler#handleOutput(ProcessOutput)}
 */
public abstract class BaseProcessOutputHandler implements ProcessOutputHandler {

    /**
     * Constructor for BaseProcessOutputHandler.
     */
    protected BaseProcessOutputHandler() {
    }

    /**
     * @param stream the stream to convert to a string
     * @return the string value of the stream
     * @throws ProcessException if the stream is not a valid string
     */
    private static String getString(@NonNull ByteArrayOutputStream stream) throws ProcessException {
        try {
            return stream.toString(Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
            throw new ProcessException(e);
        }
    }

    /**
     * @return a new ProcessOutput instance
     */
    @NonNull
    @Override
    public ProcessOutput createOutput() {
        return new BaseProcessOutput();
    }

    /**
     * Implementation of ProcessOutput that caches the output in a ByteArrayOutputStream.
     * <p>
     * This does not do anything with it, since it does not implement
     * {@link ProcessOutputHandler#handleOutput(ProcessOutput)}
     */
    public static final class BaseProcessOutput implements ProcessOutput {
        private final ByteArrayOutputStream mStandardOutput = new ByteArrayOutputStream();
        private final ByteArrayOutputStream mErrorOutput = new ByteArrayOutputStream();

        /**
         * @return the standard output as a string
         */
        @NonNull
        @Override
        public OutputStream getStandardOutput() {
            return mStandardOutput;
        }

        /**
         * @return the error output as a string
         */
        @NonNull
        @Override
        public OutputStream getErrorOutput() {
            return mErrorOutput;
        }

        /**
         * @return the standard output as a string
         * @throws ProcessException if the standard output is not a valid string
         */
        @NonNull
        public String getStandardOutputAsString() throws ProcessException {
            return getString(mStandardOutput);
        }
    }
}
