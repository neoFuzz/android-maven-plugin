package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.standalonemojos.CompatibleScreen;
import com.github.cardforge.maven.plugins.android.standalonemojos.SupportsScreens;

import java.util.List;
import java.util.Properties;

/**
 * Configuration for the manifest update. This class is only the definition of the parameters that are shadowed in
 * ManifestUpdateMojo(deprecated) and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 * @deprecated Use ManifestMerger {@link ManifestMerger} in
 * combination with ManifestUpdateMojo(deprecated)
 */
@Deprecated(since = "4.8")
public class Manifest {
    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestVersionName.
     */
    protected String versionName;

    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestVersionCode.
     */
    protected Integer versionCode;
    /**
     * Mirror of {@code ManifestUpdateMojo#manifestVersionCodeUpdateFromVersion(deprecated)}.
     */
    protected Boolean versionCodeUpdateFromVersion;
    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestApplicationIcon.
     */
    protected String applicationIcon;
    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestApplicationLabel.
     */
    protected String applicationLabel;
    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestApplicationTheme.
     */
    protected String applicationTheme;
    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestSharedUserId.
     */
    protected String sharedUserId;
    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestDebuggable.
     */
    protected Boolean debuggable;
    /**
     * Mirror of
     * ManifestUpdateMojo(deprecated)#manifestSupportsScreens
     * .
     */
    protected SupportsScreens supportsScreens;
    /**
     * Mirror of
     * ManifestUpdateMojo(deprecated)#manifestCompatibleScreens
     * .
     */
    protected List<CompatibleScreen> compatibleScreens;
    /**
     * Mirror of
     * ManifestUpdateMojo(deprecated)#manifestProviderAuthorities
     * .
     */
    protected Properties providerAuthorities;
    /**
     * Mirror of
     * ManifestUpdateMojo(deprecated)#manifestUsesSdk
     */
    protected UsesSdk usesSdk;
    /**
     * Mirror of ManifestUpdateMojo(deprecated)#manifestVersionCodeAutoIncrement.
     */
    private Boolean versionCodeAutoIncrement;

    /**
     * @return the versionName
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * @return the versionCode
     */
    public Integer getVersionCode() {
        return versionCode;
    }

    /**
     * @return the versionCodeAutoIncrement
     */
    public Boolean getVersionCodeAutoIncrement() {
        return versionCodeAutoIncrement;
    }

    /**
     * @return the versionCodeUpdateFromVersion
     */
    public Boolean getVersionCodeUpdateFromVersion() {
        return versionCodeUpdateFromVersion;
    }

    /**
     * @return the applicationIcon
     */
    public String getApplicationIcon() {
        return applicationIcon;
    }

    /**
     * @return the applicationLabel
     */
    public String getApplicationLabel() {
        return applicationLabel;
    }

    /**
     * @return the applicationTheme
     */
    public String getApplicationTheme() {
        return applicationTheme;
    }

    /**
     * @return the sharedUserId
     */
    public String getSharedUserId() {
        return sharedUserId;
    }

    /**
     * @return the debuggable
     */
    public Boolean getDebuggable() {
        return debuggable;
    }

    /**
     * @return the supportsScreens
     */
    public SupportsScreens getSupportsScreens() {
        return supportsScreens;
    }

    /**
     * @return the compatibleScreens
     */
    public List<CompatibleScreen> getCompatibleScreens() {
        return compatibleScreens;
    }

    /**
     * @return the providerAuthorities
     */
    public Properties getProviderAuthorities() {
        return providerAuthorities;
    }

    /**
     * @return the usesSdk
     */
    public UsesSdk getUsesSdk() {
        return usesSdk;
    }
}
