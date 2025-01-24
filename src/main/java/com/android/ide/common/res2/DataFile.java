/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.google.common.collect.Maps;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Represents a data file.
 * <p>
 * It contains a link to its {@link java.io.File}, and the {@link DataItem}s it generates.
 */
public abstract class DataFile<I extends DataItem> {

    /**
     * The item list
     */
    protected final Map<String, I> mItems = Maps.newHashMap();
    /**
     * The type of the DataFile
     */
    private final FileType mType;
    /**
     * The file object
     */
    protected File mFile;

    /**
     * Creates a data file with a list of data items.
     * <p>
     * The source file is set on the items with {@link DataItem#setSource(DataFile)}
     * <p>
     * The type of the DataFile will be {@link FileType#MULTI}.
     *
     * @param file the File
     */
    DataFile(@NonNull File file, FileType fileType) {
        mType = fileType;
        mFile = file;
    }

    /**
     * This must be called from the constructor of the children classes.
     *
     * @param item the item
     */
    protected final void init(@NonNull I item) {
        addItem(item);
    }

    /**
     * This must be called from the constructor of the children classes.
     *
     * @param items the items
     */
    protected final void init(@NonNull Iterable<I> items) {
        addItems(items);
    }

    /**
     * @return the type of the DataFile
     */
    @NonNull
    FileType getType() {
        return mType;
    }

    /**
     * @return the file object
     */
    @NonNull
    public File getFile() {
        return mFile;
    }

    /**
     * @return the item list
     */
    I getItem() {
        assert mItems.size() == 1;
        return mItems.values().iterator().next();
    }

    /**
     * @return true if the DataFile has not removed items
     */
    boolean hasNotRemovedItems() {
        for (I item : mItems.values()) {
            if (!item.isRemoved()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return the item list
     */
    @NonNull
    public Collection<I> getItems() {
        return mItems.values();
    }

    /**
     * @return the item map
     */
    @NonNull
    public Map<String, I> getItemMap() {
        return mItems;
    }

    /**
     * @param item the item to add
     */
    public void addItem(@NonNull I item) {
        //noinspection unchecked
        item.setSource(this);
        mItems.put(item.getKey(), item);
    }

    /**
     * @param items the items to add
     */
    public void addItems(@NonNull Iterable<I> items) {
        for (I item : items) {
            //noinspection unchecked
            item.setSource(this);
            mItems.put(item.getKey(), item);
        }
    }

    /**
     * @param items the items to remove
     */
    public void removeItems(@NonNull Iterable<I> items) {
        for (I item : items) {
            mItems.remove(item.getKey());
            //noinspection unchecked
            item.setSource(null);
        }
    }

    /**
     * @param item the item to remove
     */
    public void removeItem(@NonNull ResourceItem item) {
        mItems.remove(item.getKey());
        item.setSource(null);
    }

    /**
     * @param document     the document to add the extra attributes
     * @param node         the node to add the extra attributes
     * @param namespaceUri the namespace URI to use for the extra attributes
     */
    void addExtraAttributes(Document document, Node node, @Nullable String namespaceUri) {
        // nothing
    }

    /**
     * @param oldItem the old item to replace
     * @param newItem the new item to replace with
     */
    public void replace(@NonNull I oldItem, @NonNull I newItem) {
        mItems.remove(oldItem.getKey());
        //noinspection unchecked
        oldItem.setSource(null);
        //noinspection unchecked
        newItem.setSource(this);
        mItems.put(newItem.getKey(), newItem);
    }

    /**
     * @return the string representation of the DataFile
     */
    @Override
    public String toString() {
        return "DataFile{" +
                "mFile=" + mFile +
                '}';
    }

    /**
     * The type of the DataFile
     */
    enum FileType {
        /**
         * A data file that contains a single item
         */
        SINGLE,
        /**
         * A data file that contains multiple items
         */
        MULTI
    }
}
