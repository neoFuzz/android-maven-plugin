package com.github.cardforge.maven.plugins.android.phase01generatesources;

import com.android.annotations.NonNull;

import java.util.*;

/**
 * Looks for duplicate layout files across Android resource packages.
 */
final class ConflictingLayoutDetector {
    private final Map<String, Collection<String>> map = new HashMap<>();

    /**
     * Adds layout files to the detector.
     *
     * @param packageName the package name
     * @param layoutFiles the layout files
     */
    public void addLayoutFiles(String packageName, String[] layoutFiles) {
        map.put(packageName, Arrays.asList(layoutFiles));
    }

    /**
     * @return a collection of conflicting layouts
     */
    @NonNull
    public Collection<ConflictingLayout> getConflictingLayouts() {
        final Map<String, ConflictingLayout> result = new TreeMap<>();
        for (final String entryA : map.keySet()) {
            for (final String entryB : map.keySet()) {
                if (entryA.equals(entryB)) {
                    continue;
                }

                // Find any layout files that are in both packages.
                final Set<String> tmp = new HashSet<>();
                tmp.addAll(map.get(entryA));
                tmp.retainAll(map.get(entryB));

                for (final String layoutFile : tmp) {
                    if (!result.containsKey(layoutFile)) {
                        result.put(layoutFile, new ConflictingLayout(layoutFile));
                    }
                    final ConflictingLayout layout = result.get(layoutFile);
                    layout.addPackageName(entryA);
                    layout.addPackageName(entryB);
                }
            }
        }

        return result.values();
    }
}
