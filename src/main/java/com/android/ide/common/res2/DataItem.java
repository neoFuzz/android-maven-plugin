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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Objects;

/**
 * Base item.
 * <p>
 * This includes its name and source file as a {@link DataFile}.
 */
public abstract class DataItem<F extends DataFile> {

    private static final int MASK_TOUCHED = 0x01;
    private static final int MASK_REMOVED = 0x02;
    private static final int MASK_WRITTEN = 0x10;

    private final String mName;
    private F mSource;

    /**
     * The status of the Item. It's a bit mask as opposed to an enum
     * to differentiate removed and removed+written
     */
    private int mStatus = 0;

    /**
     * Constructs the object with a name, type and optional value.
     * <p>
     * Note that the object is not fully usable as-is. It must be added to a DataFile first.
     *
     * @param name the name of the item
     */
    DataItem(@NonNull String name) {
        mName = name;
    }

    /**
     * Returns the name of the item.
     *
     * @return the name.
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns the DataFile the item is coming from. Can be null.
     *
     * @return the data file.
     */
    @Nullable
    public F getSource() {
        return mSource;
    }

    /**
     * Sets the DataFile
     *
     * @param sourceFile the DataFile
     */
    public void setSource(F sourceFile) {
        mSource = sourceFile;
    }

    /**
     * Resets the state of the item be nothing.
     *
     * @return this
     */
    DataItem resetStatus() {
        mStatus = 0;
        return this;
    }

    /**
     * Resets the state of the item be WRITTEN. All other states are removed.
     *
     * @return this
     * @see #isWritten()
     */
    DataItem resetStatusToWritten() {
        mStatus = MASK_WRITTEN;
        return this;
    }

    /**
     * Resets the state of the item be TOUCHED. All other states are removed.
     *
     * @return this
     * @see #isWritten()
     */
    DataItem resetStatusToTouched() {
        mStatus = MASK_TOUCHED;
        return this;
    }

    /**
     * Sets the item status to be WRITTEN. Other states are kept.
     *
     * @return this
     * @see #isWritten()
     */
    DataItem setWritten() {
        mStatus |= MASK_WRITTEN;
        return this;
    }

    /**
     * Sets the item status to be REMOVED. Other states are kept.
     *
     * @return this
     * @see #isRemoved()
     */
    DataItem setRemoved() {
        mStatus |= MASK_REMOVED;
        return this;
    }

    /**
     * Sets the item status to be TOUCHED. Other states are kept.
     *
     * @return this
     * @see #isTouched()
     */
    DataItem setTouched() {
        mStatus |= MASK_TOUCHED;
        wasTouched();
        return this;
    }

    /**
     * Returns whether the item status is REMOVED
     *
     * @return true if removed
     */
    boolean isRemoved() {
        return (mStatus & MASK_REMOVED) != 0;
    }

    /**
     * Returns whether the item status is TOUCHED
     *
     * @return true if touched
     */
    boolean isTouched() {
        return (mStatus & MASK_TOUCHED) != 0;
    }

    /**
     * Returns whether the item status is WRITTEN
     *
     * @return true if written
     */
    boolean isWritten() {
        return (mStatus & MASK_WRITTEN) != 0;
    }

    /**
     * @return the status of the item
     */
    protected int getStatus() {
        return mStatus;
    }

    /**
     * Returns a key for this item. They key uniquely identifies this item. This is the name.
     *
     * @return the key for this item.
     */
    public String getKey() {
        return getName();
    }

    /**
     * @param document     the document to create the node with
     * @param node         the node to add the extra attributes to
     * @param namespaceUri the namespace URI to use for the attributes
     */
    void addExtraAttributes(Document document, Node node, @Nullable String namespaceUri) {
        // nothing
    }

    /**
     * @param document the document to create the node with
     * @return the adopted node or null if not applicable
     */
    Node getAdoptedNode(Document document) {
        return null;
    }

    /**
     * @param o the object to compare to
     * @return true if the object is equal to this item
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataItem dataItem = (DataItem) o;

        if (!mName.equals(dataItem.mName)) {
            return false;
        }

        return Objects.equals(mSource, dataItem.mSource);
    }

    /**
     * @return the hash code for this item
     */
    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + (mSource != null ? mSource.hashCode() : 0);
        return result;
    }

    /**
     * does nothing
     */
    protected void wasTouched() {

    }
}
