package com.github.cardforge.maven.plugins.android.phase01generatesources;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;
import org.apache.maven.plugin.logging.Log;

import java.util.Formatter;

/**
 * Adapter from the Android Utils ILogger interface to the Maven plugins log.
 */
public class MavenILogger implements ILogger {
    private final Log log;
    private final boolean verboseInfo;

    /**
     * @param log          the maven logger to use
     * @param verboseInfo  whether to log verbose information
     */
    public MavenILogger(Log log, boolean verboseInfo) {
        this.log = log;
        this.verboseInfo = verboseInfo;
    }

    /**
     * @param log the maven logger to use
     */
    public MavenILogger(Log log) {
        this.log = log;
        this.verboseInfo = false;
    }

    @Override
    public void error(@Nullable Throwable throwable, @Nullable String s, Object... objects) {
        if ((throwable != null) && (s != null)) {
            final Formatter formatter = new Formatter();
            log.error(formatter.format(s, objects).out().toString(), throwable);
        } else if ((throwable == null) && (s == null)) {
            // do nothing.
        } else if (throwable != null) {
            log.error(throwable);
        } else {
            final Formatter formatter = new Formatter();
            log.error(formatter.format(s, objects).out().toString());
        }
    }

    @Override
    public void warning(@NonNull String s, Object... objects) {
        final Formatter formatter = new Formatter();
        log.warn(formatter.format(s, objects).out().toString());
    }

    @Override
    public void info(@NonNull String s, Object... objects) {
        final Formatter formatter = new Formatter();
        if (verboseInfo) {
            log.debug(formatter.format(s, objects).out().toString());
        } else {
            log.info(formatter.format(s, objects).out().toString());
        }

    }

    @Override
    public void verbose(@NonNull String s, Object... objects) {
        final Formatter formatter = new Formatter();
        log.debug(formatter.format(s, objects).out().toString());
    }
}
