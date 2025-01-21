package com.github.cardforge.maven.plugins.android.phase01generatesources;

import com.android.annotations.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a layout that is duplicating among more than one Android package.
 */
final class ConflictingLayout {
    /**
     * The layout file name
     */
    private final String layoutFileName;
    /**
     * The package names that are conflicting with this layout
     */
    private final Set<String> packageNames = new TreeSet<>();

    /**
     * @param layoutFileName the layout file name
     */
    ConflictingLayout(String layoutFileName) {
        this.layoutFileName = layoutFileName;
    }

    /**
     * @return the layout file name
     */
    public String getLayoutFileName() {
        return layoutFileName;
    }

    /**
     * @param packageName the package name that is conflicting with this layout
     */
    public void addPackageName(String packageName) {
        packageNames.add(packageName);
    }

    /**
     * @return the package names that are conflicting with this layout
     */
    @NonNull
    public Set<String> getPackageNames() {
        return Collections.unmodifiableSet(packageNames);
    }
}
