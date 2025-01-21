package com.github.cardforge.maven.plugins.android.configuration;

import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.common.AndroidExtension;

import java.io.File;

/**
 * DeployApk is the configuration pojo for the DeployApk, UndeployApk and RedeployApk mojos.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class DeployApk {
    /**
     * This is the file to be deployed.
     */
    private File filename;
    /**
     * This is used to determine the package name of the application to be deployed.
     */
    private String packagename;

    /**
     * @param parsedFilename This is the file to be deployed.
     * @return A validation response.
     */
    @NonNull
    public static ValidationResponse validFileParameter(File parsedFilename) {
        ValidationResponse result;
        if (parsedFilename == null) {
            result = new ValidationResponse(false,
                    "\n\n The parameter android.deployapk.filename is missing. \n");
        } else if (!parsedFilename.isFile()) {
            result = new ValidationResponse(false,
                    "\n\n The file parameter does not point to a file: "
                            + parsedFilename.getAbsolutePath() + "\n");
        } else if (!parsedFilename.getAbsolutePath().toLowerCase().endsWith(AndroidExtension.APK)) {
            result = new ValidationResponse(false,
                    "\n\n The file parameter does not point to an APK: "
                            + parsedFilename.getAbsolutePath() + "\n");
        } else {
            result = new ValidationResponse(true,
                    "\n\n Valid file parameter: "
                            + parsedFilename.getAbsolutePath() + "\n");
        }
        return result;
    }

    /**
     * @return This is the file to be deployed.
     */
    public File getFilename() {
        return filename;
    }

    /**
     * @param filename This is the file to be deployed.
     */
    public void setFilename(File filename) {
        this.filename = filename;
    }

    /**
     * @return This is used to determine the package name of the application to be deployed.
     * If not specified, the package name of the application to be deployed is determined from the
     * application manifest.
     */
    public String getPackagename() {
        return packagename;
    }

    /**
     * @param packagename This is used to determine the package name of the application to be deployed.
     *                    If not specified, the package name of the application to be deployed is determined from the
     *                    application manifest.
     */
    public void setPackagename(String packagename) {
        this.packagename = packagename;
    }
}
