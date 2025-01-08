/*
 * Copyright (C) 2009 Jayway AB
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
package com.github.cardforge;

import com.github.cardforge.maven.plugins.android.AndroidSdk;
import org.junit.Assert;

import java.io.File;

/**
 * @author hugo.josefson@jayway.com
 */
public class SdkTestSupport {
    private final String envAndroidHome = System.getenv("ANDROID_HOME");

    private final AndroidSdk sdkWithPlatformDefault;

    public SdkTestSupport() {
        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_HOME set to a valid Android SDK 25 directory.", envAndroidHome);

        sdkWithPlatformDefault = new AndroidSdk(new File(envAndroidHome), "25", "30" );
    }

    public String getenvAndroidHome() {
        return envAndroidHome;
    }

    public AndroidSdk getSdkWithPlatformDefault() {
        return sdkWithPlatformDefault;
    }

    /**
     * Dynamically locate Maven home directory using environment variables.
     */
    public static File findMavenHome() {
        // First, check if M2_HOME environment variable is set
        String m2Home = System.getenv("M2_HOME");
        if (m2Home != null && !m2Home.isEmpty()) {
            return new File(m2Home);
        }

        // Second, try to locate Maven using the PATH environment variable
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            for (String path : paths) {
                File mavenBinary = new File(path, "mvn");
                if (mavenBinary.exists() && mavenBinary.canExecute()) {
                    // Assume Maven home is one level up from the binary's directory
                    return mavenBinary.getParentFile().getParentFile();
                }
            }
        }

        // Throw exception if Maven cannot be found
        throw new IllegalStateException("Maven home could not be determined dynamically");
    }
}
