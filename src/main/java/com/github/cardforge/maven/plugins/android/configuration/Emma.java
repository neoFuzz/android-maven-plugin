package com.github.cardforge.maven.plugins.android.configuration;


/**
 * Configuration for the emma test execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link EmmaMojo} and used there.
 *
 * @author Mariusz Saramak mariusz@saramak.eu
 */
public class Emma {

    /**
     * Mirror of {@link EmmaMojo#emmaEnable}
     */
    private Boolean enable;

    /**
     * Mirror of {@link EmmaMojo#emmaClassFolders}
     */
    private String classFolders;

    /**
     * Mirror of {@link EmmaMojo#emmaOutputMetaFile}
     */
    private String outputMetaFile;

    /**
     * Mirror of {@link EmmaMojo#emmaFilters}
     */
    private String filters;

    public String getFilters() {
        return filters;
    }

    public Boolean isEnable() {
        return enable;
    }

    public String getClassFolders() {
        return classFolders;
    }

    public String getOutputMetaFile() {
        return outputMetaFile;
    }
}
