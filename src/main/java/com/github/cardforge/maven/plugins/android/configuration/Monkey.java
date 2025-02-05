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

    /**
     * @return the value of skip
     */
    public Boolean isSkip() {
        return skip;
    }

    /**
     * @return the value of eventCount
     */
    public Integer getEventCount() {
        return eventCount;
    }

    /**
     * @return the value of seed
     */
    public Long getSeed() {
        return seed;
    }

    /**
     * @return the value of throttle
     */
    public Long isThrottle() {
        return throttle;
    }

    /**
     * @return the value of percentTouch
     */
    public Integer getPercentTouch() {
        return percentTouch;
    }

    /**
     * @return the value of percentMotion
     */
    public Integer getPercentMotion() {
        return percentMotion;
    }

    /**
     * @return the value of percentTrackball
     */
    public Integer getPercentTrackball() {
        return percentTrackball;
    }

    /**
     * @return the value of percentNav
     */
    public Integer getPercentNav() {
        return percentNav;
    }

    /**
     * @return the value of percentMajorNav
     */
    public Integer getPercentMajorNav() {
        return percentMajorNav;
    }

    /**
     * @return the value of percentSyskeys
     */
    public Integer getPercentSyskeys() {
        return percentSyskeys;
    }

    /**
     * @return the value of percentAppswitch
     */
    public Integer getPercentAppswitch() {
        return percentAppswitch;
    }

    /**
     * @return the value of percentAnyevent
     */
    public Integer getPercentAnyevent() {
        return percentAnyevent;
    }

    /**
     * @return the value of packages
     */
    public String[] getPackages() {
        return packages;
    }

    /**
     * @return the value of categories
     */
    public String[] getCategories() {
        return categories;
    }

    /**
     * @return the value of debugNoEvents
     */
    public Boolean isDebugNoEvents() {
        return skip;
    }

    /**
     * @return the value of hprof
     */
    public Boolean hProf() {
        return skip;
    }

    /**
     * @return if timeout is ignored.
     */
    public Boolean isIgnoreTimeouts() {
        return ignoreTimeouts;
    }

    /**
     * @return if security exceptions are ignored
     */
    public Boolean isIgnoreSecurityExceptions() {
        return ignoreSecurityExceptions;
    }

    /**
     * @return if process should be killed after an error
     */
    public Boolean isKillProcessAfterError() {
        return killProcessAfterError;
    }

    /**
     * @return if native crashes should be monitored
     */
    public Boolean isMonitorNativeErrors() {
        return monitorNativeCrashes;
    }

    /**
     * @return if crashes should be ignored
     */
    public Boolean isCreateReport() {
        return createReport;
    }
}
