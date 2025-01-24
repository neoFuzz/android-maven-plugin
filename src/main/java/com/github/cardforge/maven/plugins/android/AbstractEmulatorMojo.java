/*
 * Copyright (C) 2009, 2010 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cardforge.maven.plugins.android;

import com.android.annotations.NonNull;
import com.android.ddmlib.*;
import com.github.cardforge.maven.plugins.android.common.DeviceHelper;
import com.github.cardforge.maven.plugins.android.configuration.Emulator;
import com.github.cardforge.maven.plugins.android.standalonemojos.EmulatorStartMojo;
import com.github.cardforge.maven.plugins.android.standalonemojos.EmulatorStopAllMojo;
import com.github.cardforge.maven.plugins.android.standalonemojos.EmulatorStopMojo;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * AbstractEmulatorMojo contains all code related to the interaction with the Android emulator. At this stage that is
 * starting and stopping the emulator.
 *
 * @author Manfred Moser - manfred@simpligility.com
 * @author Bryan O'Neil - bryan.oneil@hotmail.com
 * @see Emulator
 * @see EmulatorStartMojo
 * @see EmulatorStopMojo
 * @see EmulatorStopAllMojo
 */
public abstract class AbstractEmulatorMojo extends AbstractAndroidMojo {
    /**
     * operating system name.
     */
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);
    /**
     * Static string for "found"
     */
    public static final String FOUND = "Found ";
    /**
     * Static string for "devices" log message
     */
    public static final String ANDROID_DEBUG_BRIDGE = " devices connected with the Android Debug Bridge";
    /**
     * Milliseconds to sleep between device checks
     */
    private static final int MILLIS_TO_SLEEP_BETWEEN_DEVICE_ONLINE_CHECKS = 200;
    /**
     * Even if the device finished booting, there are usually still some things going on in the background,
     * polling at a higher frequency (un-cached!) is most probably useless
     */
    private static final int MILLIS_TO_SLEEP_BETWEEN_SYS_BOOTED_CHECKS = 5000;
    /**
     * Names of device properties related to the boot state
     */
    private static final String[] BOOT_INDICATOR_PROP_NAMES
            = {"dev.bootcomplete", "sys.boot_completed", "init.svc.bootanim"};
    /**
     * Target values for properties listed in {@link #BOOT_INDICATOR_PROP_NAMES}, which indicate 'boot completed'
     */
    private static final String[] BOOT_INDICATOR_PROP_TARGET_VALUES = {"1", "1", "stopped"};
    /**
     * Determines, which of the properties listed in {@link #BOOT_INDICATOR_PROP_NAMES} are required
     * to reach the target value in {@link #BOOT_INDICATOR_PROP_TARGET_VALUES} in order to stop polling.
     * Since one cannot be picky about what is used as a 'booted' indicator, any 'signalled' property will
     * be used as an indicator in case of a timeout.
     */
    private static final boolean[] BOOT_INDICATOR_PROP_WAIT_FOR = {false, false, true};
    /**
     * Warning threshold for narrow timeout values
     * TODO Improve; e.g. with an additional percentage threshold
     */
    private static final long START_TIMEOUT_REMAINING_TIME_WARNING_THRESHOLD = 5000; //[ms]
    private static final String START_EMULATOR_MSG = "Starting android emulator with script: ";
    private static final String START_EMULATOR_WAIT_MSG = "Waiting for emulator start:";
    /**
     * Folder that contains the startup script and the pid file.
     */
    private static final String SCRIPT_FOLDER = System.getProperty("java.io.tmpdir");
    /**
     * Configuration for the emulator goals. Either use the plugin configuration like this
     * <pre>
     * &lt;emulator&gt;
     *   &lt;avd&gt;Default&lt;/avd&gt;
     *   &lt;wait&gt;20000&lt;/wait&gt;
     *   &lt;options&gt;-no-skin&lt;/options&gt;
     *   &lt;executable&gt;emulator-arm&lt;/executable&gt;
     *   &lt;location&gt;C:/SDK/emulator&lt;/location&gt;
     * &lt;/emulator&gt;
     * </pre>
     * or configure as properties on the command line as {@code android.emulator.avd}, {@code android.emulator.wait},
     * {@code android.emulator.options} and {@code android.emulator.executable} or in pom or settings file as {@code emulator.avd},
     * {@code emulator.wait} and {@code emulator.options}.
     */
    @Parameter
    private Emulator emulator;
    /**
     * Name of the Android Virtual Device (emulatorAvd) that will be started by the emulator. Default value is "Default"
     *
     * @see Emulator#avd
     */
    @Parameter(property = "android.emulator.avd")
    private String emulatorAvd;
    /**
     * Unlock the emulator after it is started.
     */
    @Parameter(property = "android.emulatorUnlock", defaultValue = "false")
    private boolean emulatorUnlock;
    /**
     * Wait time for the emulator start up.
     *
     * @see Emulator#wait
     */
    @Parameter(property = "android.emulator.wait")
    private String emulatorWait;
    /**
     * Additional command line options for the emulator start up. This option can be used to pass any additional
     * options desired to the invocation of the emulator. Use emulator -help for more details. An example would be
     * "-no-skin".
     *
     * @see Emulator#options
     */
    @Parameter(property = "android.emulator.options")
    private String emulatorOptions;
    /**
     * Override default emulator executable. Default uses just "emulator".
     *
     * @see Emulator#executable
     */
    @Parameter(property = "android.emulator.executable")
    private String emulatorExecutable;
    /**
     * Override default path to emulator folder.
     */
    @Parameter(property = "android.emulator.location")
    private String emulatorLocation;
    /**
     * parsed value for avd that will be used for the invocation.
     */
    private String parsedAvd;
    /**
     * parsed value for options that will be used for the invocation.
     */
    private String parsedOptions;
    /**
     * parsed value for wait that will be used for the invocation.
     */
    private String parsedWait;
    private String parsedExecutable;
    /**
     * parsed value for location that will be used for the invocation.
     */
    private String parsedEmulatorLocation;

    /**
     * Are we running on a flavour of Windows?
     *
     * @return <code>true</code> if we are running on a flavour of Windows, <code>false</code> otherwise.
     */
    private boolean isWindows() {
        boolean result;
        result = OS_NAME.toLowerCase().contains("windows");
        getLog().debug("isWindows: " + result);
        return result;
    }

    /**
     * Start the Android Emulator with the specified options.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException if an error occurs
     * @see #emulatorAvd
     * @see #emulatorWait
     * @see #emulatorOptions
     */
    public void startAndroidEmulator() throws MojoExecutionException {
        parseParameters();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        try {
            String filename;
            if (isWindows()) {
                filename = writeEmulatorStartScriptWindows();
            } else {
                filename = writeEmulatorStartScriptUnix();
            }

            final AndroidDebugBridge androidDebugBridge = initAndroidDebugBridge();
            if (androidDebugBridge.isConnected()) {
                waitForInitialDeviceList(androidDebugBridge);
                List<IDevice> devices = Arrays.asList(androidDebugBridge.getDevices());
                int numberOfDevices = devices.size();
                getLog().info(FOUND + numberOfDevices + ANDROID_DEBUG_BRIDGE);

                IDevice existingEmulator = findExistingEmulator(devices);
                if (existingEmulator == null) {
                    getLog().info(START_EMULATOR_MSG + filename);
                    executor.executeCommand(filename, null);

                    getLog().info(START_EMULATOR_WAIT_MSG + parsedWait);
                    // wait for the emulator to start up
                    boolean booted = waitUntilDeviceIsBootedOrTimeout(androidDebugBridge);
                    if (booted) {
                        getLog().info("Emulator is up and running.");
                        unlockEmulator(androidDebugBridge);
                    } else {
                        throw new MojoExecutionException("Timeout while waiting for emulator to startup.");
                    }

                } else {
                    getLog().info(String.format(
                            "Emulator already running [Serial No: '%s', AVD Name '%s']. " + "Skipping start and wait.",
                            existingEmulator.getSerialNumber(), existingEmulator.getAvdName()));
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * Unlocks the emulator.
     *
     * @param androidDebugBridge the Android Debug Bridge
     */
    void unlockEmulator(AndroidDebugBridge androidDebugBridge) {
        if (emulatorUnlock) {
            IDevice myEmulator = findExistingEmulator(Arrays.asList(androidDebugBridge.getDevices()));
            int devicePort = extractPortFromDevice(myEmulator);
            if (devicePort == -1) {
                getLog().info("Unable to retrieve port to unlock emulator "
                        + DeviceHelper.getDescriptiveName(myEmulator));
            } else {
                getLog().info("Unlocking emulator "
                        + DeviceHelper.getDescriptiveName(myEmulator));

                sendEmulatorCommand(devicePort,
                        "event send EV_KEY:KEY_SOFT1:1");
                sendEmulatorCommand(devicePort,
                        "event send EV_KEY:KEY_SOFT1:0");
                sendEmulatorCommand(devicePort,
                        "event send EV_KEY:KEY_SOFT1:1");
                sendEmulatorCommand(devicePort,
                        "event send EV_KEY:KEY_SOFT1:0");
            }
        }
    }

    // TODO Separate timeout params?: New param 'android.emulator.bootTimeout', rename param 'android.emulator.wait' to 'android.emulator.connectTimeout'
    // TODO Higher default timeout(s)?: Perhaps at least for emulators, since they are probably booted or even created on demand
    boolean waitUntilDeviceIsBootedOrTimeout(@NonNull AndroidDebugBridge androidDebugBridge)
            throws MojoExecutionException {
        final long timeout = System.currentTimeMillis() + Long.parseLong(parsedWait);
        IDevice myEmulator;
        boolean devOnline;
        boolean sysBootCompleted = false;
        long remainingTime;

        //If necessary, wait until the device is online or the specified timeout is reached
        boolean waitingForConnection = false;
        do {
            myEmulator = findExistingEmulator(Arrays.asList(androidDebugBridge.getDevices()));
            devOnline = (myEmulator != null) && (myEmulator.isOnline());
            if (devOnline) {
                break;
            } else {
                myEmulator = null;
            }

            if (!waitingForConnection) {
                waitingForConnection = true;
                getLog().info("Waiting for the device to go online...");
            }
            try {
                Thread.sleep(MILLIS_TO_SLEEP_BETWEEN_DEVICE_ONLINE_CHECKS);
            } catch (InterruptedException e) {
                throw new MojoExecutionException("Interrupted waiting for device to become ready");
            }

            remainingTime = timeout - System.currentTimeMillis();
        } while (remainingTime > 0);

        if (devOnline) {
            boolean waitingForBootCompleted = false;
            final String[] bootIndicatorPropValues = new String[BOOT_INDICATOR_PROP_NAMES.length];
            boolean anyTargetStateReached = false;
            boolean requiredTargetStatesReached = false;

            // If necessary, wait until the device's system is booted or the specified timeout is reached
            do {
                try {
                    // update state flags...
                    anyTargetStateReached = false;
                    requiredTargetStatesReached = true;

                    for (int indicatorProp = 0; indicatorProp < BOOT_INDICATOR_PROP_NAMES.length; ++indicatorProp) {
                        // issue an un-cached property request
                        boolean targetStateReached =
                                (
                                        bootIndicatorPropValues[indicatorProp] != null
                                                && bootIndicatorPropValues[indicatorProp]
                                                .equals(BOOT_INDICATOR_PROP_TARGET_VALUES[indicatorProp])
                                );
                        if (!targetStateReached) {
                            // (re)query
                            bootIndicatorPropValues[indicatorProp] =
                                    myEmulator.getPropertySync(BOOT_INDICATOR_PROP_NAMES[indicatorProp]);
                            targetStateReached =
                                    (
                                            bootIndicatorPropValues[indicatorProp] != null
                                                    && bootIndicatorPropValues[indicatorProp]
                                                    .equals(BOOT_INDICATOR_PROP_TARGET_VALUES[indicatorProp])
                                    );
                        }
                        anyTargetStateReached |= targetStateReached;
                        requiredTargetStatesReached &=
                                !BOOT_INDICATOR_PROP_WAIT_FOR[indicatorProp] || targetStateReached;

                        getLog().debug(BOOT_INDICATOR_PROP_NAMES[indicatorProp]
                                + " : " + bootIndicatorPropValues[indicatorProp]
                                + (targetStateReached ? " == " : " != ")
                                + BOOT_INDICATOR_PROP_TARGET_VALUES[indicatorProp]
                                + " [" + (targetStateReached ? "OK" : "PENDING") + ']'
                        );
                    }
                } catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException e) {
                    // TODO Abort here? Not too problematic since timeouts are used
                    // optimistically ignore this exception and continue...
                } catch (IOException e) {
                    throw new MojoExecutionException("IO error during status request", e);
                }

                remainingTime = timeout - System.currentTimeMillis();

                if (remainingTime > 0) {
                    // consider the boot process to be finished, if all required states have been reached
                    sysBootCompleted = requiredTargetStatesReached;
                } else {
                    // on timeout, use any indicator
                    sysBootCompleted = anyTargetStateReached;
                }

                if (remainingTime > 0 && !sysBootCompleted) {
                    if (!waitingForBootCompleted) {
                        waitingForBootCompleted = true;
                        getLog().info("Waiting for the device to finish booting...");
                    }

                    try {
                        Thread.sleep(MILLIS_TO_SLEEP_BETWEEN_SYS_BOOTED_CHECKS);
                    } catch (InterruptedException e) {
                        throw new MojoExecutionException(
                                "Interrupted while waiting for the device to finish booting");
                    }
                }
            } while (!sysBootCompleted && remainingTime > 0);
            if (sysBootCompleted && remainingTime < START_TIMEOUT_REMAINING_TIME_WARNING_THRESHOLD) {
                getLog().warn(
                        "Boot indicators have been signalled, but remaining time was " + remainingTime + " ms");
            }
        }
        return sysBootCompleted;
    }

    private IDevice findExistingEmulator(@NonNull List<IDevice> devices) {
        IDevice existingEmulator = null;

        for (IDevice device : devices) {
            if (device.isEmulator()) {
                if (isExistingEmulator(device)) {
                    existingEmulator = device;
                    break;
                }
            }
        }
        return existingEmulator;
    }

    /**
     * Checks whether the given device has the same AVD name as the device which the current command
     * is related to. <code>true</code> returned if the device AVD names are identical (independent of case)
     * and <code>false</code> if the device AVD names are different.
     *
     * @param device The device to check
     * @return Boolean results of the check
     */
    private boolean isExistingEmulator(@NonNull IDevice device) {
        return ((device.getAvdName() != null) && (device.getAvdName().equalsIgnoreCase(parsedAvd)));
    }

    /**
     * Writes the script to start the emulator in the background for windows based environments.
     *
     * @return absolute path name of start script
     * @throws MojoExecutionException If something goes wrong.
     */
    @NonNull
    private String writeEmulatorStartScriptWindows() throws MojoExecutionException {

        String filename = SCRIPT_FOLDER + "\\android-maven-plugin-emulator-start.vbs";

        File file = new File(filename);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file));


            // command needs to be assembled before unique window title since it parses settings and sets up parsedAvd
            // and others.
            String command = assembleStartCommandLine();
            String uniqueWindowTitle = "AndroidMavenPlugin-AVD" + parsedAvd;
            writer.println("Dim oShell");
            writer.println("Set oShell = WScript.CreateObject(\"WScript.shell\")");
            String cmdPath = System.getenv("COMSPEC");
            if (cmdPath == null) {
                cmdPath = "cmd.exe";
            }
            String cmd = cmdPath + " /X /C START /SEPARATE \"\"" + uniqueWindowTitle + "\"\"  " + command.trim();
            writer.println("oShell.run \"" + cmd + "\"");
        } catch (IOException e) {
            getLog().error("Failure writing file " + filename);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
        file.setExecutable(true);
        return filename;
    }

    /**
     * Writes the script to start the emulator in the background for unix based environments.
     *
     * @return absolute path name of start script
     * @throws IOException            If something goes wrong.
     * @throws MojoExecutionException If something goes wrong.
     */
    @NonNull
    private String writeEmulatorStartScriptUnix() throws MojoExecutionException {
        String filename = SCRIPT_FOLDER + "/android-maven-plugin-emulator-start.sh";

        File sh;
        sh = new File("/bin/bash");
        if (!sh.exists()) {
            sh = new File("/usr/bin/bash");
        }
        if (!sh.exists()) {
            sh = new File("/bin/sh");
        }

        File file = new File(filename);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file));
            writer.println("#!" + sh.getAbsolutePath());
            writer.print(assembleStartCommandLine());
            writer.print(" 1>/dev/null 2>&1 &"); // redirect outputs and run as background task
        } catch (IOException e) {
            getLog().error("Failure writing file " + filename);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
        file.setExecutable(true);
        return filename;
    }

    /**
     * Stop the running Android Emulator.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException If something goes wrong.
     */
    protected void stopAndroidEmulator() throws MojoExecutionException {
        parseParameters();

        final AndroidDebugBridge androidDebugBridge = initAndroidDebugBridge();
        if (androidDebugBridge.isConnected()) {
            List<IDevice> devices = Arrays.asList(androidDebugBridge.getDevices());
            int numberOfDevices = devices.size();
            getLog().info(FOUND + numberOfDevices + ANDROID_DEBUG_BRIDGE);

            for (IDevice device : devices) {
                if (device.isEmulator()) {
                    if (isExistingEmulator(device)) {
                        stopEmulator(device);
                    }
                } else {
                    getLog().info("Skipping stop. Not an emulator. " + DeviceHelper.getDescriptiveName(device));
                }
            }
        }
    }

    /**
     * Stop the running Android Emulators.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException If something goes wrong.
     */
    protected void stopAndroidEmulators() throws MojoExecutionException {
        final AndroidDebugBridge androidDebugBridge = initAndroidDebugBridge();
        if (androidDebugBridge.isConnected()) {
            List<IDevice> devices = Arrays.asList(androidDebugBridge.getDevices());
            int numberOfDevices = devices.size();
            getLog().info(FOUND + numberOfDevices + ANDROID_DEBUG_BRIDGE);

            for (IDevice device : devices) {
                if (device.isEmulator()) {
                    stopEmulator(device);
                } else {
                    getLog().info("Skipping stop. Not an emulator. " + DeviceHelper.getDescriptiveName(device));
                }
            }
        }
    }

    /**
     * This method contains the code required to stop an emulator
     *
     * @param device The device to stop
     */
    private void stopEmulator(IDevice device) {
        int devicePort = extractPortFromDevice(device);
        if (devicePort == -1) {
            getLog().info("Unable to retrieve port to stop emulator " + DeviceHelper.getDescriptiveName(device));
        } else {
            getLog().info("Stopping emulator " + DeviceHelper.getDescriptiveName(device));

            sendEmulatorCommand(devicePort, "avd stop");
            boolean killed = sendEmulatorCommand(devicePort, "kill");
            if (!killed) {
                getLog().info("Emulator failed to stop " + DeviceHelper.getDescriptiveName(device));
            } else {
                getLog().info("Emulator stopped successfully " + DeviceHelper.getDescriptiveName(device));
            }
        }
    }

    /**
     * This method extracts a port number from the serial number of a device.
     * It assumes that the device name is of format [xxxx-nnnn] where nnnn is the
     * port number.
     *
     * @param device The device to extract the port number from.
     * @return Returns the port number of the device
     */
    private int extractPortFromDevice(@NonNull IDevice device) {
        String portStr = StringUtils.substringAfterLast(device.getSerialNumber(), "-");
        if (StringUtils.isNotBlank(portStr) && StringUtils.isNumeric(portStr)) {
            return Integer.parseInt(portStr);
        }

        //If the port is not available then return -1
        return -1;
    }

    /**
     * Sends a user command to the running emulator via its telnet interface.
     *
     * @param port    The emulator's telnet port.
     * @param command The command to execute on the emulator's telnet interface.
     * @return Whether sending the command succeeded.
     */
    private boolean sendEmulatorCommand(
            //final Launcher launcher,
            //final PrintStream logger,
            final int port, final String command) {
        Callable<Boolean> task = new Callable<>() {
            private static final long serialVersionUID = 1L;

            @NonNull
            public Boolean call() throws IOException {
                Socket socket = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    socket = new Socket("127.0.0.1", port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if (in.readLine() == null) {
                        return false;
                    }

                    out.write(command);
                    out.write("\r\n");
                } finally {
                    try {
                        out.close();
                        in.close();
                        socket.close();
                    } catch (Exception e) {
                        // Do nothing
                    }
                }

                return true;
            }
        };

        boolean result = false;
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(task);
            result = future.get();
        } catch (Exception e) {
            getLog().error(String.format("Failed to execute emulator command '%s': %s", command, e));
        }

        return result;
    }

    /**
     * Assemble the command line for starting the emulator based on the parameters supplied in the pom file and on the
     * command line. It should not be that painful to do work with command line and pom supplied values, but evidently
     * it is.
     *
     * @return the assembled command line for starting the emulator
     * @throws MojoExecutionException if there is a problem assembling the command line
     * @see Emulator
     */
    @NonNull
    private String assembleStartCommandLine() throws MojoExecutionException {
        String emulatorPath;
        if (!"SdkTools".equals(parsedEmulatorLocation)) {
            emulatorPath = new File(parsedEmulatorLocation, parsedExecutable).getAbsolutePath();
        } else {
            emulatorPath = new File(getAndroidSdk().getToolsPath(), parsedExecutable).getAbsolutePath();
        }
        StringBuilder startCommandline = new StringBuilder("\"\"").append(emulatorPath).append("\"\"")
                .append(" -avd ").append(parsedAvd).append(" ");
        if (!StringUtils.isEmpty(parsedOptions)) {
            startCommandline.append(parsedOptions);
        }
        getLog().info("Android emulator command: " + startCommandline);
        return startCommandline.toString();
    }

    private void parseParameters() {
        // <emulator> exist in pom file
        if (emulator != null) {
            // <emulator><avd> exists in pom file
            if (emulator.getAvd() != null) {
                parsedAvd = emulator.getAvd();
            } else {
                parsedAvd = determineAvd();
            }
            // <emulator><options> exists in pom file
            if (emulator.getOptions() != null) {
                parsedOptions = emulator.getOptions();
            } else {
                parsedOptions = determineOptions();
            }
            // <emulator><wait> exists in pom file
            if (emulator.getWait() != null) {
                parsedWait = emulator.getWait();
            } else {
                parsedWait = determineWait();
            }
            // <emulator><emulatorExecutable> exists in pom file
            if (emulator.getExecutable() != null) {
                parsedExecutable = emulator.getExecutable();
            } else {
                parsedExecutable = determineExecutable();
            }
            // <emulator><location> exists in pom file
            if (emulator.getLocation() != null) {
                parsedEmulatorLocation = emulator.getLocation();
            } else {
                parsedEmulatorLocation = determineEmulatorLocation();
            }
        }
        // commandline options
        else {
            parsedAvd = determineAvd();
            parsedOptions = determineOptions();
            parsedWait = determineWait();
            parsedExecutable = determineExecutable();
            parsedEmulatorLocation = determineEmulatorLocation();
        }
    }

    /**
     * Get executable value for emulator from command line options or default to "emulator".
     *
     * @return if available return command line value otherwise return default value ("emulator").
     */
    private String determineExecutable() {
        String emu;
        emu = Objects.requireNonNullElse(emulatorExecutable, "emulator");
        return emu;
    }

    /**
     * Get wait value for emulator from command line option.
     *
     * @return if available return command line value otherwise return default value (5000).
     */
    String determineWait() {
        String wait;
        wait = Objects.requireNonNullElse(emulatorWait, "5000");
        return wait;
    }

    /**
     * Get options value for emulator from command line option.
     *
     * @return if available return command line value otherwise return default value ("").
     */
    private String determineOptions() {
        String options;
        options = Objects.requireNonNullElse(emulatorOptions, "");
        return options;
    }

    /**
     * Get avd value for emulator from command line option.
     *
     * @return if available return command line value otherwise return default value ("Default").
     */
    String determineAvd() {
        String avd;
        avd = Objects.requireNonNullElse(emulatorAvd, "Default");
        return avd;
    }

    /**
     * Get location value for emulator from command line option.
     *
     * @return if available return command line value otherwise return default value ("SdkTools").
     */
    String determineEmulatorLocation() {
        String location;
        location = Objects.requireNonNullElse(emulatorLocation, "SdkTools");
        return location;
    }

}