package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;
import com.android.ddmlib.IDevice;
import org.apache.commons.lang3.StringUtils;

/**
 * A bunch of helper methods for dealing with IDevice instances.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class DeviceHelper {

    /**
     * The property name for the manufacturer of the device.
     */
    private static final String MANUFACTURER_PROPERTY = "ro.product.manufacturer";
    /**
     * The property name for the model of the device.
     */
    private static final String MODEL_PROPERTY = "ro.product.model";
    /**
     * Separator for device identifiers.
     */
    private static final String SEPARATOR = "_";

    /**
     * Private constructor to prevent instantiation.
     */
    private DeviceHelper() {
        // no instances
    }

    /**
     * Get a device identifier string that is suitable for filenames as well as log messages.
     * This means it is human-readable and contains no spaces.
     * Used for instrumentation test report file names so see more at
     * AbstractInstrumentationMojo#testCreateReport javadoc since
     * that is the public documentation.
     *
     * @param device the device to get the identifier for
     * @return the device identifier string
     */
    @NonNull
    public static String getDescriptiveName(@NonNull IDevice device) {
        // if any of this logic changes update javadoc for
        // AbstractInstrumentationMojo#testCreateReport
        StringBuilder builder = new StringBuilder().append(device.getSerialNumber());
        if (device.getAvdName() != null) {
            builder.append(SEPARATOR).append(device.getAvdName());
        }
        String manufacturer = getManufacturer(device);
        if (StringUtils.isNotBlank(manufacturer)) {
            builder.append(SEPARATOR).append(manufacturer);
        }
        String model = getModel(device);
        if (StringUtils.isNotBlank(model)) {
            builder.append(SEPARATOR).append(model);
        }

        return FileNameHelper.fixFileName(builder.toString());
    }

    /**
     * @param device the device to get the prefix for
     * @return the prefix for log messages for the given device
     */
    @NonNull
    public static String getDeviceLogLinePrefix(IDevice device) {
        return getDescriptiveName(device) + " :   ";
    }

    /**
     * @param device the device to get the manufacturer for
     * @return the manufacturer of the device as set in #MANUFACTURER_PROPERTY, typically "unknown" for emulators
     */
    @NonNull
    public static String getManufacturer(@NonNull IDevice device) {
        return StringUtils.deleteWhitespace(device.getProperty(MANUFACTURER_PROPERTY));
    }

    /**
     * @param device the device to get the model for
     * @return the model of the device as set in #MODEL_PROPERTY, typically "sdk" for emulators
     */
    public static String getModel(@NonNull IDevice device) {
        return StringUtils.deleteWhitespace(device.getProperty(MODEL_PROPERTY));
    }

    /**
     * @param device the device to get the descriptive name for
     * @return the descriptive name with online/offline/unknown status string appended.
     */
    @NonNull
    public static String getDescriptiveNameWithStatus(@NonNull IDevice device) {
        String status;
        if (device.isOnline()) {
            status = "Online";
        } else {
            if (device.isOffline()) {
                status = "Offline";
            } else {
                status = "Unknown";
            }
        }
        return getDescriptiveName(device) + " " + status;
    }
}
