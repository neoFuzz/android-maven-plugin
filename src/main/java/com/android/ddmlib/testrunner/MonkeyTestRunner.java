package com.android.ddmlib.testrunner;

import com.android.annotations.NonNull;
import com.android.ddmlib.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Runs a Monkey test command remotely and reports results.
 */
public class MonkeyTestRunner {

    private static final String LOG_TAG = "RemoteAndroidTest";
    // defined instrumentation argument names
    private static final String SEED_ARG_NAME = "-s";
    private static final String THROTTLE_ARG_NAME = "--throttle";
    private static final String PERCENT_TOUCH_ARG_NAME = "--pct-touch";
    private static final String PERCENT_MOTION_ARG_NAME = "--pct-motion";
    private static final String PERCENT_TRACKBALL_ARG_NAME = "--pct-trackball";
    private static final String PERCENT_NAV_ARG_NAME = "--pct-nav";
    private static final String PERCENT_MAJORNAV_ARG_NAME = "--pct-majornav";
    private static final String PERCENT_SYSKEYS_ARG_NAME = "--pct-syskeys";
    private static final String PERCENT_APPSWITCH_ARG_NAME = "--pct-appswitch";
    private static final String PERCENT_ANYEVENT_ARG_NAME = "--pct-anyevent";
    private static final String PACKAGE_ARG_NAME = "-p";
    private static final String CATEGORY_ARG_NAME = "-c";
    private final int eventCount;
    private final IDevice mRemoteDevice;
    /**
     * map of name-value instrumentation argument pairs
     */
    private final List<Entry<String, String>> mArgList;
    // default to no timeout
    private int mMaxTimeToOutputResponse = 0;
    private String mRunName = null;
    private MonkeyResultParser mParser;
    private boolean ignoreCrashes;
    private boolean debugNoEvents;
    private boolean hprof;
    private boolean ignoreTimeouts;
    private boolean ignoreSecurityExceptions;
    private boolean killProcessAfterError;
    private boolean monitorNativeCrashes;

    /**
     * @param eventCount   number of events to inject
     * @param remoteDevice device to run instrumentation on
     */
    public MonkeyTestRunner(int eventCount, IDevice remoteDevice) {
        this.eventCount = eventCount;
        mRemoteDevice = remoteDevice;
        mArgList = new ArrayList<>();
    }

