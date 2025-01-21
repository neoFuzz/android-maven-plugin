package com.github.cardforge.maven.plugins.android.configuration;

import org.codehaus.plexus.util.SelectorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * POJO to specify META-INF include and exclude patterns.
 *
 * @author Pappy STĂNESCU - pappy.stanescu@gmail.com
 */
public class MetaInf {

    private List<String> includes;

    private List<String> excludes;

    /**
     * @param obj The reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj argument;
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

        final MetaInf that = (MetaInf) obj;

        if (this.includes == null) {
            if (that.includes != null) {
                return false;
            }
        } else if (!this.includes.equals(that.includes)) {
            return false;
        }

        if (this.excludes == null) {
            return that.excludes == null;
        } else return this.excludes.equals(that.excludes);
    }

    /**
     * @param excludes An array with a list of files to exclude
     * @return <code>MetaInf</code>
     */
    public MetaInf exclude(String... excludes) {
        getExcludes().addAll(Arrays.asList(excludes));

        return this;
    }

    /**
     * @return An array with a list of files to exclude
     */
    public List<String> getExcludes() {
        if (this.excludes == null) {
            this.excludes = new ArrayList<>();
        }

        return this.excludes;
    }

    /**
     * @return An array with a list of files to include
     */
    public List<String> getIncludes() {
        if (this.includes == null) {
            this.includes = new ArrayList<>();
        }

        return this.includes;
    }

    /**
     * @return <code>int</code> with the hash code of this object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.excludes == null) ? 0 : this.excludes.hashCode());
        result = (prime * result) + ((this.includes == null) ? 0 : this.includes.hashCode());
        return result;
    }

    /**
     * @param includes An array with a list of files to include
     * @return <code>MetaInf</code>
     */
    public MetaInf include(String... includes) {
        getIncludes().addAll(Arrays.asList(includes));

        return this;
    }

    /**
     * @param name The name of the file to check
     * @return <code>true</code> if the file is included or excluded
     */
    public boolean isIncluded(String name) {
        boolean included = this.includes == null;

        if (this.includes != null) {
            for (final String x : this.includes) {
                if (SelectorUtils.matchPath("META-INF/" + x, name)) {
                    included = true;

                    break;
                }
            }
        }

        if (included && (this.excludes != null)) {
            for (final String x : this.excludes) {
                if (SelectorUtils.matchPath("META-INF/" + x, name)) {
                    included = false;

                    break;
                }
            }
        }

        return included;
    }
}
