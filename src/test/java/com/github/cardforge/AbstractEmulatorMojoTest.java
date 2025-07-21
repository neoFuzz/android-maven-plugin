package com.github.cardforge;

import com.android.ddmlib.*;
import com.github.neofuzz.AbstractEmulatorMojo;
import com.github.neofuzz.AndroidSdk;
import com.github.neofuzz.CommandExecutor;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@Disabled("Does not work anymore with new sdk")
class AbstractEmulatorMojoTest {
    private static final String AVD_NAME = "emulator";
    private static final long DEFAULT_TIMEOUT = 500;
    private AbstractEmulatorMojoToTest abstractEmulatorMojo;

    @Mock
    private CommandExecutor mockExecutor;

    @Mock
    private AndroidDebugBridge mockAndroidDebugBridge;

    @Mock
    private ArtifactResolver mockArtifactResolver;

    @Mock
    private ArtifactHandler mockArtifactHandler;

    @Mock
    private MavenProjectHelper mockProjectHelper;

    @Mock
    private DependencyGraphBuilder mockDependencyGraphBuilder;


    @BeforeEach
    void setUp() throws Exception {
        openMocks(this);
        doNothing().when(mockExecutor).executeCommand(any(String.class), isNull());

        // Mock the CommandExecutor.Factory using Mockito's mockStatic
        try (var mockedStatic = mockStatic(CommandExecutor.Factory.class)) {
            mockedStatic.when(CommandExecutor.Factory::createDefaultCommandExecutor)
                    .thenReturn(mockExecutor);
        }

        abstractEmulatorMojo = new AbstractEmulatorMojoToTest(
                mockArtifactResolver,
                mockArtifactHandler,
                mockProjectHelper,
                mockDependencyGraphBuilder
        );
    }

    @Test
    @Disabled("Not working")
    void testStartAndroidEmulatorWithTimeoutToConnect() {
        boolean onlineAtSecondTry = false;
        int extraBootStatusPollCycles = -1;//ignored
        abstractEmulatorMojo.setWait(DEFAULT_TIMEOUT);

        IDevice emulatorDevice = withEmulatorDevice(onlineAtSecondTry, extraBootStatusPollCycles);

        withConnectedDebugBridge(emulatorDevice);

        try {
            abstractEmulatorMojo.startAndroidEmulator();
            Assertions.fail();
        } catch (MojoExecutionException e) {
            verify(mockExecutor);
        }
    }

    @Test
    @Disabled("Does not work anymore with new sdk")
    void testStartAndroidEmulatorAlreadyBooted() throws MojoExecutionException {
        boolean onlineAtSecondTry = true;
        int extraBootStatusPollCycles = 0;
        abstractEmulatorMojo.setWait(DEFAULT_TIMEOUT);

        IDevice emulatorDevice = withEmulatorDevice(onlineAtSecondTry, extraBootStatusPollCycles);
        withConnectedDebugBridge(emulatorDevice);

        abstractEmulatorMojo.startAndroidEmulator();

        verify(mockExecutor);
    }

    @Test
    @Disabled("Does not work anymore with new sdk")
    void testStartAndroidEmulatorWithOngoingBoot() throws MojoExecutionException {
        boolean onlineAtSecondTry = true;
        int extraBootStatusPollCycles = 1;
        abstractEmulatorMojo.setWait(extraBootStatusPollCycles * 5000 + 500);

        IDevice emulatorDevice = withEmulatorDevice(onlineAtSecondTry, extraBootStatusPollCycles);
        withConnectedDebugBridge(emulatorDevice);

        abstractEmulatorMojo.startAndroidEmulator();

        verify(mockExecutor);
    }

    @Test
    void testStartAndroidEmulatorWithBootTimeout() {
        boolean onlineAtSecondTry = true;
        int extraBootStatusPollCycles = -1;
        abstractEmulatorMojo.setWait(DEFAULT_TIMEOUT);

        IDevice emulatorDevice = withEmulatorDevice(onlineAtSecondTry, extraBootStatusPollCycles);
        withConnectedDebugBridge(emulatorDevice);

        try {
            abstractEmulatorMojo.startAndroidEmulator();
            Assertions.fail();
        } catch (MojoExecutionException e) {
            verify(mockExecutor).getResult();
        }
    }

