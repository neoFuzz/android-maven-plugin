package com.github.cardforge.maven.plugins.android.configuration;


/**
 * Configuration for the emma test execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.github.cardforge.maven.plugins.android.phase04processclasses.EmmaMojo} and used there.
 *
 * @author Mariusz Saramak mariusz@saramak.eu
 */
public class Emma {

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase04processclasses.EmmaMojo#emmaEnable}
     */
    private Boolean enable;

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase04processclasses.EmmaMojo#emmaClassFolders}
     */
    private String classFolders;

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase04processclasses.EmmaMojo#emmaOutputMetaFile}
     */
    private String outputMetaFile;

    /**
     * Mirror of {@link com.github.cardforge.maven.plugins.android.phase04processclasses.EmmaMojo#emmaFilters}
     */
    private String filters;

    /**
     * @return the filters
     */
    public String getFilters() {
        return filters;
    }

    /**
     * @return whether emma is enabled
     */
    public Boolean isEnable() {
        return enable;
    }

    /**
     * @return the class folders.
     */
    public String getClassFolders() {
        return classFolders;
    }

    /**
     * @return the outputMetaFile path.
     */
    public String getOutputMetaFile() {
        return outputMetaFile;
    }
}
