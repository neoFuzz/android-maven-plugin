package com.github.cardforge.maven.plugins.android.standalonemojos;

/**
 * CompatibleScreen abstracts the AndroidManifest element.
 */
public class CompatibleScreen {

    /**
     * Screen size in string format
     */
    private String screenSize;
    /**
     *
     */
    private String screenDensity;

    /**
     * @return the screen size
     */
    public String getScreenSize() {
        return screenSize;
    }

    /**
     * @param screenSize the screen size to set.
     */
    public void setScreenSize(String screenSize) {
        this.screenSize = screenSize;
    }

    /**
     * @return the screen density
     */
    public String getScreenDensity() {
        return screenDensity;
    }

    /**
     * @param screenDensity the screen density to set.
     */
    public void setScreenDensity(String screenDensity) {
        this.screenDensity = screenDensity;
    }

    /**
     * @param obj object to compare with
     * @return true if the given object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompatibleScreen that) {
            return this.screenDensity.equals(that.screenDensity) && this.screenSize.equals(that.screenSize);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (screenDensity + screenSize).hashCode();
    }

    /**
     * @return the object string
     */
    @Override
    public String toString() {
        return screenSize + ":" + screenDensity;
    }
}
