package com.github.cardforge.maven.plugins.android.standalonemojos;

/**
 * Class that represents and manages Android screen size configurations for an application.
 * This class handles various screen-related attributes including size categories, density,
 * and width specifications that can be defined in an Android manifest.
 */
public class SupportsScreens {

    private String resizeable;
    private String smallScreens, normalScreens, largeScreens, xlargeScreens;
    private String anyDensity;
    private String requiresSmallestWidthDp;
    private String compatibleWidthLimitDp;
    private String largestWidthLimitDp;

    /**
     * Gets the resizeable flag indicating if the application's windows can be resized.
     *
     * @return String value representing if the application is resizeable
     */
    public String getResizeable() {
        return resizeable;
    }

    /**
     * Sets the resizeable flag for the application.
     *
     * @param resizable String value indicating if the application should be resizeable
     */
    public void setResizeable(String resizable) {
        this.resizeable = resizable;
    }

    /**
     * Gets the small screens support flag.
     *
     * @return String value indicating if the application supports small screens
     */
    public String getSmallScreens() {
        return smallScreens;
    }

    /**
     * Sets the small screens support flag.
     *
     * @param smallScreens String value indicating if the application should support small screens
     */
    public void setSmallScreens(String smallScreens) {
        this.smallScreens = smallScreens;
    }

    /**
     * Gets the normal screens support flag.
     *
     * @return String value indicating if the application supports normal screens
     */
    public String getNormalScreens() {
        return normalScreens;
    }

    /**
     * Sets the normal screens support flag.
     *
     * @param normalScreens String value indicating if the application should support normal screens
     */
    public void setNormalScreens(String normalScreens) {
        this.normalScreens = normalScreens;
    }

    /**
     * @return String value indicating if the application supports large screens
     */
    public String getLargeScreens() {
        return largeScreens;
    }

    /**
     * Sets the large screens support flag.
     *
     * @param largeScreens String value indicating if the application should support large screens
     */
    public void setLargeScreens(String largeScreens) {
        this.largeScreens = largeScreens;
    }

    /**
     * Gets the extra large screens support flag.
     *
     * @return String value indicating if the application supports extra large screens
     */
    public String getXlargeScreens() {
        return xlargeScreens;
    }

    /**
     * Sets the extra large screens support flag.
     *
     * @param xlargeScreens String value indicating if the application should support extra large screens
     */
    public void setXlargeScreens(String xlargeScreens) {
        this.xlargeScreens = xlargeScreens;
    }

    /**
     * Gets the 'any' density support flag.
     *
     * @return String value indicating if the application supports any screen density
     */
    public String getAnyDensity() {
        return anyDensity;
    }

    /**
     * Sets the any density support flag.
     *
     * @param anyDensity String value indicating if the application should support any screen density
     */
    public void setAnyDensity(String anyDensity) {
        this.anyDensity = anyDensity;
    }

    /**
     * Gets the required smallest width in density-independent pixels.
     *
     * @return String value representing the smallest width in dp that the application requires
     */
    public String getRequiresSmallestWidthDp() {
        return requiresSmallestWidthDp;
    }

    /**
     * Sets the required smallest width in density-independent pixels.
     *
     * @param requiresSmallestWidthDp String value setting the smallest width in dp that the application requires
     */
    public void setRequiresSmallestWidthDp(String requiresSmallestWidthDp) {
        this.requiresSmallestWidthDp = requiresSmallestWidthDp;
    }

    /**
     * Gets the compatible width limit in density-independent pixels.
     *
     * @return String value representing the maximum compatible width in dp
     */
    public String getCompatibleWidthLimitDp() {
        return compatibleWidthLimitDp;
    }

    /**
     * Sets the compatible width limit in density-independent pixels.
     *
     * @param compatibleWidthLimitDp String value setting the maximum compatible width in dp
     */
    public void setCompatibleWidthLimitDp(String compatibleWidthLimitDp) {
        this.compatibleWidthLimitDp = compatibleWidthLimitDp;
    }

    /**
     * Gets the largest width limit in density-independent pixels.
     *
     * @return String value representing the largest supported width in dp
     */
    public String getLargestWidthLimitDp() {
        return largestWidthLimitDp;
    }


    /**
     * Sets the largest width limit in density-independent pixels.
     *
     * @param largestWidthLimitDp String value setting the largest supported width in dp
     */
    public void setLargestWidthLimitDp(String largestWidthLimitDp) {
        this.largestWidthLimitDp = largestWidthLimitDp;
    }

}