    /**
     * @param onlineAtSecondTry         <code>true</code> to simulate emulator being online after first try
     * @param extraBootStatusPollCycles < 0 to simulate 'stuck in boot animation'
     * @return mocked emulator device
     */
    @Nonnull
    private IDevice withEmulatorDevice(boolean onlineAtSecondTry, int extraBootStatusPollCycles) {
        IDevice emulatorDevice = mock(IDevice.class);
        when(emulatorDevice.getAvdName()).thenReturn(AVD_NAME);
        when(emulatorDevice.isEmulator()).thenReturn(true);

        try {
            if (onlineAtSecondTry) {
                when(emulatorDevice.isOnline())
                        .thenReturn(false)
                        .thenReturn(true);

                if (extraBootStatusPollCycles < 0) {
                    // Simulate 'stuck in boot animation'
                    when(emulatorDevice.getPropertySync("dev.bootcomplete")).thenReturn(null);
                    when(emulatorDevice.getPropertySync("sys.boot_completed")).thenReturn(null);
                    when(emulatorDevice.getPropertySync("init.svc.bootanim"))
                            .thenReturn(null)
                            .thenReturn("running");
                } else if (extraBootStatusPollCycles == 0) {
                    // Simulate 'already booted'
                    when(emulatorDevice.getPropertySync("dev.bootcomplete")).thenReturn("1");
                    when(emulatorDevice.getPropertySync("sys.boot_completed")).thenReturn("1");
                    when(emulatorDevice.getPropertySync("init.svc.bootanim")).thenReturn("stopped");
                } else if (extraBootStatusPollCycles == 1) {
                    // Simulate 'almost booted (1 extra poll)'
                    when(emulatorDevice.getPropertySync("dev.bootcomplete"))
                            .thenReturn(null)
                            .thenReturn("1");
                    when(emulatorDevice.getPropertySync("sys.boot_completed"))
                            .thenReturn(null)
                            .thenReturn("1");
                    when(emulatorDevice.getPropertySync("init.svc.bootanim"))
                            .thenReturn("running")
                            .thenReturn("stopped");
                } else if (extraBootStatusPollCycles >= 3) {
                    // Simulate 'almost booted (>=3 extra polls)'
                    when(emulatorDevice.getPropertySync("dev.bootcomplete"))
                            .thenReturn(null, new String[extraBootStatusPollCycles - 2])
                            .thenReturn("1");
                    when(emulatorDevice.getPropertySync("sys.boot_completed"))
                            .thenReturn(null, new String[extraBootStatusPollCycles - 2])
                            .thenReturn("1");
                    when(emulatorDevice.getPropertySync("init.svc.bootanim"))
                            .thenReturn(null, new String[extraBootStatusPollCycles / 2 - 1])
                            .thenReturn("running", new String[extraBootStatusPollCycles / 2 + extraBootStatusPollCycles % 2 - 1])
                            .thenReturn("stopped");
                } else if (extraBootStatusPollCycles >= 2) {
                    // Simulate 'almost booted (>=2 extra polls)'
                    when(emulatorDevice.getPropertySync("dev.bootcomplete"))
                            .thenReturn(null, new String[extraBootStatusPollCycles - 2])
                            .thenReturn("1");
                    when(emulatorDevice.getPropertySync("sys.boot_completed"))
                            .thenReturn(null, new String[extraBootStatusPollCycles - 2])
                            .thenReturn("1");
                    when(emulatorDevice.getPropertySync("init.svc.bootanim"))
                            .thenReturn("running", new String[extraBootStatusPollCycles - 2])
                            .thenReturn("stopped");
                }
            } else {
                when(emulatorDevice.isOnline()).thenReturn(false);
            }
        } catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException |
                 IOException e) {
            throw new RuntimeException("Unexpected checked exception during mock setup", e);
        }

        return emulatorDevice;
    }

    private void withConnectedDebugBridge(IDevice emulatorDevice) {
        when(mockAndroidDebugBridge.isConnected()).thenReturn(true);
        when(mockAndroidDebugBridge.hasInitialDeviceList()).thenReturn(true);
        when(mockAndroidDebugBridge.getDevices())
                .thenReturn(new IDevice[0])
                .thenReturn(new IDevice[]{emulatorDevice});
    }

    private class AbstractEmulatorMojoToTest extends AbstractEmulatorMojo {
        private long wait = DEFAULT_TIMEOUT;

        @Inject
        protected AbstractEmulatorMojoToTest(ArtifactResolver artifactResolver, ArtifactHandler artHandler, MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
            super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
        }

        public long getWait() {
            return wait;
        }

        public void setWait(long wait) {
            this.wait = wait;
        }

        @Override
        public AndroidSdk getAndroidSdk() {
            return new SdkTestSupport().getSdkWithPlatformDefault();
        }

        @Override
        public void execute() {
            // empty by design
        }

        @Override
        public AndroidDebugBridge initAndroidDebugBridge() {
            return mockAndroidDebugBridge;
        }

        String determineAvd() {
            return AVD_NAME;
        }

        @Nonnull
        String determineWait() {
            return String.valueOf(wait);
        }
    }

}
