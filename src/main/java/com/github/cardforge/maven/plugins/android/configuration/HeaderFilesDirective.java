package com.github.cardforge.maven.plugins.android.configuration;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Johan Lindquist
 */
public class HeaderFilesDirective {

    /**
     * Base directory from where to include/exclude files from.
     */
    private String directory;

    /**
     * A list of &lt;include> elements specifying the files (usually C/C++ header files) that should be included in the
     * header archive. When not specified, the default includes will be <code><br>
     * &lt;includes><br>
     * &nbsp;&lt;include>**&#47;*.h&lt;/include><br>
     * &lt;/includes><br>
     * </code>
     */
    @Parameter
    private String[] includes;

    /**
     * A list of &lt;include> elements specifying the files (usually C/C++ header files) that should be excluded from
     * the header archive.
     */
    @Parameter
    private String[] excludes;

    /**
     * @return Base directory from where to include/exclude files from.
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @param directory Base directory from where to include/exclude files from.
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * @return An array with a list of files to exclude
     */
    public String[] getExcludes() {
        return excludes;
    }

    /**
     * @param excludes An array with a list of files to exclude
     */
    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

    /**
     * @return An array with a list of files to include
     */
    public String[] getIncludes() {
        return includes;
    }

    /**
     * @param includes An array with a list of files to include
     */
    public void setIncludes(String[] includes) {
        this.includes = includes;
    }
}
