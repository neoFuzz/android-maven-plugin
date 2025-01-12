package com.github.cardforge.maven.plugins.android.configuration;


/**
 * Embedded configuration of {@link ApkMojo}.
 *
 * @author Pappy STĂNESCU - pappy.stanescu@gmail.com
 */
@SuppressWarnings("unused")
public class Apk {

    /**
     * Mirror of {@link ApkMojo#apkMetaIncludes}.
     */
    @Deprecated(since = "4.8", forRemoval = false)
    private String[] metaIncludes;

    /**
     * Mirror of {@link ApkMojo#apkMetaInf}.
     */
    private MetaInf metaInf;

    /**
     * Mirror of {@link ApkMojo#apkDebug}.
     */
    private Boolean debug;

    /**
     * Mirror of {@link ApkMojo#apkNativeToolchain}.
     */
    private String nativeToolchain;
}
