/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.res2;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceUrl;
import com.android.ide.common.resources.configuration.Configurable;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.ResourceType;
import com.google.common.base.Splitter;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*;

import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.ide.common.resources.ResourceResolver.MAX_RESOURCE_INDIRECTION;

/**
 * Base class for {@link ResourceItem} containers.
 */
public abstract class AbstractResourceRepository {

    /**
     * Lock used to protect map access
     */
    protected static final Object ITEM_MAP_LOCK = new Object();
    /**
     * Flag for the framework
     */
    private final boolean mFramework;

    /**
     * Constructs a new {@link AbstractResourceRepository} with the given {@link ResourceType}.
     * <p>
     * This constructor is used to create a new {@link AbstractResourceRepository} with the given
     * {@link ResourceType}.
     * </p>
     *
     * @param isFramework if true, this repository is a framework repository.
     */
    protected AbstractResourceRepository(boolean isFramework) {
        mFramework = isFramework;
    }

    /**
     * Returns true if this repository is a framework repository.
     * <p>
     * This is used to determine if a resource is a framework resource or not.
     * </p>
     *
     * @return true if this repository is a framework repository.
     */
    public boolean isFramework() {
        return mFramework;
    }

    /**
     * Creates a {@link MergeConsumer} that can be used to merge resources into this repository.
     * <p>
     * The returned {@link MergeConsumer} can be used to merge resources into this repository.
     * </p>
     *
     * @return a {@link MergeConsumer} that can be used to merge resources into this repository
     */
    @NonNull
    public MergeConsumer<ResourceItem> createMergeConsumer() {
        return new RepositoryMerger();
    }

    /**
     * Returns the map of all resources of the given {@link ResourceType}.
     * <p>
     * The map is a {@link Multimap} of {@link String} to {@link ResourceItem}.
     * /<p>
     * If the {@link ResourceType} is not found, this method returns null.
     *
     * @return the map of all resources of the given {@link ResourceType}.
     */
    @NonNull
    protected abstract Map<ResourceType, ListMultimap<String, ResourceItem>> getMap();

    /**
     * Returns the map of all resources of the given {@link ResourceType}.
     * <p>
     * The map is a {@link Multimap} of {@link String} to {@link ResourceItem}.
     * /<p>
     * If the {@link ResourceType} is not found, this method returns null.
     *
     * @param type   the {@link ResourceType} to get the map for.
     * @param create if true, the map will be created if it does not exist.
     * @return a new {@link ListMultimap} for the given {@link ResourceType}.
     */
    @Nullable
    protected abstract ListMultimap<String, ResourceItem> getMap(ResourceType type, boolean create);

    /**
     * Returns the map of all resources of the given {@link ResourceType}.
     * <p>
     * The map is a {@link Multimap} of {@link String} to {@link ResourceItem}.
     *
     * @param type the {@link ResourceType} to get the map for.
     * @return a new {@link ListMultimap} for the given {@link ResourceType}.
     */
    @NonNull
    protected ListMultimap<String, ResourceItem> getMap(ResourceType type) {
        //noinspection ConstantConditions
        return getMap(type, true); // Won't return null if create is false
    }

    /**
     * Returns the map of all resources.
     * <p>
     * The map is a {@link Multimap} of {@link ResourceType} to {@link ResourceItem}.
     *
     * @return the map of all resources.
     */
    @NonNull
    public Map<ResourceType, ListMultimap<String, ResourceItem>> getItems() {
        return getMap();
    }

    /**
     * Returns the list of resources of the given {@link ResourceType} and name.
     * <p>
     * The list is a {@link List} of {@link ResourceItem}.
     * </p><p>
     * If the {@link ResourceType} is not found, this method returns null.
     * </p>
     *
     * @param resourceType the type of resource to look up
     * @param resourceName the name of the resource to look up
     * @return the list of resources of the given {@link ResourceType} and name.
     */
    @Nullable
    public List<ResourceItem> getResourceItem(@NonNull ResourceType resourceType, // TODO: Rename to getResourceItemList?
                                              @NonNull String resourceName) {
        synchronized (ITEM_MAP_LOCK) {
            ListMultimap<String, ResourceItem> map = getMap(resourceType, false);

            if (map != null) {
                return map.get(resourceName);
            }
        }

        return null;
    }

