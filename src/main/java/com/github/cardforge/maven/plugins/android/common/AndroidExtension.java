package com.github.cardforge.maven.plugins.android.common;

/**
 * The file system extension for the Android artifact also used for the packaging type of Android Maven Project.
 */
public final class AndroidExtension {
    /**
     * Android application.
     */
    public static final String APK = "apk";

    /**
     * Android library project as created by Android Maven Plugin.
     *
     * @deprecated Use {@link #AAR} instead.
     */
    @Deprecated(since = "4.8")
    public static final String APKLIB = "apklib";

    /**
     * Android archive as introduced by the Gradle Android build system (modelled after apklib with extensions and some
     * differences).
     */
    public static final String AAR = "aar";


    /**
     * @deprecated Use {@link #APKLIB} instead.
     */
    @Deprecated(since = "4.8")
    public static final String APKSOURCES = "apksources";

    //No instances
    private AndroidExtension() {
    }

    /**
     * Determine whether a {@link org.apache.maven.project.MavenProject MavenProject}'s packaging is an
     * Android project.
     *
     * @param packaging Project packaging.
     * @return True if an Android project.
     */
    public static boolean isAndroidPackaging(String packaging) {
        return APK.equals(packaging)|| AAR.equalsIgnoreCase(packaging) ||
                APKLIB.equals(packaging) || APKSOURCES.equals(packaging);
    }
}
