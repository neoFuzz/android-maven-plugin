package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.standalonemojos.MonkeyMojo;
import com.github.cardforge.maven.plugins.android.standalonemojos.UIAutomatorMojo;

/**
 * Configuration for the monkey test runs. This class is only the definition of the parameters that are shadowed in
 * {@link MonkeyMojo} and used there.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
public class Monkey {
    /**
     * Mirror of {@link UIAutomatorMojo}#testSkip
     */
    private Boolean skip;
    /**
     * Mirror of {@link Monkey#eventCount}
     */
    private Integer eventCount;
    /**
     * Mirror of {@link Monkey#seed}
     */
    private Long seed;
    /**
     * Mirror of {@link Monkey#throttle}
     */
    private Long throttle;
    /**
     * Mirror of {@link Monkey#percentTouch}
     */
    private Integer percentTouch;
    /**
     * Mirror of {@link Monkey#percentMotion}
     */
    private Integer percentMotion;
    /**
     * Mirror of {@link Monkey#percentTrackball}
     */
    private Integer percentTrackball;
    /**
     * Mirror of {@link Monkey#percentNav}
     */
    private Integer percentNav;
    /**
     * Mirror of {@link Monkey#percentMajorNav}
     */
    private Integer percentMajorNav;
    /**
     * Mirror of {@link Monkey#percentSyskeys}
     */
    private Integer percentSyskeys;
    /**
     * Mirror of {@link Monkey#percentAppswitch}
     */
    private Integer percentAppswitch;
    /**
     * Mirror of {@link Monkey#percentAnyevent}
     */
    private Integer percentAnyevent;

    /**
     * Mirror of {@link Monkey#packages}
     */
    private String[] packages;
    /**
     * Mirror of {@link Monkey#categories}
     */
    private String[] categories;
    /**
     * Mirror of {@link UIAutomatorMojo}#debugNoEvents
     */
    private Boolean debugNoEvents;
    /**
     * Mirror of {@link UIAutomatorMojo}#hprof
     */
    private Boolean hprof;
    /**
     * Mirror of {@link Monkey#ignoreCrashes}
     */
    private Boolean ignoreCrashes;
    /**
     * Mirror of {@link Monkey#ignoreTimeouts}
     */
    private Boolean ignoreTimeouts;
    /**
     * Mirror of {@link Monkey#ignoreSecurityExceptions}
     */
    private Boolean ignoreSecurityExceptions;
    /**
     * Mirror of {@link Monkey#killProcessAfterError}
     */
    private Boolean killProcessAfterError;
    /**
     * Mirror of {@link Monkey#monitorNativeCrashes}
     */
    private Boolean monitorNativeCrashes;
    /**
     * Mirror of {@link Monkey#createReport}
     */
    private Boolean createReport;

    public Boolean isSkip() {
        return skip;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public Long getSeed() {
        return seed;
    }

    public Long isThrottle() {
        return throttle;
    }

    public Integer getPercentTouch() {
        return percentTouch;
    }

    public Integer getPercentMotion() {
        return percentMotion;
    }

    public Integer getPercentTrackball() {
        return percentTrackball;
    }

    public Integer getPercentNav() {
        return percentNav;
    }

    public Integer getPercentMajorNav() {
        return percentMajorNav;
    }

    public Integer getPercentSyskeys() {
        return percentSyskeys;
    }

    public Integer getPercentAppswitch() {
        return percentAppswitch;
    }

    public Integer getPercentAnyevent() {
        return percentAnyevent;
    }

    public String[] getPackages() {
        return packages;
    }

    public String[] getCategories() {
        return categories;
    }

    public Boolean isDebugNoEvents() {
        return skip;
    }

    public Boolean hProf() {
        return skip;
    }

    public Boolean isIgnoreTimeouts() {
        return ignoreTimeouts;
    }

    public Boolean isIgnoreSecurityExceptions() {
        return ignoreSecurityExceptions;
    }

    public Boolean isKillProcessAfterError() {
        return killProcessAfterError;
    }

    public Boolean isMonitorNativeErrors() {
        return monitorNativeCrashes;
    }

    public Boolean isCreateReport() {
        return createReport;
    }
}
