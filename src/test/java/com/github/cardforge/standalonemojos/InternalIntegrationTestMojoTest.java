/*
 * Copyright (C) 2012 Jayway AB
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

import com.android.ddmlib.IDevice;
import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.DeviceCallback;
import com.github.cardforge.maven.plugins.android.phase12integrationtest.InternalIntegrationTestMojo;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


/**
 * Tests the {@link InternalIntegrationTestMojo} mojo, as far as possible without actually
 * connecting and communicating with a device.
 *
 * @author Erik Ogenvik
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
public class InternalIntegrationTestMojoTest extends AbstractAndroidMojoTestCase<InternalIntegrationTestMojo> {

    @Override
    public String getPluginGoalName() {
        return "internal-integration-test";
    }

    @Override
    protected Class<InternalIntegrationTestMojo> getMojoClass() {
        return null;
    }

    @Test
    public void testTestProject() throws Exception {

        // Mock the AbstractAndroidMojo to intercept the doWithDevices method
        AbstractAndroidMojo mockedMojo = mock(AbstractAndroidMojo.class);

        // Stub the doWithDevices method
        doAnswer(invocation -> {
            DeviceCallback callback = invocation.getArgument(0);
            callback.doWithDevice(createMockDevice());
            return null;
        }).when(mockedMojo).doWithDevices(any(DeviceCallback.class));

        InternalIntegrationTestMojo mojo = createMojo("manifest-tests/test-project");

        mojo.execute();

        // Verify parsed classes
        List<String> classes = mojo.getParsedClasses(); // Assuming getParsedClasses is exposed
        assertNotNull(classes);
        assertEquals(1, classes.size());
    }

    @Nonnull
    private IDevice createMockDevice() {
        IDevice device = mock(IDevice.class);

        when(device.getSerialNumber()).thenReturn(null);
        when(device.getAvdName()).thenReturn(null);
        when(device.getState()).thenReturn(null);
        when(device.getProperties()).thenReturn(null);
        when(device.getPropertyCount()).thenReturn(0);
        when(device.getProperty(anyString())).thenReturn(null);
        when(device.isOnline()).thenReturn(false);
        when(device.isEmulator()).thenReturn(false);
        when(device.isOffline()).thenReturn(false);
        when(device.isBootLoader()).thenReturn(false);

        return device;
    }
}
