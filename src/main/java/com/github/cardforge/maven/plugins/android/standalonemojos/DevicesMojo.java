package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.android.annotations.Nullable;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.common.DeviceHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * DevicesMojo lists all attached devices and emulators found with the android debug bridge. It uses the same
 * naming convention for the emulator as used in other places in the Android Maven Plugin and adds the status
 * of the device in the list.
 * <p>
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
@SuppressWarnings("unused") // used in Maven goals
@Mojo(name = "devices", requiresProject = false)
public class DevicesMojo extends AbstractAndroidMojo {
    /**
     * Flag to control verbose output. If true, more detailed information about devices will be displayed.
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose;

    /**
     * The specific device or emulator to filter by. If set, only this device will be displayed.
     */
    @Parameter(property = "android.device")
    private String targetDevice;

    /**
     * Display a list of attached devices.
     *
     * @throws MojoExecutionException if the execution fails
     * @throws MojoFailureException   if the execution fails
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        // If a target device is set, we filter for that device
        if (targetDevice != null && !targetDevice.isEmpty()) {
            IDevice target = findDeviceByName(targetDevice);
            if (target != null) {
                getLog().info("Device: " + DeviceHelper.getDescriptiveNameWithStatus(target));
            } else {
                throw new MojoFailureException("Device with name " + targetDevice + " not found.");
            }
        } else {
            doWithDevices(device -> {
                String deviceInfo = DeviceHelper.getDescriptiveNameWithStatus(device);

                // If verbose is true, include additional details about the device
                if (verbose) {
                    String sb =
                            "    Version: " + device.getVersion() + "\n" +
                                    "    Serial: " + device.getSerialNumber() + "\n" +
                                    "    Emulator: " + device.isEmulator() + "\n" +
                                    "    ABIs: " + device.getAbis();
                    deviceInfo += " (Verbose Info: " + sb + ")";
                }

                getLog().info(deviceInfo);
            });
        }
    }

    /**
     * Finds a device by its name using the Android Debug Bridge (ADB).
     *
     * @param deviceName the name of the device
     * @return the device if found, otherwise null
     */
    @Nullable
    private IDevice findDeviceByName(String deviceName) throws MojoFailureException {
        AndroidDebugBridge adb = AndroidDebugBridge.createBridge();

        if (adb == null) {
            throw new MojoFailureException("Unable to create ADB bridge.");
        }

        // Wait for ADB to finish connecting to devices (optional, could be skipped if already connected)
        adb.init(true);
        IDevice[] devices = adb.getDevices();

        for (IDevice device : devices) {
            if (device.getName().equals(deviceName)) {
                return device;  // Return the device if it matches the provided name
            }
        }

        return null;  // No device found with the specified name
    }
}
