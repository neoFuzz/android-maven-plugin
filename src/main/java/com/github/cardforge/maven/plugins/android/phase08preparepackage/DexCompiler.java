package com.github.cardforge.maven.plugins.android.phase08preparepackage;

import com.android.annotations.NonNull;

/**
 * Which compiler to use to dex the classes.
 */
public enum DexCompiler {
    /**
     * Dex is the old dex compiler from Google.
     */
    DEX, // Default
    /**
     * D8 is the new dex compiler from Google.
     */
    D8;

    /**
     * @param name the name of the compiler to use
     * @return the compiler to use
     */
    @NonNull
    public static DexCompiler valueOfIgnoreCase(String name) {
        for (DexCompiler dexCompiler : DexCompiler.values()) {
            if (dexCompiler.name().equalsIgnoreCase(name)) {
                return dexCompiler;
            }
        }
        throw new IllegalArgumentException(
                "No enum constant " + DexCompiler.class.getCanonicalName() + "." + name);
    }

}
