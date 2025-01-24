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
package com.github.cardforge.maven.plugins.android;

/**
 * Can sign Android applications (apk's).
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidSigner {

    /**
     * @param debug the debug mode to use when signing the apk. Must be 'true', 'false', 'both' or 'auto'.
     */
    private final Debug debug;

    /**
     * @param debug the debug mode to use when signing the apk. Must be 'true', 'false', 'both' or 'auto'.
     */
    public AndroidSigner(String debug) {
        if (debug == null) {
            throw new IllegalArgumentException("android.sign.debug must be 'true', 'false', 'both' or 'auto'.");
        }
        try {
            this.debug = Debug.valueOf(debug.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("android.sign.debug must be 'true', 'false', 'both' or 'auto'.", e);
        }
    }

    /**
     * @param debug the debug mode to use when signing the apk.
     */
    public AndroidSigner(Debug debug) {
        this.debug = debug;
    }

    /**
     * @return true if the apk should be signed with debug keystore.
     */
    public boolean isSignWithDebugKeyStore() {
        if (debug == Debug.TRUE) {
            return true;
        }
        if (debug == Debug.BOTH) {
            return true;
        }
        if (debug == Debug.FALSE) {
            return false;
        }
        if (debug == Debug.AUTO) {
            //TODO: This is where to add logic for skipping debug sign if there are other keystores configured.
            return true;
        }
        throw new IllegalStateException("Could not determine whether to sign with debug keystore.");
    }

    /**
     * @return true if the apk should be signed with both debug and release keystore.
     */
    public boolean shouldCreateBothSignedAndUnsignedApk() {
        return debug == Debug.BOTH;
    }

    /**
     * The debug mode to use when signing the apk.
     */
    public enum Debug {
        /**
         * debug is on
         */
        TRUE,
        /**
         * debug is off
         */
        FALSE,
        /**
         * both debug and release
         */
        BOTH,
        /**
         * auto-detect whether to sign with debug keystore or not
         */
        AUTO
    }

}