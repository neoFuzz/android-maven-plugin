package com.github.cardforge.maven.plugins.android.phase08preparepackage;

import com.android.annotations.NonNull;

/**
 * Which compiler to use to dex the classes.
 */
public enum DexCompiler {
    DEX, // Default
    D8;

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
