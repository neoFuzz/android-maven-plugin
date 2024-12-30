/*
 * Copyright (C) 2014 simpligility technologies inc.
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
package com.github.cardforge.standalonemojos;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.github.cardforge.SdkTestSupport.findMavenHome;

public class AarMojoIntegrationTest {

    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime mavenRuntime;

    public AarMojoIntegrationTest() throws Exception {
        // Locate Maven Home
        File mavenHome = findMavenHome();
        if (!mavenHome.exists()) {
            throw new IllegalStateException("Maven home not found. Check M2_HOME environment variable.");
        }

        // Pass the temp file to MavenRuntime builder
        this.mavenRuntime = MavenRuntime.builder(mavenHome, null).build();
    }

    @Test
    public void buildDeployAndRun() throws Exception {
        File basedir = resources.getBasedir("aar-no-resources");
        MavenExecutionResult result = mavenRuntime
                .forProject(basedir)
                .execute("clean", "install");
        result.assertErrorFreeLog();

        // Check contents of AAR and confirm that /res folder and R.txt exist.
        final File targetFolder = new File(basedir, "target");
        final ZipFile aarFile = new ZipFile(new File(targetFolder, "aar-no-resources.aar"));
        try {
            final Enumeration<? extends ZipEntry> entries = aarFile.entries();
            final Map<String, ? extends ZipEntry> entriesMap = convertEntriesToNamedMap(entries);
            Assert.assertTrue("AAR must have res folder", entriesMap.containsKey("res/"));
            Assert.assertTrue("AAR must have R.txt", entriesMap.containsKey("R.txt"));
        } finally {
            aarFile.close();
        }

    }

    private Map<String, ? extends ZipEntry> convertEntriesToNamedMap(Enumeration<? extends ZipEntry> entries) {
        final Map<String, ZipEntry> map = new HashMap<>();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            map.put(entry.getName(), entry);
        }
        return map;
    }
}