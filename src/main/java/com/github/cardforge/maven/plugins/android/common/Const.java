package com.github.cardforge.maven.plugins.android.common;

/**
 * Constants used by this plugin.
 */
public class Const {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Const() {
        // No instances
    }

    /**
     * Constants for artifact types.
     */
    public static class ArtifactType {
        /**
         * Static string representing the native library archive file extension.
         */
        public static final String NATIVE_IMPLEMENTATION_ARCHIVE = "a";
        /**
         * Static string representing the native object file extension.
         */
        public static final String NATIVE_SYMBOL_OBJECT = "so";

        /**
         * Private constructor to prevent instantiation of this utility class.
         */
        private ArtifactType() {
            // No instances
        }
    }

}
