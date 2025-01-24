package com.android.ddmlib.testrunner;

import com.android.annotations.NonNull;
import com.android.ddmlib.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Runs a UI Automator test command remotely and reports results.
 */
public class UIAutomatorRemoteAndroidTestRunner {

    private static final String LOG_TAG = "RemoteAndroidTest";
    // defined instrumentation argument names
    private static final String CLASS_ARG_NAME = "class";
    private static final String DEBUG_ARG_NAME = "debug";
    private final String jarFile;
    private final IDevice mRemoteDevice;
    /**
     * map of name-value instrumentation argument pairs
     */
    private final List<Entry<String, String>> mArgList;
    // default to no timeout
    private int mMaxTimeToOutputResponse = 0;
    private String mRunName = null;
    private InstrumentationResultParser mParser;
    private boolean noHup;
    private Object dumpFilePath;

    public UIAutomatorRemoteAndroidTestRunner(String jarFile, IDevice remoteDevice) {
        this.jarFile = jarFile;
        mRemoteDevice = remoteDevice;
        mArgList = new ArrayList<>();
    }

    /**
     * Sets the test class or method to be run. This method must be called before calling
     * {@link #run(ITestRunListener...)}.
     *
     * @param testClassOrMethods a list of test classes or test methods.
     */
    public void setTestClassOrMethods(@NonNull String[] testClassOrMethods) {
        for (String testClassOrMethod : testClassOrMethods) {
            addInstrumentationArg(CLASS_ARG_NAME, testClassOrMethod);
        }
    }

    /**
     * Adds an instrumentation argument to be passed to the test runner.
     *
     * @param name  the argument name
     * @param value the argument value. May be null.
     */
    public void addInstrumentationArg(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name or value arguments cannot be null");
        }
        mArgList.add(new AbstractMap.SimpleImmutableEntry<>(name, value));
    }

    /**
     * Adds an instrumentation boolean argument to be passed to the test runner.
     *
     * @param name  the argument name
     * @param value the argument value.
     */
    public void addBooleanArg(String name, boolean value) {
        addInstrumentationArg(name, Boolean.toString(value));
    }

    /**
     * Set the {@code debug} flag. If true, the command will be run with debug.
     *
     * @param debug if true, the command will be run with debug.
     */

    public void setDebug(boolean debug) {
        addBooleanArg(DEBUG_ARG_NAME, debug);
    }

    /**
     * Set the {@code nohup} flag. If true, the command will be run with nohup.
     *
     * @param noHup if true, the command will be run with nohup.
     */
    public void setNoHup(boolean noHup) {
        this.noHup = noHup;
    }

    /**
     * Set the dump file path.
     *
     * @param dumpFilePath the dump file path.
     */
    public void setDumpFilePath(String dumpFilePath) {
        this.dumpFilePath = dumpFilePath;
    }

    /**
     * Sets the max time the shell command will wait while receiving instrumentation results from the device.
     *
     * @param maxTimeToOutputResponse the max time to wait for instrumentation to handle the test output. A value of 0
     *                                means wait forever.
     */
    public void setMaxtimeToOutputResponse(int maxTimeToOutputResponse) {
        mMaxTimeToOutputResponse = maxTimeToOutputResponse;
    }

    /**
     * Sets the Run's name.
     *
     * @param runName the run name to report in logs and instrumentation results. May be null.
     */
    public void setRunName(String runName) {
        mRunName = runName;
    }

    /**
     * Builds the command line syntax for the instrumentation arguments and executes the command.
     *
     * @param listeners {@link ITestRunListener}s that will receive test results. May be empty.
     */
    public void run(ITestRunListener... listeners) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        run(Arrays.asList(listeners));
    }

    /**
     * Builds the command line syntax for the instrumentation arguments and executes the command.
     *
     * @param listeners {@link ITestRunListener}s that will receive test results. May be empty.
     */
    public void run(Collection<ITestRunListener> listeners) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        final String runCaseCommandStr = String.format("uiautomator runtest %1$s %2$s", jarFile, buildArgsCommand());
        Log.i(LOG_TAG, String.format("Running %1$s on %2$s", runCaseCommandStr, mRemoteDevice.getSerialNumber()));
        mParser = new InstrumentationResultParser(mRunName, listeners);

        try {
            mRemoteDevice.executeShellCommand(runCaseCommandStr, mParser, mMaxTimeToOutputResponse);
        } catch (IOException e) {
            Log.w(LOG_TAG, String.format("IOException %1$s when running tests %2$s on %3$s", e, jarFile,
                    mRemoteDevice.getSerialNumber()));
            // rely on parser to communicate results to listeners
            mParser.handleTestRunFailed(e.toString());
            throw e;
        } catch (ShellCommandUnresponsiveException e) {
            Log.w(LOG_TAG,
                    String.format("ShellCommandUnresponsiveException %1$s when running tests %2$s on %3$s",
                            e, jarFile, mRemoteDevice.getSerialNumber()));
            mParser.handleTestRunFailed(String.format("Failed to receive adb shell test output within %1$d ms. "
                            + "Test may have timed out, or adb connection to device became unresponsive",
                    mMaxTimeToOutputResponse));
            throw e;
        } catch (TimeoutException e) {
            Log.w(LOG_TAG,
                    String.format("TimeoutException when running tests %1$s on %2$s", jarFile,
                            mRemoteDevice.getSerialNumber()));
            mParser.handleTestRunFailed(e.toString());
            throw e;
        } catch (AdbCommandRejectedException e) {
            Log.w(LOG_TAG, String.format("AdbCommandRejectedException %1$s when running tests %2$s on %3$s",
                    e, jarFile, mRemoteDevice.getSerialNumber()));
            mParser.handleTestRunFailed(e.toString());
            throw e;
        }
    }

    /**
     * Cancels the test run.
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
            final String argCmd = String.format(" -e %1$s %2$s", argPair.getKey(), argPair.getValue());
            commandBuilder.append(argCmd);
        }

        if (noHup) {
            commandBuilder.append(" --nohup");
        }

        if (dumpFilePath != null) {
            commandBuilder.append(" dump " + dumpFilePath);
        }
        return commandBuilder.toString();
    }

    /**
     * Adds instrumentation arguments from userProperties from keys with the propertiesKeyPrefix prefix.
     *
     * @param userProperties      the properties to load the arguments from
     * @param propertiesKeyPrefix the prefix of the properties to load
     */
    public void setUserProperties(Properties userProperties, String propertiesKeyPrefix) {
        if (userProperties == null) {
            throw new IllegalArgumentException("userProperties  cannot be null");
        }

        if (StringUtils.isBlank(propertiesKeyPrefix)) {
            //propertiesPrefix is blank, ignore all properties
            return;
        }

        for (Entry<Object, Object> property : userProperties.entrySet()) {
            String name = (String) property.getKey();

            //Check if the key starts with the parameterPrefix
            if (StringUtils.startsWith(name, propertiesKeyPrefix)) {
                String value = (String) property.getValue();

                //Remove the prefix
                name = StringUtils.substring(name,
                        StringUtils.length(propertiesKeyPrefix),
                        StringUtils.length(name));

                // Verify so the key isn't blank after substring
                if (StringUtils.isNotBlank(name)) {
                    //Now it's safe to add the parameter
                    addInstrumentationArg(name, value);
                }
            }
        }
    }
}
