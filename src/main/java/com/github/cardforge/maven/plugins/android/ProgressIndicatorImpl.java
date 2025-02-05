package com.github.cardforge.maven.plugins.android;

import com.android.repository.api.ProgressIndicator;

/**
 * Progress indicator implementation.
 */
public class ProgressIndicatorImpl implements ProgressIndicator {

    /**
     * Constructor.
     */
    public ProgressIndicatorImpl() {
        // empty
    }

    @Override
    public void setText(String s) {
        // empty
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void cancel() {
        // empty
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    @Override
    public void setCancellable(boolean b) {
        // empty
    }

    @Override
    public boolean isIndeterminate() {
        return false;
    }

    @Override
    public void setIndeterminate(boolean b) {
        // empty
    }

    @Override
    public double getFraction() {
        return 0;
    }

    @Override
    public void setFraction(double v) {
        // empty
    }

    @Override
    public void setSecondaryText(String s) {
        // empty
    }

    @Override
    public void logWarning(String s) {
        // empty
    }

    @Override
    public void logWarning(String s, Throwable throwable) {
        // empty
    }

    @Override
    public void logError(String s) {
        // empty
    }

    @Override
    public void logError(String s, Throwable throwable) {
        // empty
    }

    @Override
    public void logInfo(String s) {
        // empty
    }
}

