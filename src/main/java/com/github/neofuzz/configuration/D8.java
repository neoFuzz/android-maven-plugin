package com.github.neofuzz.configuration;


/**
 * Configuration for the D8 execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.github.neofuzz.phase08preparepackage.D8Mojo} and used there.
 *
 * @author William Ferguson - william.ferguson@xandar.com.aui
 */
public class D8 {
    /**
     * Mirror of {@link com.github.neofuzz.phase08preparepackage.D8Mojo#d8JvmArguments}
     */
    private String[] jvmArguments;
    /**
     * Mirror of {@link com.github.neofuzz.phase08preparepackage.D8Mojo#d8Intermediate}
     */
    private Boolean intermediate;
    /**
     * Mirror of {@link com.github.neofuzz.phase08preparepackage.D8Mojo#d8MainDexList}
     */
    private String mainDexList;

    /**
     * Mirror of {@link com.github.neofuzz.phase08preparepackage.D8Mojo#d8Arguments}
     */
    private String[] arguments;

    /**
     * Mirror of {@link com.github.neofuzz.phase08preparepackage.D8Mojo#d8Release}
     */
    private Boolean release;

    /**
     * Mirror of {@link com.github.neofuzz.phase08preparepackage.D8Mojo#d8MinApi}
     */
    private Integer minApi;

    /**
     * @return the jvmArguments
     */
    public String[] getJvmArguments() {
        return jvmArguments;
    }

    /**
     * @return whether to generate an intermediate dex file
     */
    public Boolean isIntermediate() {
        return intermediate;
    }

    /**
     * @return the main dex list file
     */
    public String getMainDexList() {
        return mainDexList;
    }

    /**
     * @return the arguments
     */
    public String[] getArguments() {
        return arguments;
    }

    /**
     * @return whether to generate a release build
     */
    public Boolean isRelease() {
        return release;
    }

    /**
     * @return the minApi
     */
    public Integer getMinApi() {
        return minApi;
    }
}
