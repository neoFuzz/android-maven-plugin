package com.github.cardforge.phase09package;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.neofuzz.phase09package.ApkMojo;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for ApkMojo using JUnit 5
 */
public class ApkMojoTest17 extends AbstractAndroidMojoTestCase<ApkMojo> {

    @Override
    public String getPluginGoalName() {
        return "apk";
    }

    @Override
    protected Class<ApkMojo> getMojoClass() {
        return ApkMojo.class;
    }

    @Test
    public void testConfigHelper() throws Exception {
        // Create and configure the mojo
        ApkMojo mojo = createMojo("manifest-tests/application-changes");

        // Verify the mojo
        assertNotNull(mojo, "Could not create the apk mojo");

        // Execute the mojo
        mojo.execute();

        // Add assertions to verify the execution result
        // For example, check if the APK file was created
        File apkFile = new File(getProjectDir(mojo), "target/your-app.apk");
        assertTrue(apkFile.exists(), "APK file was not created");
    }

    /**
     * Override to provide specialized configuration for the ApkMojo
     */
    @Override
    protected void configureFromPom(ApkMojo mojo, File projectDir) throws Exception {
        super.configureFromPom(mojo, projectDir);

        // Set required properties for APK generation
        // These would normally come from plugin configuration in pom.xml
        File androidManifest = new File(projectDir, "AndroidManifest.xml");
        if (androidManifest.exists()) {
            setField(mojo, "androidManifestFile", androidManifest);
        }

        File resourceDir = new File(projectDir, "res");
        if (resourceDir.exists()) {
            setField(mojo, "resourceDirectory", resourceDir);
        }

        File assetsDir = new File(projectDir, "assets");
        if (assetsDir.exists()) {
            setField(mojo, "assetsDirectory", assetsDir);
        }

        // Set output APK file location
        setField(mojo, "outputApk", new File(projectDir, "target/your-app.apk").toURI().toString());

        // Set build final setting to true so an APK is generated
        setField(mojo, "finalName", "your-app");
        setField(mojo, "release", true);
    }
}
