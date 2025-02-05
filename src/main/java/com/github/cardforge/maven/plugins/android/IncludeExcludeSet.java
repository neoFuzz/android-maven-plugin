package com.github.cardforge.maven.plugins.android;

import java.util.Set;

/**
 * A simple class to hold the includes and excludes sets
 */
public class IncludeExcludeSet {

    /**
     * A set to hold all the includes
     */
    private Set<String> includes;
    /**
     * A set to hold all the excludes
     */
    private Set<String> excludes;

    /**
     * @return the includes Set
     */
    public Set<String> getIncludes() {
        return includes;
    }

    /**
     * @return the excludes Set
     */
    public Set<String> getExcludes() {
        return excludes;
    }

}