    /**
     * Add argument with given name and value.
     *
     * @param name  name of the argument
     * @param value value of the argument.
     */
    public void addArg(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name or value arguments cannot be null");
        }
        mArgList.add(new AbstractMap.SimpleImmutableEntry<>(name, value));
    }

    /**
     * Add boolean argument with given name and value.
     *
     * @param name  name of the boolean
     * @param value value of the boolean.
     */
    public void addBooleanArg(String name, boolean value) {
        addArg(name, Boolean.toString(value));
    }

    /**
     * Add long argument with given name and value.
     *
     * @param name  name of the Long
     * @param value value of the Long.
     */
    public void addLongArg(String name, long value) {
        addArg(name, Long.toString(value));
    }

    /**
     * @param seed the seed to use for the monkey test
     */
    public void setSeed(long seed) {
        addLongArg(SEED_ARG_NAME, seed);
    }

    /**
     * @param throttle the throttle to use for the monkey test
     */
    public void setThrottle(long throttle) {
        addLongArg(THROTTLE_ARG_NAME, throttle);
    }

    /**
     * @param percent the percent of touch events to send
     */
    public void setPercentTouch(long percent) {
        addLongArg(PERCENT_TOUCH_ARG_NAME, percent);
    }

    /**
     * @param percent the percent of motion events to send
     */
    public void setPercentMotion(long percent) {
        addLongArg(PERCENT_MOTION_ARG_NAME, percent);
    }

    /**
     * @param percent the percent of trackball events to send
     */
    public void setPercentTrackball(long percent) {
        addLongArg(PERCENT_TRACKBALL_ARG_NAME, percent);
    }

    /**
     * @param percent the percent of navigation events to send
     */
    public void setPercentNav(long percent) {
        addLongArg(PERCENT_NAV_ARG_NAME, percent);
    }

    /**
     * @param percent the percent of major navigation events to send
     */
    public void setPercentMajorNav(long percent) {
        addLongArg(PERCENT_MAJORNAV_ARG_NAME, percent);
    }

    /**
     * @param percent the percent of system key events to send
     */
    public void setPercentSyskeys(long percent) {
        addLongArg(PERCENT_SYSKEYS_ARG_NAME, percent);
    }

    /**
     * @param percent the percent of app switching events to send
     */
    public void setPercentAppswitch(long percent) {
        addLongArg(PERCENT_APPSWITCH_ARG_NAME, percent);
    }

    /**
     * @param percent the percent of any events to send
     */
    public void setPercentAnyEvent(int percent) {
        addLongArg(PERCENT_ANYEVENT_ARG_NAME, percent);
    }

    /**
     * @param packages the packages to run the monkey test against
     */
    public void setPackages(@NonNull String[] packages) {
        for (String packageName : packages) {
            addArg(PACKAGE_ARG_NAME, packageName);
        }
    }

    /**
     * @param categories the categories to run the monkey test against
     */
    public void setCategories(@NonNull String[] categories) {
        for (String category : categories) {
            addArg(CATEGORY_ARG_NAME, category);
        }
    }

    /**
     * @param debugNoEvents if true, do not send any events to the system
     */
    public void setDebugNoEvents(boolean debugNoEvents) {
        this.debugNoEvents = debugNoEvents;
    }

    /**
     * @param hprof if true, generate a hprof file
     */
    public void setHprof(boolean hprof) {
        this.hprof = hprof;
    }

    /**
     * @param ignoreCrashes if true, do not report crashes
     */
    public void setIgnoreCrashes(boolean ignoreCrashes) {
        this.ignoreCrashes = ignoreCrashes;
    }

    /**
     * @param ignoreTimeouts if true, do not report timeouts
     */
    public void setIgnoreTimeouts(boolean ignoreTimeouts) {
        this.ignoreTimeouts = ignoreTimeouts;
    }

    /**
     * @param ignoreSecurityExceptions if true, do not report security exceptions
     */
    public void setIgnoreSecurityExceptions(boolean ignoreSecurityExceptions) {
        this.ignoreSecurityExceptions = ignoreSecurityExceptions;
    }

    /**
     * @param killProcessAfterError if true, kill the process after an error occurs
     */
    public void setKillProcessAfterError(boolean killProcessAfterError) {
        this.killProcessAfterError = killProcessAfterError;
    }

    /**
     * @param monitorNativeCrashes if true, monitor native crashes
     */
    public void setMonitorNativeCrash(boolean monitorNativeCrashes) {
        this.monitorNativeCrashes = monitorNativeCrashes;
    }

    /**
     * Set the maximum time to wait for instrumentation to send a response before assuming the test
     * has timed out.
     *
     * @param maxTimeToOutputResponse the time, in ms, or 0 for no timeout
     */
    public void setMaxtimeToOutputResponse(int maxTimeToOutputResponse) {
        mMaxTimeToOutputResponse = maxTimeToOutputResponse;
    }

    /**
     * Sets the run name to be used when reporting test results.
     *
     * @param runName the test run name
     */
    public void setRunName(String runName) {
        mRunName = runName;
    }

    /**
     * Runs the tests from a list of instrumentation listeners.
     *
     * @param listeners the listeners to report test results to
     * @throws TimeoutException                  if the test run has timed out
     * @throws AdbCommandRejectedException       if adb rejects the shell command
     * @throws ShellCommandUnresponsiveException if the shell command is unresponsive
     * @throws IOException                       if an I/O error occurs
     */
    public void run(ITestRunListener... listeners) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        run(Arrays.asList(listeners));
    }

    /**
     * Runs the tests from a list of instrumentation listeners.
     *
     * @param listeners the listeners to report test results to
     * @throws TimeoutException                  if the test run has timed out
     * @throws AdbCommandRejectedException       if adb rejects the shell command
     * @throws ShellCommandUnresponsiveException if the shell command is unresponsive
     * @throws IOException                       if an I/O error occurs
     */
    public void run(Collection<ITestRunListener> listeners) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        final String runCaseCommandStr = String.format("monkey -v -v -v %1$s %2$s", buildArgsCommand(),
                Long.toString(eventCount));
        Log.i(LOG_TAG, String.format("Running %1$s on %2$s", runCaseCommandStr, mRemoteDevice.getSerialNumber()));
        mParser = new MonkeyResultParser(mRunName, listeners);

        try {
            mRemoteDevice.executeShellCommand(runCaseCommandStr, mParser, mMaxTimeToOutputResponse);
        } catch (IOException e) {
            Log.w(LOG_TAG,
                    String.format("IOException %1$s when running monkey tests on %3$s", e,
                            mRemoteDevice.getSerialNumber()));
            // rely on parser to communicate results to listeners
            mParser.handleTestRunFailed(e.toString());
            throw e;
        } catch (ShellCommandUnresponsiveException e) {
            Log.w(LOG_TAG,
                    String.format("ShellCommandUnresponsiveException %1$s when running monkey tests on %3$s",
                            e, mRemoteDevice.getSerialNumber()));
            mParser.handleTestRunFailed(String.format("Failed to receive adb shell test output within %1$d ms. "
                            + "Test may have timed out, or adb connection to device became unresponsive",
                    mMaxTimeToOutputResponse));
            throw e;
        } catch (TimeoutException e) {
            Log.w(LOG_TAG,
                    String.format("TimeoutException when running monkey tests on %2$s",
                            mRemoteDevice.getSerialNumber()));
            mParser.handleTestRunFailed(e.toString());
            throw e;
        } catch (AdbCommandRejectedException e) {
            Log.w(LOG_TAG,
                    String.format("AdbCommandRejectedException %1$s when running monkey tests %2$s on %3$s",
                            e, mRemoteDevice.getSerialNumber()));
            mParser.handleTestRunFailed(e.toString());
            throw e;
        }
    }

    /**
     * Sets the boolean for {@code mParser}
     */
    public void cancel() {
        if (mParser != null) {
            mParser.cancel();
        }
    }

    /**
     * Returns the full instrumentation command line syntax for the provided instrumentation arguments. Returns an empty
     * string if no arguments were specified.
     */
    @NonNull
    private String buildArgsCommand() {
        StringBuilder commandBuilder = new StringBuilder();
        for (Entry<String, String> argPair : mArgList) {
            final String argCmd = String.format(" %1$s %2$s", argPair.getKey(), argPair.getValue());
            commandBuilder.append(argCmd);
        }

        if (debugNoEvents) {
            commandBuilder.append(" --dbg-no-events");
        }
        if (hprof) {
            commandBuilder.append(" --hprof");
        }
        if (ignoreCrashes) {
            commandBuilder.append(" --ignore-crashes");
        }
        if (ignoreTimeouts) {
            commandBuilder.append(" --ignore-timeouts");
        }
        if (ignoreSecurityExceptions) {
            commandBuilder.append(" --ignore-security-exceptions");
        }
        if (killProcessAfterError) {
            commandBuilder.append(" --kill-process-after-error");
        }
        if (monitorNativeCrashes) {
            commandBuilder.append(" --monitor-native-crashes");
        }
        return commandBuilder.toString();
    }

    /**
     * Parses the output of the monkey test run and notifies listeners of the results.
     */
    private class MonkeyResultParser extends MultiLineReceiver {

        private static final String CRASH_KEY = "// CRASH:";
        private static final String SHORT_MESSAGE_KEY = "// Short Msg:";
        private static final String LONG_MESSAGE_KEY = "// Long Msg:";
        private static final String BUILD_LABEL_KEY = "// Build Label:";
        private static final String BUILD_CHANGELIST_KEY = "// Build Changelist:";
        private static final String BUILD_TIME_KEY = "// Build Time:";
        private static final String EMPTY_KEY = "//";
        private static final String SENDING_KEY = ":Sending";
        private static final String SWITCHING_KEY = ":Switch";
        private static final String MONKEY_KEY = ":Monkey:";

        private final Collection<ITestRunListener> mTestListeners;

        private final String runName;
        private final HashMap<String, String> runMetrics = new HashMap<>();
        private boolean canceled;
        private TestIdentifier mCurrentTestIndentifier;
        private long elapsedTime;

        /**
         * @param runName   the test run name
         * @param listeners the listeners to notify of test results
         */
        private MonkeyResultParser(String runName, Collection<ITestRunListener> listeners) {
            this.runName = runName;
            mTestListeners = new ArrayList<>(listeners);
        }

        /**
         * Cancels the test run.
         */
        public void cancel() {
            canceled = true;
        }

        /**
         * @return true if the test run has been canceled, false otherwise
         */
        @Override
        public boolean isCancelled() {
            return canceled;
        }

        /**
         * Called when the test run is complete. Notifies listeners of the test run ending.
         */
        @Override
        public void done() {
            handleTestEnd();
            handleTestRunEnded();
            super.done();
        }

        /**
         * @param lines The array containing the new lines.
         */
        @Override
        public void processNewLines(@NonNull String[] lines) {
            for (int indexLine = 0; indexLine < lines.length; indexLine++) {
                String line = lines[indexLine];
                Log.v("monkey receiver:" + runName, line);

                if (line.startsWith(MONKEY_KEY)) {
                    handleTestRunStarted();
                }
                if (line.startsWith(SHORT_MESSAGE_KEY)) {
                    runMetrics.put("ShortMsg", line.substring(SHORT_MESSAGE_KEY.length() - 1));
                }
                if (line.startsWith(LONG_MESSAGE_KEY)) {
                    runMetrics.put("LongMsg", line.substring(LONG_MESSAGE_KEY.length() - 1));
                }
                if (line.startsWith(BUILD_LABEL_KEY)) {
                    runMetrics.put("BuildLabel", line.substring(BUILD_LABEL_KEY.length() - 1));
                }
                if (line.startsWith(BUILD_CHANGELIST_KEY)) {
                    runMetrics.put("BuildChangeList", line.substring(BUILD_CHANGELIST_KEY.length() - 1));
                }
                if (line.startsWith(BUILD_TIME_KEY)) {
                    runMetrics.put("BuildTime", line.substring(BUILD_TIME_KEY.length() - 1));
                }

                if (line.startsWith(SENDING_KEY) || line.startsWith(SWITCHING_KEY)) {
                    handleTestEnd();
                    handleTestStarted(line);
                }

                if (line.startsWith(CRASH_KEY)) {
                    Log.d("monkey received crash:", line);
                    indexLine = handleCrash(lines, indexLine);
                    handleTestEnd();
                }
            }
        }

        /**
         * Notifies listeners of the start of the test run.
         */
        private void handleTestRunStarted() {
            elapsedTime = System.currentTimeMillis();
            for (ITestRunListener listener : mTestListeners) {
                listener.testRunStarted(mRunName, eventCount);
            }
        }

        /**
         * @param error the error message to notify listeners of
         */
        public void handleTestRunFailed(String error) {
            for (ITestRunListener listener : mTestListeners) {
                listener.testRunFailed(error);
            }
        }

        /**
         * Notifies listeners of the end of the test run.
         */
        private void handleTestRunEnded() {
            elapsedTime = System.currentTimeMillis() - elapsedTime;

            for (ITestRunListener listener : mTestListeners) {
                listener.testRunEnded(elapsedTime, runMetrics);
            }
        }

        /**
         * @param line the line containing the test identifier
         */
        private void handleTestStarted(String line) {
            mCurrentTestIndentifier = new TestIdentifier("MonkeyTest", line);
            for (ITestRunListener listener : mTestListeners) {
                listener.testStarted(mCurrentTestIndentifier);
            }
        }

        /**
         * Notifies listeners of the end of the current test.
         */
        private void handleTestEnd() {
            if (mCurrentTestIndentifier != null) {
                for (ITestRunListener listener : mTestListeners) {
                    listener.testEnded(mCurrentTestIndentifier, new HashMap<>());
                }
                mCurrentTestIndentifier = null;
            }
        }

        /**
         * @param lines     The array containing the new lines.
         * @param indexLine The index of the line containing the crash
         * @return the index of the line after the crash
         */
        private int handleCrash(@NonNull String[] lines, int indexLine) {
            StringBuilder errorBuilder = new StringBuilder();
            boolean errorEnd = false;
            boolean errorStart = false;
            do {
                String line = lines[indexLine];
                if (line.startsWith(BUILD_TIME_KEY)) {
                    errorStart = true;
                }
                indexLine++;
            } while (!errorStart);

            // indexLine point to the first line of the stack trace now
            int firstLine = indexLine;

            do {
                String line = lines[indexLine];
                if (line.equals(EMPTY_KEY)) {
                    errorEnd = true;
                } else {
                    String stackTraceLine = lines[indexLine];
                    stackTraceLine = stackTraceLine.substring(indexLine == firstLine ? 3 : 4);
                    errorBuilder.append(stackTraceLine).append("\n");
                }
                indexLine++;
            } while (!errorEnd);

            String trace = errorBuilder.toString();

            for (ITestRunListener listener : mTestListeners) {
                listener.testFailed(mCurrentTestIndentifier, trace);
            }
            mCurrentTestIndentifier = null;
            return indexLine;
        }
    }
}
