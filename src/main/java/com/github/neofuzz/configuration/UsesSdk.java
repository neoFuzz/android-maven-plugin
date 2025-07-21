package com.github.neofuzz.configuration;

/**
 * Abstraction for uses-sdk tag in the Android manifest.
 *
 * @author Francisco Javier Fernandez fjfernandez@tuenti.com
 */

public class UsesSdk {
    private static final int PRIME_NUMBER = 31;
    private String minSdkVersion;
    private String maxSdkVersion;
    private String targetSdkVersion;

    /**
     * @return the minSdkVersion
     */
    public String getMinSdkVersion() {
        return minSdkVersion;
    }

    /**
     * @return the maxSdkVersion
     */
    public String getMaxSdkVersion() {
        return maxSdkVersion;
    }

    /**
     * @return the targetSdkVersion
     */
    public String getTargetSdkVersion() {
        return targetSdkVersion;
    }

    /**
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UsesSdk usesSdk = (UsesSdk) o;

        if (maxSdkVersion != null
                ? !maxSdkVersion.equals(usesSdk.maxSdkVersion)
                : usesSdk.maxSdkVersion != null) {
            return false;
        }
        if (minSdkVersion != null
                ? !minSdkVersion.equals(usesSdk.minSdkVersion)
                : usesSdk.minSdkVersion != null) {
            return false;
        }
        if (targetSdkVersion != null
                ? !targetSdkVersion.equals(usesSdk.targetSdkVersion)
                : usesSdk.targetSdkVersion != null) {
            return false;
        }

        return true;
    }

    /**
     * @return the hash code of the object
     */
    @Override
    public int hashCode() {
        int result = minSdkVersion != null ? minSdkVersion.hashCode() : 0;
        result = PRIME_NUMBER * result + (maxSdkVersion != null ? maxSdkVersion.hashCode() : 0);
        result = PRIME_NUMBER * result + (targetSdkVersion != null ? targetSdkVersion.hashCode() : 0);
        return result;
    }

    /**
     * @return the string representation of the object
     */
    @Override
    public String toString() {
        return minSdkVersion + " : " + maxSdkVersion + " : " + targetSdkVersion;
    }
}
