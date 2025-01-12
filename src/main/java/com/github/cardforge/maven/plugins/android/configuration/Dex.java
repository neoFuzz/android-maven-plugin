package com.github.cardforge.maven.plugins.android.configuration;


/**
 * Configuration for the dex  test execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link DexMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Dex {
    /**
     * Mirror of {@link DexMojo#dexJvmArguments}
     */
    private String[] jvmArguments;
    /**
     * Mirror of {@link DexMojo#dexCoreLibrary}
     */
    private Boolean coreLibrary;
    /**
     * Mirror of {@link DexMojo#dexNoLocals}
     */
    private Boolean noLocals;
    /**
     * Mirror of {@link DexMojo#dexOptimize}
     */
    private Boolean optimize;
    /**
     * Mirror of {@link DexMojo#dexPreDex}
     */
    private Boolean preDex;
    /**
     * Mirror of {@link DexMojo#dexPreDexLibLocation}
     */
    private String preDexLibLocation;
    /**
     * Mirror of {@link DexMojo#dexIncremental}
     */
    private Boolean incremental;
    /**
     * Mirror of {@link DexMojo#forceJumbo}
     */
    private Boolean forceJumbo;
    /**
     * Mirror of {@link DexMojo#multiDex}
     */
    private Boolean multiDex;
    /**
     * Mirror of {@link DexMojo#mainDexList}
     */
    private String mainDexList;
    /**
     * Mirror of {@link DexMojo#minimalMainDex}
     */
    private Boolean minimalMainDex;
    /**
     * Mirror of {@link DexMojo#generateMainDexList}
     */
    private Boolean generateMainDexList;

    private String dexArguments;

    public String[] getJvmArguments() {
        return jvmArguments;
    }

    public Boolean isCoreLibrary() {
        return coreLibrary;
    }

    public Boolean isNoLocals() {
        return noLocals;
    }

    public Boolean isOptimize() {
        return optimize;
    }

    public Boolean isPreDex() {
        return preDex;
    }

    public String getPreDexLibLocation() {
        return preDexLibLocation;
    }

    public Boolean isIncremental() {
        return incremental;
    }

    public Boolean isForceJumbo() {
        return forceJumbo;
    }

    public Boolean isMultiDex() {
        return multiDex;
    }

    public String getMainDexList() {
        return mainDexList;
    }

    public Boolean isMinimalMainDex() {
        return minimalMainDex;
    }

    public Boolean isGenerateMainDexList() {
        return generateMainDexList;
    }

    public String getDexArguments() {
        return dexArguments;
    }

}
