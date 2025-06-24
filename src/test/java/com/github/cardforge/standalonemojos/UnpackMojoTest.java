package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.configuration.MetaInf;
import com.github.cardforge.maven.plugins.android.standalonemojos.UnpackMojo;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UnpackMojoTest
        extends AbstractAndroidMojoTestCase<UnpackMojo> {

    private final String projectName;
    private final MetaInf expected;

    public UnpackMojoTest(String projectName, MetaInf expected) {
        this.projectName = projectName;
        this.expected = expected;
    }

    @Parameters
    @Nonnull
    static public List<Object[]> suite() {
        final List<Object[]> suite = new ArrayList<Object[]>();

        suite.add(new Object[]{"unpack-config-project1", null});
        suite.add(new Object[]{"unpack-config-project2", new MetaInf().include("persistence.xml")});
        suite.add(new Object[]{"unpack-config-project3", new MetaInf().include("services/**", "persistence.xml")});
        suite.add(new Object[]{"unpack-config-project4", new MetaInf()});

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
        //unsure
    }

    @Test
    public void testConfigHelper()
            throws Exception {
        final UnpackMojo mojo = createMojo(this.projectName);
        final ConfigHandler cfh = new ConfigHandler(mojo, this.session, this.execution);

        cfh.parseConfiguration();

        MetaInf result = getFieldValue(mojo, "unpackMetaInf");

        Assert.assertEquals(this.expected, result);

        Assert.assertEquals(result == null, getFieldValue(mojo, "unpack") == null);
    }

    protected <T> T getFieldValue(Object object, String fieldName)
            throws Exception {
        return (T) super.getVariableValueFromObject(object, fieldName);
    }

}
