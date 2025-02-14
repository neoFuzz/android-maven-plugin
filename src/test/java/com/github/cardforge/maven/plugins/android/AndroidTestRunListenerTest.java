package com.github.cardforge.maven.plugins.android;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AndroidTestRunListenerTest {

    private final String suffix = RandomStringUtils.randomAlphabetic(10);
    private final String serial = RandomStringUtils.randomAlphabetic(10);
    private final String avd = RandomStringUtils.randomAlphabetic(10);
    private final String manufacturer = RandomStringUtils.randomAlphabetic(10);
    private final String model = RandomStringUtils.randomAlphabetic(10);
    private final String runName = RandomStringUtils.randomAlphabetic(10);
    private final String key = RandomStringUtils.randomAlphabetic(10);
    private final String value = RandomStringUtils.randomAlphabetic(10);
    private final int count = RandomUtils.nextInt(1, 10);
    private final int elapsed = RandomUtils.nextInt(10, 1000);

    @Rule
    public TemporaryFolder target = new TemporaryFolder();
    @Mock
    private IDevice device;

    @BeforeAll
    public void setUp() {
        openMocks(this);
        when(device.getSerialNumber()).thenReturn(serial);
        when(device.getAvdName()).thenReturn(avd);
        when(device.getProperty("ro.product.manufacturer")).thenReturn(manufacturer);
        when(device.getProperty("ro.product.model")).thenReturn(model);
        when(device.getProperties()).thenReturn(Collections.singletonMap(key, value));
    }

    @Nonnull
    private String randomTrace() {
        return RandomStringUtils.randomAlphabetic(20) + ":" + RandomStringUtils.randomAlphabetic(20);
    }

    @Test
    public void validReport() throws IOException {
        target.create();
        // Instantiate the listener using the pre-configured mock device
        final ITestRunListener listener = new AndroidTestRunListener(
                device, new SystemStreamLog(), true, false,
                null, suffix, target.getRoot());
        listener.testRunStarted(runName, count);

        // Generate a random number of tests
        final int tests = RandomUtils.nextInt(5, 10);
        for (int i = 0; i < tests; i++) {
            // Generate random test identifiers
            final TestIdentifier id = new TestIdentifier(
                    RandomStringUtils.randomAlphabetic(20), RandomStringUtils.randomAlphabetic(10));
            listener.testStarted(id);

            // Randomly simulate different outcomes for the test
            switch (RandomUtils.nextInt(0, 4)) {
                case 0:
                    listener.testFailed(id, randomTrace());
                    break;
                case 1:
                    listener.testAssumptionFailure(id, randomTrace());
                    break;
                case 2:
                    listener.testIgnored(id);
                    break;
                default:
                    // Default case intentionally left empty
            }
            listener.testEnded(id, Collections.<String, String>emptyMap());
        }

        // Randomly simulate a test run failure
        if (RandomUtils.nextInt(0, 1) == 1) {
            listener.testRunFailed(RandomStringUtils.randomAlphabetic(20));
        }
        listener.testRunEnded(elapsed, Collections.<String, String>emptyMap());

        // Verify the device methods were called as expected
        verify(device, times(2)).getSerialNumber();
        verify(device, times(4)).getAvdName();
        verify(device, times(2)).getProperty("ro.product.manufacturer");
        verify(device, times(2)).getProperty("ro.product.model");
        verify(device).getProperties();

        Assertions.assertEquals(1, target.getRoot().listFiles().length);
        Assertions.assertEquals(1, target.getRoot().listFiles()[0].listFiles().length);
        for (File file : target.getRoot().listFiles()[0].listFiles()) {
            Assertions.assertTrue(validateXMLSchema("surefire/surefire-test-report.xsd", file));
        }
    }

    public boolean validateXMLSchema(String xsdResource, File xmlFile) {
        try {
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = factory.newSchema(new StreamSource(getClass().getClassLoader().getResourceAsStream(xsdResource)));
            final Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlFile));
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}