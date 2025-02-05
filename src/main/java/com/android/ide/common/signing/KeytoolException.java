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

package com.android.ide.common.signing;

import java.io.Serial;

/**
 * Exception thrown when keytool fails.
 */
public class KeytoolException extends Exception {
    /**
     * default serial uid
     */
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * The java home used to run keytool
     */
    private String mJavaHome = null;
    /**
     * The command line used to run keytool
     */
    private String mCommandLine = null;

    /**
     * @param message the message to display
     */
    KeytoolException(String message) {
        super(message);
    }

    /**
     * @param message the message to display
     * @param t       the cause of the exception
     */
    KeytoolException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * @param message     the message to display
     * @param javaHome    the java home used to run keytool
     * @param commandLine the command line used to run keytool
     */
    KeytoolException(String message, String javaHome, String commandLine) {
        super(message);

        mJavaHome = javaHome;
        mCommandLine = commandLine;
    }

    /**
     * @return the java home used to run keytool
     */
    public String getJavaHome() {
        return mJavaHome;
    }

    /**
     * @return the command line used to run keytool
     */
    public String getCommandLine() {
        return mCommandLine;
    }

}
