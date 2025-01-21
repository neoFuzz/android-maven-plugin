package com.github.cardforge.maven.plugins.android.configuration;


/**
 * Configuration for the dex  test execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Dex {
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexJvmArguments}
     */
    private String[] jvmArguments;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexCoreLibrary}
     */
    private Boolean coreLibrary;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexNoLocals}
     */
    private Boolean noLocals;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexOptimize}
     */
    private Boolean optimize;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexPreDex}
     */
    private Boolean preDex;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexPreDexLibLocation}
     */
    private String preDexLibLocation;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexIncremental}
     */
    private Boolean incremental;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#forceJumbo}
     */
    private Boolean forceJumbo;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#multiDex}
     */
    private Boolean multiDex;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#mainDexList}
     */
    private String mainDexList;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#minimalMainDex}
     */
    private Boolean minimalMainDex;
    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#generateMainDexList}
     */
    private Boolean generateMainDexList;

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase08preparepackage.DexMojo#dexArguments}
     */
    private String dexArguments;

    /**
     * @return the jvmArguments
     */
    public String[] getJvmArguments() {
        return jvmArguments;
    }

    /**
     * @return if core library should be used
     */
    public Boolean isCoreLibrary() {
        return coreLibrary;
    }

    /**
     * @return if locals should be kept
     */
    public Boolean isNoLocals() {
        return noLocals;
    }

    /**
     * @return if optimization should be used
     */
    public Boolean isOptimize() {
        return optimize;
    }

    /**
     * @return if pre-dexing should be used
     */
    public Boolean isPreDex() {
        return preDex;
    }

    /**
     * @return the location of the pre-dexed libraries
     */
    public String getPreDexLibLocation() {
        return preDexLibLocation;
    }

    /**
     * @return if dex should be incremental
     */
    public Boolean isIncremental() {
        return incremental;
    }

    /**
     * @return if jumbo should be forced
     */
    public Boolean isForceJumbo() {
        return forceJumbo;
    }

    /**
     * @return if multi dex should be used
     */
    public Boolean isMultiDex() {
        return multiDex;
    }

    /**
     * @return the main dex list file
     */
    public String getMainDexList() {
        return mainDexList;
    }

    /**
     * @return if main dex list should be minimal
     */
    public Boolean isMinimalMainDex() {
        return minimalMainDex;
    }

    /**
     * @return if main dex list should be generated
     */
    public Boolean isGenerateMainDexList() {
        return generateMainDexList;
    }

    /**
     * @return the dexArguments
     */
    public String getDexArguments() {
        return dexArguments;
    }

}
