/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.builder.testing;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.testing.api.DeviceConfig;
import com.android.builder.testing.api.DeviceConnector;
import com.android.builder.testing.api.DeviceException;
import com.android.ddmlib.*;
import com.android.utils.ILogger;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Local device connected to with ddmlib. This is a wrapper around {@link IDevice}.
 */
@SuppressWarnings({"unused", "UnstableApiUsage"})
public class ConnectedDevice extends DeviceConnector {

    /**
     * The device to wrap.
     */
    private final IDevice iDevice;

    /**
     * @param iDevice the device to wrap.
     */
    public ConnectedDevice(@NonNull IDevice iDevice) {
        this.iDevice = iDevice;
    }

    /**
     * @return the name of the device.
     */
    @NonNull
    @Override
    public String getName() {
        String version = iDevice.getProperty(IDevice.PROP_BUILD_VERSION);
        boolean emulator = iDevice.isEmulator();

        String name;
        if (emulator) {
            name = iDevice.getAvdName() != null ?
                    iDevice.getAvdName() + "(AVD)" :
                    iDevice.getSerialNumber();
        } else {
            String model = iDevice.getProperty(IDevice.PROP_DEVICE_MODEL);
            name = model != null ? model : iDevice.getSerialNumber();
        }

        return version != null ? name + " - " + version : name;
    }

    /**
     * @param timeout the time-out.
     * @param logger  the logger to use to log debug, warnings and errors.
     * @throws TimeoutException if the device could not be connected.
     */
    @Override
    public void connect(int timeout, ILogger logger) throws TimeoutException {
        // nothing to do here
    }

    /**
     * @param timeout the time-out.
     * @param logger  the logger to use to log debug, warnings and errors.
     */
    @Override
    public void disconnect(int timeout, ILogger logger) {
        // nothing to do here
    }

    /**
     * @param apkFile the APK file to install.
     * @param options options to use.
     * @param timeout the time-out.
     * @param logger  the logger to use to log debug, warnings and errors.
     * @throws DeviceException if the package could not be installed.
     */
    @Override
    public void installPackage(@NonNull File apkFile,
                               @NonNull Collection<String> options,
                               int timeout,
                               ILogger logger) throws DeviceException {
        try {
            iDevice.installPackage(apkFile.getAbsolutePath(), true /*reinstall*/,
                    options.isEmpty() ? null : options.toArray(new String[options.size()]));
        } catch (Exception e) {
            logger.error(e, "Unable to install " + apkFile.getAbsolutePath());
            throw new DeviceException(e);
        }
    }

