package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.standalonemojos.UnpackMojo;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pappy STĂNESCU - pappy.stanescu@gmail.com
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
@RunWith(Parameterized.class)
public class UnpackMojoLazyTest
        extends AbstractAndroidMojoTestCase<UnpackMojo> {

    private final String projectName;

    public UnpackMojoLazyTest(String projectName) {
        this.projectName = projectName;
    }

    @Parameters
    @Nonnull
    static public List<Object[]> suite() {
        final List<Object[]> suite = new ArrayList<Object[]>();

        suite.add(new Object[]{"unpack-config-lazy"});
        suite.add(new Object[]{"unpack-config-lazy-deprecated"});

        return suite;
    }

    @Override
    public String getPluginGoalName() {
        return "unpack";
    }

    @Override
    protected Class<UnpackMojo> getMojoClass() {
        return UnpackMojo.class;
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

    public String getName() {
        return projectName;
    }

    @Test
    public void testConfigHelper()
            throws Exception {
        final UnpackMojo mojo = createMojo(this.projectName);
        final ConfigHandler cfh = new ConfigHandler(mojo, this.session, this.execution);

        cfh.parseConfiguration();

        Boolean result = getFieldValue(mojo, "unpackLazy");

        Assert.assertNotNull(result);
        Assert.assertTrue(result);
    }

    protected <T> T getFieldValue(Object object, String fieldName) throws Exception {
        return (T) super.getVariableValueFromObject(object, fieldName);
    }

}
