package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.AbstractInstrumentationMojo;

import java.util.List;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link AbstractInstrumentationMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Test {
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testPackages
     */
    protected List<String> packages;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testClasses
     */
    protected List<String> classes;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testSkip
     */
    private String skip;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testInstrumentationPackage
     */
    private String instrumentationPackage;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testInstrumentationRunner
     */
    private String instrumentationRunner;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testDebug
     */
    private Boolean debug;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testCoverage
     */
    private Boolean coverage;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testCoverageFile
     */
    private String coverageFile;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testLogOnly
     */
    private Boolean logOnly;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testTestSize
     */
    private String testSize;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testCreateReport
     */
    private Boolean createReport;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testAnnotations
     */
    private List<String> annotations;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testExcludeAnnotations
     */
    private List<String> excludeAnnotations;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testInstrumentationArgs
     */
    private List<String> instrumentationArgs;
    /**
     * Mirror of {@link AbstractInstrumentationMojo}#testFailSafe
     */
    private Boolean failSafe;


    /**
     * @return the skip
     */
    public String getSkip() {
        return skip;
    }

    /**
     * @return the instrumentationPackage
     */
    public String getInstrumentationPackage() {
        return instrumentationPackage;
    }

    /**
     * @return the instrumentationRunner
     */
    public String getInstrumentationRunner() {
        return instrumentationRunner;
    }

    /**
     * @return the debug
     */
    public Boolean isDebug() {
        return debug;
    }

    /**
     * @return the coverage
     */
    public Boolean isCoverage() {
        return coverage;
    }

    /**
     * @return the coverageFile
     */
    public String getCoverageFile() {
        return coverageFile;
    }

    /**
     * @return the logOnly
     */
    public Boolean isLogOnly() {
        return logOnly;
    }

    /**
     * @return the testSize
     */
    public String getTestSize() {
        return testSize;
    }

    /**
     * @return the createReport
     */
    public Boolean isCreateReport() {
        return createReport;
    }

    /**
     * @return the packages
     */
    public List<String> getPackages() {
        return packages;
    }

    /**
     * @return the classes
     */
    public List<String> getClasses() {
        return classes;
    }

    /**
     * @return the annotations
     */
    public List<String> getAnnotations() {
        return annotations;
    }

    /**
     * @return the excludeAnnotations
     */
    public List<String> getExcludeAnnotations() {
        return excludeAnnotations;
    }

    /**
     * @return the instrumentationArgs
     */
    public List<String> getInstrumentationArgs() {
        return instrumentationArgs;
    }

    /**
     * @return the failSafe
     */
    public Boolean isFailSafe() {
        return failSafe;
    }
}
