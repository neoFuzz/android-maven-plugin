package com.github.cardforge.phase09package;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.neofuzz.config.ConfigHandler;
import com.github.neofuzz.phase09package.ApkMojo;
import org.junit.*;
import org.junit.jupiter.api.Disabled;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Disabled("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
@RunWith(Parameterized.class)
public class ApkMojoTest extends AbstractAndroidMojoTestCase<ApkMojo> {

    private final String projectName;
    private final String[] expected;

    public ApkMojoTest(String projectName, String[] expected) {
        this.projectName = projectName;
        this.expected = expected;
    }

    @Parameters
    @Nonnull
    public static List<Object[]> suite() {
        final List<Object[]> suite = new ArrayList<>();

        suite.add(new Object[]{"apk-config-project1", null});
        suite.add(new Object[]{"apk-config-project2", new String[]{"persistence.xml"}});
        suite.add(new Object[]{"apk-config-project3", new String[]{"services/**", "persistence.xml"}});

        return suite;
    }

    @Override
    public String getPluginGoalName() {
        return "apk";
    }

    @Override
    protected Class<ApkMojo> getMojoClass() {
        return ApkMojo.class;
    }

    @Override
    @Before
    public void setUp()
            throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() {
        // not sure
    }

    @Test
    public void testConfigHelper()
            throws Exception {
        final ApkMojo mojo = createMojo(this.projectName);

        final ConfigHandler cfh = new ConfigHandler(mojo, this.session, this.execution);

        cfh.parseConfiguration();

        String[] includes;

        try {
            includes = getFieldValue(getFieldValue(mojo, "apk"), "metaIncludes");
        } catch (NullPointerException e) {
            // the first test has something in pluginMetaInf but not metaIncludes.
            // Therefore, null is the expected result.
            includes = null;
        }

        Assert.assertArrayEquals(this.expected, includes);
    }

    protected <T> T getFieldValue(Object object, String fieldName) throws Exception {
        return (T) super.getVariableValueFromObject(object, fieldName);
    }

}
