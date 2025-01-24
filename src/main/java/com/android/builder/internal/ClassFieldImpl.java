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

package com.android.builder.internal;

import com.android.annotations.NonNull;
import com.android.annotations.concurrency.Immutable;
import com.android.builder.model.ClassField;
import com.google.common.collect.ImmutableSet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Immutable implementation of {@link ClassField}
 */
@Immutable
public final class ClassFieldImpl implements ClassField, Serializable {
    /**
     * Serializable implementation of ClassFieldImpl
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The type of this field.
     */
    @NonNull
    private final String type;
    /**
     * The name of this field.
     */
    @NonNull
    private final String name;
    /**
     * The value of this field.
     */
    @NonNull
    private final String value;
    /**
     * The set of annotations for this field.
     */
    @NonNull
    private final Set<String> annotations;
    /**
     * The documentation for this field.
     */
    @NonNull
    private final String documentation;

    /**
     * @param type  the type of this field.
     * @param name  the name of this field.
     * @param value the value of this field.
     */
    public ClassFieldImpl(@NonNull String type, @NonNull String name, @NonNull String value) {
        this(type, name, value, ImmutableSet.of(), "");
    }

    /**
     * @param type          the type of this field.
     * @param name          the name of this field.
     * @param value         the value of this field.
     * @param annotations   the set of annotations for this field.
     * @param documentation the documentation for this field.
     */
    public ClassFieldImpl(@NonNull String type, @NonNull String name, @NonNull String value,
                          @NonNull Set<String> annotations, @NonNull String documentation) {
        //noinspection ConstantConditions
        if (type == null || name == null || value == null || annotations == null || documentation == null) {
            throw new NullPointerException("Build Config field cannot have a null parameter");
        }
        this.type = type;
        this.name = name;
        this.value = value;
        this.annotations = ImmutableSet.copyOf(annotations);
        this.documentation = documentation;
    }

    /**
     * @return the type of this field.
     */
    @Override
    @NonNull
    public String getType() {
        return type;
    }

    /**
     * @return the name of this field.
     */
    @Override
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * @return the value of this field.
     */
    @Override
    @NonNull
    public String getValue() {
        return value;
    }

    /**
     * @return the documentation for this field.
     */
    @NonNull
    @Override
    public String getDocumentation() {
        return documentation;
    }

    /**
     * @return the set of annotations for this field.
     */
    @NonNull
    @Override
    public Set<String> getAnnotations() {
        return annotations;
    }

    /**
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassFieldImpl that = (ClassFieldImpl) o;

        if (!name.equals(that.name)) return false;
        if (!type.equals(that.type)) return false;
        if (!value.equals(that.value)) return false;
        if (!annotations.equals(that.annotations)) return false;
        return documentation.equals(that.documentation);
    }

    /**
     * @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + annotations.hashCode();
        result = 31 * result + documentation.hashCode();
        return result;
    }
}