    /**
     * Returns a collection of all the names of the resources of the given type.
     *
     * @param type the {@link ResourceType} to get the items for.
     * @return a collection of all the names of the resources of the given type.
     */
    @NonNull
    public Collection<String> getItemsOfType(@NonNull ResourceType type) {
        synchronized (ITEM_MAP_LOCK) {
            Multimap<String, ResourceItem> map = getMap(type, false);
            if (map == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableCollection(map.keySet());
        }
    }

    /**
     * Returns true if this resource repository contains a resource of the given
     * name.
     *
     * @param resourceType the type of resource to look up
     * @param resourceName the name of the resource
     * @return true if the resource is known
     */
    public boolean hasResourceItem(@NonNull ResourceType resourceType,
                                   @NonNull String resourceName) {
        synchronized (ITEM_MAP_LOCK) {
            ListMultimap<String, ResourceItem> map = getMap(resourceType, false);

            if (map != null) {
                List<ResourceItem> itemList = map.get(resourceName);
                return !itemList.isEmpty();
            }
        }

        return false;
    }

    /**
     * Returns whether the repository has resources of a given {@link ResourceType}.
     *
     * @param resourceType the type of resource to check.
     * @return true if the repository contains resources of the given type, false otherwise.
     */
    public boolean hasResourcesOfType(@NonNull ResourceType resourceType) {
        synchronized (ITEM_MAP_LOCK) {
            ListMultimap<String, ResourceItem> map = getMap(resourceType, false);
            return map != null && !map.isEmpty();
        }
    }

    /**
     * Returns the {@link ResourceFile} matching the given name, {@link ResourceType} and
     * configuration.
     * <p>
     * This only works with files generating one resource named after the file
     * (for instance, layouts, bitmap based drawable, xml, anims).
     *
     * @param name   the resource name
     * @param type   the folder type search for
     * @param config the folder configuration to match for
     * @return the matching file or <code>null</code> if no match was found.
     */
    @Nullable
    public ResourceFile getMatchingFile(
            @NonNull String name,
            @NonNull ResourceType type,
            @NonNull FolderConfiguration config) {
        List<ResourceFile> matchingFiles = getMatchingFiles(name, type, config);
        return matchingFiles.isEmpty() ? null : matchingFiles.get(0);
    }

    /**
     * Returns a list of {@link ResourceFile} matching the given name, {@link ResourceType} and
     * configuration. This ignores the qualifiers which are missing from the configuration.
     * <p>
     * This only works with files generating one resource named after the file (for instance,
     * layouts, bitmap based drawable, xml, anims).
     *
     * @param name   the resource name
     * @param type   the folder type search for
     * @param config the folder configuration to match for
     * @return a list of matching files. This list is never empty.
     * @see #getMatchingFile(String, ResourceType, FolderConfiguration)
     */
    @NonNull
    public List<ResourceFile> getMatchingFiles(
            @NonNull String name,
            @NonNull ResourceType type,
            @NonNull FolderConfiguration config) {
        return getMatchingFiles(name, type, config, new HashSet<>(), 0);
    }

    /**
     * Returns a list of {@link ResourceFile} matching the given name, {@link ResourceType} and
     * configuration.
     * <p>
     * This only works with files generating one resource named after the file (for instance,
     * layouts, bitmap based drawable, xml, anims).
     *
     * @param name      the resource name
     * @param type      the folder type search for
     * @param config    the folder configuration to match for
     * @param seenNames a set of names already seen. This is used to prevent infinite recursion.
     * @param depth     the current depth of the search. This is used to prevent infinite recursion
     * @return a list of matching files. This list is never empty.
     */
    @NonNull
    private List<ResourceFile> getMatchingFiles(
            @NonNull String name,
            @NonNull ResourceType type,
            @NonNull FolderConfiguration config,
            @NonNull Set<String> seenNames,
            int depth) {
        assert !seenNames.contains(name);
        if (depth >= MAX_RESOURCE_INDIRECTION) {
            return Collections.emptyList();
        }
        List<ResourceFile> output;
        synchronized (ITEM_MAP_LOCK) {
            ListMultimap<String, ResourceItem> typeItems = getMap(type, false);
            if (typeItems == null) {
                return Collections.emptyList();
            }
            seenNames.add(name);
            output = new ArrayList<>();
            List<ResourceItem> matchingItems = typeItems.get(name);
            List<Configurable> matches = config.findMatchingConfigurables(matchingItems);
            for (Configurable conf : matches) {
                ResourceItem match = (ResourceItem) conf;
                // if match is an alias, check if the name is in seen names.
                ResourceValue resourceValue = match.getResourceValue(isFramework());
                if (resourceValue != null) {
                    String value = resourceValue.getValue();
                    if (value != null && value.startsWith(PREFIX_RESOURCE_REF)) {
                        ResourceUrl url = ResourceUrl.parse(value);
                        if (url != null && url.type == type && url.framework == isFramework()) {
                            if (!seenNames.contains(url.name)) {
                                // This resource alias needs to be resolved again.
                                output.addAll(getMatchingFiles(
                                        url.name, type, config, seenNames, depth + 1));
                            }
                            continue;
                        }
                    }
                }
                output.add(match.getSource());

            }
        }

        return output;
    }

    /**
     * Returns the resources values matching a given {@link FolderConfiguration}.
     *
     * @param referenceConfig the configuration that each value must match.
     * @return a map with guaranteed to contain an entry for each {@link ResourceType}
     */
    @NonNull
    public Map<ResourceType, Map<String, ResourceValue>> getConfiguredResources(
            @NonNull FolderConfiguration referenceConfig) {
        Map<ResourceType, Map<String, ResourceValue>> map = Maps.newEnumMap(ResourceType.class);

        synchronized (ITEM_MAP_LOCK) {
            Map<ResourceType, ListMultimap<String, ResourceItem>> itemMap = getMap();
            for (ResourceType key : ResourceType.values()) {
                // get the local results and put them in the map
                map.put(key, getConfiguredResources(itemMap, key, referenceConfig));
            }
        }

        return map;
    }

    /**
     * Returns a map of (resource name, resource value) for the given {@link ResourceType}.
     * <p>The values returned are taken from the resource files best matching a given
     * {@link FolderConfiguration}.
     *
     * @param type            the type of the resources.
     * @param referenceConfig the configuration to best match.
     * @return a map of (resource name, resource value) for the given {@link ResourceType}.
     */
    @NonNull
    public Map<String, ResourceValue> getConfiguredResources(
            @NonNull ResourceType type,
            @NonNull FolderConfiguration referenceConfig) {
        return getConfiguredResources(getMap(), type, referenceConfig);
    }

    /**
     * Returns a map of (resource name, resource value) for the given {@link ResourceType}.
     * <p>The values returned are taken from the resource files best matching a given
     * {@link FolderConfiguration}.
     *
     * @param itemMap         the map of all resources.
     *                        The map is a {@link Multimap} of {@link ResourceType} to {@link ResourceItem}.
     * @param type            the type of the resources.
     * @param referenceConfig the configuration to best match.
     * @return a map of (resource name, resource value) for the given {@link ResourceType}.
     */
    @NonNull
    public Map<String, ResourceValue> getConfiguredResources(
            @NonNull Map<ResourceType, ListMultimap<String, ResourceItem>> itemMap,
            @NonNull ResourceType type,
            @NonNull FolderConfiguration referenceConfig) {
        // get the resource item for the given type
        ListMultimap<String, ResourceItem> items = itemMap.get(type);
        if (items == null) {
            return Maps.newHashMap();
        }

        Set<String> keys = items.keySet();

        // create the map
        Map<String, ResourceValue> map = Maps.newHashMapWithExpectedSize(keys.size());

        for (String key : keys) {
            List<ResourceItem> keyItems = items.get(key);

            // look for the best match for the given configuration
            // the match has to be of type ResourceFile since that's what the input list contains
            ResourceItem match = (ResourceItem) referenceConfig.findMatchingConfigurable(keyItems);
            if (match != null) {
                ResourceValue value = match.getResourceValue(mFramework);
                if (value != null) {
                    map.put(match.getName(), value);
                }
            }
        }

        return map;
    }

    /**
     * Returns the resource value matching a given {@link FolderConfiguration}.
     *
     * @param type            the resource type.
     * @param name            the resource name.
     * @param referenceConfig the configuration to best match.
     * @return the resource value for the given type and name, or null if not found.
     */
    @Nullable
    public ResourceValue getConfiguredValue(
            @NonNull ResourceType type,
            @NonNull String name,
            @NonNull FolderConfiguration referenceConfig) {
        // get the resource item for the given type
        ListMultimap<String, ResourceItem> items = getMap(type, false);
        if (items == null) {
            return null;
        }

        List<ResourceItem> keyItems = items.get(name);
        if (keyItems == null) {
            return null;
        }

        // look for the best match for the given configuration
        // the match has to be of type ResourceFile since that's what the input list contains
        ResourceItem match = (ResourceItem) referenceConfig.findMatchingConfigurable(keyItems);
        return match != null ? match.getResourceValue(mFramework) : null;
    }

    /**
     * Adds a {@link ResourceItem} to the repository.
     *
     * @param item the {@link ResourceItem} to add.
     */
    private void addItem(@NonNull ResourceItem item) {
        synchronized (ITEM_MAP_LOCK) {
            ListMultimap<String, ResourceItem> map = getMap(item.getType());
            if (!map.containsValue(item)) {
                map.put(item.getName(), item);
            }
        }
    }

    /**
     * Removes a {@link ResourceItem} from the repository.
     *
     * @param removedItem the {@link ResourceItem} to remove.
     */
    private void removeItem(@NonNull ResourceItem removedItem) {
        synchronized (ITEM_MAP_LOCK) {
            Multimap<String, ResourceItem> map = getMap(removedItem.getType(), false);
            if (map != null) {
                map.remove(removedItem.getName(), removedItem);
            }
        }
    }

    /**
     * Returns the sorted list of languages used in the resources.
     *
     * @return a sorted set of languages.
     */
    @NonNull
    public SortedSet<String> getLanguages() {
        SortedSet<String> set = new TreeSet<>();

        // As an optimization we could just look for values since that's typically where
        // the languages are defined -- not on layouts, menus, etc. -- especially if there
        // are no translations for it
        Set<String> qualifiers = Sets.newHashSet();

        synchronized (ITEM_MAP_LOCK) {
            for (ListMultimap<String, ResourceItem> map : getMap().values()) {
                for (ResourceItem item : map.values()) {
                    qualifiers.add(item.getQualifiers());
                }
            }
        }

        Splitter splitter = Splitter.on('-');
        for (String s : qualifiers) {
            for (String qualifier : splitter.split(s)) {
                if (qualifier.length() == 2 && Character.isLetter(qualifier.charAt(0))
                        && Character.isLetter(qualifier.charAt(1))) {
                    set.add(qualifier);
                }
            }
        }

        return set;
    }

    /**
     * Clears the repository.
     */
    public void clear() {
        getMap().clear();
    }

    /**
     * Class for merging repositories. This is used to merge a library project with the main project.
     */
    private class RepositoryMerger implements MergeConsumer<ResourceItem> {

        /**
         * Function does nothing.
         *
         * @param factory unused
         * @throws ConsumerException if an error occurs
         */
        @Override
        public void start(@NonNull DocumentBuilderFactory factory)
                throws ConsumerException {
            // unused
        }

        /**
         * Function does nothing.
         *
         * @throws ConsumerException if an error occurs
         */
        @Override
        public void end() throws ConsumerException {
            // unused
        }

        /**
         * Adds a new item to the repository.
         *
         * @param item the new item.
         * @throws ConsumerException if an error occurs
         */
        @Override
        public void addItem(@NonNull ResourceItem item) throws ConsumerException {
            if (item.isTouched()) {
                AbstractResourceRepository.this.addItem(item);
            }
        }

        /**
         * Removes an item from the repository.
         *
         * @param removedItem the removed item.
         * @param replacedBy  the optional item that replaces the removed item.
         * @throws ConsumerException if an error occurs
         */
        @Override
        public void removeItem(@NonNull ResourceItem removedItem, @Nullable ResourceItem replacedBy)
                throws ConsumerException {
            AbstractResourceRepository.this.removeItem(removedItem);
        }

        /**
         * Function returns false as it is going to never ignore items.
         *
         * @param item the item to be ignored for merge.
         * @return <code>false</code> never ignoring items.
         */
        @Override
        public boolean ignoreItemInMerge(ResourceItem item) {
            // we never ignore any item.
            return false;
        }
    }
}
