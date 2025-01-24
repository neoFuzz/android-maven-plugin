package com.github.cardforge.maven.plugins.android.configuration;


/**
 * Embedded configuration of {@link com.github.cardforge.maven.plugins.android.phase09package.ApkMojo}.
 *
 * @author Pappy STĂNESCU - pappy.stanescu@gmail.com
 */
@SuppressWarnings("unused")
public class Apk {

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase09package.ApkMojo#apkMetaIncludes}.
     *
     * @deprecated going to be unused
     */
    @Deprecated(since = "4.8")
    private String[] metaIncludes;

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase09package.ApkMojo#apkMetaInf}.
     */
    private MetaInf metaInf;

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase09package.ApkMojo#apkDebug}.
     */
    private Boolean debug;

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase09package.ApkMojo#apkNativeToolchain}.
     */
    private String nativeToolchain;
}
