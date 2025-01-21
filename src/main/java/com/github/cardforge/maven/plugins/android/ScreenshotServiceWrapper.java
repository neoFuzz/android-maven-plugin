package com.github.cardforge.maven.plugins.android;

import com.android.annotations.NonNull;
import com.android.ddmlib.IDevice;
import com.github.rtyley.android.screenshot.paparazzo.OnDemandScreenshotService;
import com.github.rtyley.android.screenshot.paparazzo.processors.AnimatedGifCreator;
import com.github.rtyley.android.screenshot.paparazzo.processors.ImageSaver;
import com.github.rtyley.android.screenshot.paparazzo.processors.ImageScaler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

import static com.github.cardforge.maven.plugins.android.common.DeviceHelper.getDescriptiveName;
import static com.github.rtyley.android.screenshot.paparazzo.processors.util.Dimensions.square;
import static org.apache.commons.io.FileUtils.forceMkdir;

/**
 * ScreenshotServiceWrapper wraps the feature to capture a screenshot during an instrumentation test run.
 */
public class ScreenshotServiceWrapper implements DeviceCallback {

    private static final int MAX_BOUNDS = 320;
    private final DeviceCallback delegate;
    private final Log log;
    private final File screenshotParentDir;

    /**
     * Constructor for ScreenshotServiceWrapper.
     *
     * @param delegate the delegate.
     * @param project  the project.
     * @param log      the logger to use.
     */
    public ScreenshotServiceWrapper(DeviceCallback delegate, @NonNull MavenProject project, Log log) {
        this.delegate = delegate;
        this.log = log;
        screenshotParentDir = new File(project.getBuild().getDirectory(), "screenshots");
        create(screenshotParentDir);
    }


    /**
     * Do actions with device.
     *
     * @param device the device
     * @throws MojoExecutionException in case of error1
     * @throws MojoFailureException   in case of error2
     */
    @Override
    public void doWithDevice(final IDevice device) throws MojoExecutionException, MojoFailureException {
        String deviceName = getDescriptiveName(device);

        File deviceGifFile = new File(screenshotParentDir, deviceName + ".gif");
        File deviceScreenshotDir = new File(screenshotParentDir, deviceName);
        create(deviceScreenshotDir);


        OnDemandScreenshotService screenshotService = new OnDemandScreenshotService(device,
                new ImageSaver(deviceScreenshotDir),
                new ImageScaler(new AnimatedGifCreator(deviceGifFile), square(MAX_BOUNDS)));

        screenshotService.start();

        delegate.doWithDevice(device);

        screenshotService.finish();
    }

    /**
     * Create a directory if it does not exist.
     *
     * @param dir the directory to create
     */
    private void create(File dir) {
        try {
            forceMkdir(dir);
        } catch (IOException e) {
            log.warn("Unable to create screenshot directory: " + screenshotParentDir.getAbsolutePath(), e);
        }
    }
}
