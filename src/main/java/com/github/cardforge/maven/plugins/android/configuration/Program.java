package com.github.cardforge.maven.plugins.android.configuration;

/**
 * Configuration element inside MonkeyRunner configuration. It represents both filename and options for a given
 * monkeyrunner program execution.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
public class Program {
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#filename}
     */
    private String filename;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#options}
     */
    private String options;

    /**
     * Constructor used for parsing.
     */
    public Program() {
        // do not remove, used by parsing.
    }

    /**
     * Constructor used for testing.
     *
     * @param filename The file name
     * @param options The options to use
     */
    public Program(String filename, String options) {
        this.filename = filename;
        this.options = options;
    }

    /**
     * @return the filename of the program to execute.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the options to use when executing the program.
     */
    public String getOptions() {
        return options;
    }

    // ----------------------------------
    // TESTING METHODS
    // ----------------------------------
    /**
     * @return The hash code of the object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (filename == null ? 0 : filename.hashCode());
        result = prime * result + (options == null ? 0 : options.hashCode());
        return result;
    }

    /**
     * @param obj The object to compare with
     * @return <code>true</code> if the objects are equal, <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Program other = (Program) obj;
        if (filename == null) {
            if (other.filename != null) {
                return false;
            }
        } else if (!filename.equals(other.filename)) {
            return false;
        }
        if (options == null) {
            return other.options == null;
        } else return options.equals(other.options);
    }

}