    /**
     * @param splitApkFiles the APK files to install.
     * @param options       the install options.
     * @param timeoutInMs   the time-out in milliseconds.
     * @param logger        the logger to use to log debug, warnings and errors.
     * @throws DeviceException if the packages could not be installed.
     */
    @Override
    public void installPackages(@NonNull List<File> splitApkFiles,
                                @NonNull Collection<String> options,
                                int timeoutInMs,
                                ILogger logger)
            throws DeviceException {

        List<String> apkFileNames = Lists.transform(splitApkFiles, input -> input != null ? input.getAbsolutePath() : null);
        try {
            iDevice.installPackages(splitApkFiles, true /*reinstall*/,
                    options.isEmpty() ? new ArrayList<>() : (List<String>) options, timeoutInMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error(e, "Unable to install " + Joiner.on(',').join(apkFileNames));
            throw new DeviceException(e);
        }
    }

    /**
     * @param packageName the package name
     * @param timeout     the time-out
     * @param logger      the logger to use to log debug, warnings and errors.
     * @throws DeviceException if the package could not be uninstalled.
     */
    @Override
    public void uninstallPackage(@NonNull String packageName, int timeout, ILogger logger) throws DeviceException {
        try {
            iDevice.uninstallPackage(packageName);
        } catch (Exception e) {
            logger.error(e, "Unable to uninstall " + packageName);
            throw new DeviceException(e);
        }
    }

    /**
     * @param command                 the shell command to execute
     * @param receiver                the {@link IShellOutputReceiver} that will receive the output of the shell
     *                                command
     * @param maxTimeToOutputResponse the maximum amount of time during which the command is allowed
     *                                to not output any response. A value of 0 means the method will wait forever
     *                                (until the <var>receiver</var> cancels the execution) for command output and
     *                                never throw.
     * @param maxTimeUnits            Units for non-zero {@code maxTimeToOutputResponse} values.
     * @throws TimeoutException                  If the command did not complete within the specified time.
     * @throws AdbCommandRejectedException       If the command is rejected on device.
     * @throws ShellCommandUnresponsiveException If the shell stops responding
     * @throws IOException                       If the shell fails to execute the command.
     */
    @Override
    public void executeShellCommand(String command, IShellOutputReceiver receiver,
                                    long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
            throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        iDevice.executeShellCommand(command, receiver, maxTimeToOutputResponse, maxTimeUnits);
    }

    /**
     * @param command                 the shell command to execute
     * @param receiver                the {@link IShellOutputReceiver} that will receive the output of the shell
     *                                command
     * @param maxTimeout              the maximum timeout for the command to return. A value of 0 means no max
     *                                timeout will be applied.
     * @param maxTimeToOutputResponse the maximum amount of time during which the command is allowed
     *                                to not output any response. A value of 0 means the method will wait forever (until the
     *                                <var>receiver</var> cancels the execution) for command output and never throw.
     * @param maxTimeUnits            Units for non-zero {@code maxTimeout} and {@code maxTimeToOutputResponse}
     *                                values.
     */
    @Override
    public void executeShellCommand(String command, IShellOutputReceiver receiver, long maxTimeout,
                                    long maxTimeToOutputResponse, TimeUnit maxTimeUnits) {
        // empty
    }

    /**
     * @param name the name of the value to return.
     * @return the value of the system property or <code>null</code> if defined on the device.
     */
    @NonNull
    @Override
    public Future<String> getSystemProperty(@NonNull String name) {
        return iDevice.getSystemProperty(name);
    }

    /**
     * @param remote the full path to the remote file
     * @param local  The local destination.
     * @throws IOException if the pull failed.
     */
    @Override
    public void pullFile(String remote, String local) throws IOException {
        try {
            iDevice.pullFile(remote, local);

        } catch (TimeoutException | AdbCommandRejectedException | SyncException e) {
            throw new IOException(String.format("Failed to pull %s from device", remote), e);
        }
    }

    /**
     * @return the serial number of the device.
     */
    @NonNull
    @Override
    public String getSerialNumber() {
        return iDevice.getSerialNumber();
    }

    /**
     * @return the API level of the device or 0 if the platform is not an integer.
     */
    @Override
    public int getApiLevel() {
        String sdkVersion = iDevice.getProperty(IDevice.PROP_BUILD_API_LEVEL);
        if (sdkVersion != null) {
            try {
                return Integer.parseInt(sdkVersion);
            } catch (NumberFormatException ignored) {
                // nothing
            }
        }

        // can't get it, return 0.
        return 0;
    }

    /**
     * @return the API code name or {@code null} if the platform is a release platform.
     */
    @Override
    public String getApiCodeName() {
        String codeName = iDevice.getProperty(IDevice.PROP_BUILD_CODENAME);
        if (codeName != null) {
            // if this is a release platform return null.
            if ("REL".equals(codeName)) {
                return null;
            }

            // else return the codename
            return codeName;
        }

        // can't get it, return 0.
        return null;
    }

    /**
     * @return the {@link DeviceConfig} for this device.
     */
    @Nullable
    @Override
    public IDevice.DeviceState getState() {
        return iDevice.getState();
    }

    /**
     * @return the list of ABIs supported by the device.
     */
    @NonNull
    @Override
    public List<String> getAbis() {
        return iDevice.getAbis();
    }

    /**
     * @return the density of the device.
     */
    @Override
    public int getDensity() {
        return iDevice.getDensity();
    }

    /**
     * @return 0
     */
    @Override
    public int getHeight() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * @return 0
     */
    @Override
    public int getWidth() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * @return The string representation of the language in use.
     */
    @Override
    public String getLanguage() {
        return iDevice.getLanguage();
    }

    /**
     * @return The set of language splits in use.
     * @throws TimeoutException                  if the command failed to execute
     * @throws AdbCommandRejectedException       if the command failed to execute
     * @throws ShellCommandUnresponsiveException if the shell stops responding
     * @throws IOException                       if the command fails to execute
     */
    @Override
    public Set<String> getLanguageSplits() throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        return new Set<>() {
            /**
             * @return Always 0 since this set is always empty
             */
            @Override
            public int size() {
                return 0;
            }

            /**
             * @return Always {@code false} since this set is always empty
             */
            @Override
            public boolean isEmpty() {
                return false;
            }

            /**
             * @param o element whose presence in this set is to be tested
             * @return Always {@code false} since this set is always empty
             */
            @Override
            public boolean contains(Object o) {
                return false;
            }

            /**
             * @return Always null instead of an iterator
             */
            @NonNull
            @Override
            public Iterator<String> iterator() {
                return null;
            }

            /**
             * @return Always null instead of the array containing the elements of this set
             */
            @NonNull
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            /**
             * @param a   the array into which the elements of this set are to be
             *            stored, if it is big enough; otherwise, a new array of the same
             *            runtime type is allocated for this purpose.
             * @param <T> the class of the objects in the array
             * @return Always null instead of the array containing the elements of this set
             */
            @NonNull
            @Override
            public <T> T[] toArray(@NonNull T[] a) {
                return null;
            }

            /**
             * @param s element whose presence in this collection is to be ensured
             * @return Always {@code false}
             */
            @Override
            public boolean add(String s) {
                return false;
            }

            /**
             * @param o object to be removed from this set, if present
             * @return Always {@code false}
             */
            @Override
            public boolean remove(Object o) {
                return false;
            }

            /**
             * @param c collection to be checked for containment in this set
             * @return Always {@code false}
             */
            @Override
            public boolean containsAll(@NonNull Collection<?> c) {
                return false;
            }

            /**
             * @param c collection containing elements to be added to this collection
             * @return Always {@code false}
             */
            @Override
            public boolean addAll(@NonNull Collection<? extends String> c) {
                return false;
            }

            /**
             * @param c collection containing elements to be retained in this set
             * @return Always {@code false}
             */
            @Override
            public boolean retainAll(@NonNull Collection<?> c) {
                return false;
            }

            /**
             * @param c collection containing elements to be removed from this set
             * @return Always {@code false}
             */
            @Override
            public boolean removeAll(@NonNull Collection<?> c) {
                return false;
            }

            /**
             * Does nothing
             */
            @Override
            public void clear() {
                // nothing
            }
        };
    }


    /**
     * @return the region of the device, or null if the region is not set.
     */
    @Override
    public String getRegion() {
        return iDevice.getRegion();
    }

    /**
     * @param propertyName the name of the property to return.
     * @return the value of the property, or null if the property is not set.
     */
    @Override
    @NonNull
    public String getProperty(@NonNull String propertyName) {
        return iDevice.getProperty(propertyName);
    }

    /**
     * @param propertyName the name of the property to return.
     * @return the value of the property, or an empty string if the property is not set.
     */
    @Override
    public String getNullableProperty(String propertyName) {
        return "";
    }

    /**
     * @return the {@link DeviceConfig} for this device.
     * @throws DeviceException if the device config cannot be retrieved.
     */
    @NonNull
    @Override
    public DeviceConfig getDeviceConfig() throws DeviceException {
        final List<String> output = new ArrayList<>();
        final MultiLineReceiver receiver = new MultiLineReceiver() {
            /**
             * @param lines The array containing the new lines.
             */
            @Override
            public void processNewLines(String[] lines) {
                output.addAll(Arrays.asList(lines));
            }

            /**
             * @return false
             */
            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        try {
            executeShellCommand("am get-config", receiver, 5, TimeUnit.SECONDS);
            return DeviceConfig.Builder.parse(output);
        } catch (Exception e) {
            throw new DeviceException(e);
        }
    }
}
