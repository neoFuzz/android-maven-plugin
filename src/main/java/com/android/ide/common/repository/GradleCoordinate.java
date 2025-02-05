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
package com.android.ide.common.repository;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a maven coordinate and allows for comparison at any level.
 * <p>
 * This class does not directly implement {@link java.lang.Comparable}; instead,
 * you should use one of the specific {@link java.util.Comparator} constants based
 * on what type of ordering you need.
 */
public class GradleCoordinate {
    /**
     * the preview id
     */
    public static final String PREVIEW_ID = "rc";
    /**
     * the Plus Revision ID
     */
    public static final int PLUS_REV_VALUE = -1;

    /**
     * Comparator which compares Gradle versions - and treats a + version as higher
     * than a specific number. This is typically useful when seeing if a dependency
     * is met, e.g. if you require version 0.7.3, comparing it with 0.7.+ would consider
     * 0.7.+ higher and therefore satisfying the version requirement.
     */
    public static final Comparator<GradleCoordinate> COMPARE_PLUS_HIGHER =
            new GradleCoordinateComparator(1);
    /**
     * Maven coordinates take the following form: groupId:artifactId:packaging:classifier:version
     * where
     * groupId is dot-notated alphanumeric
     * artifactId is the name of the project
     * packaging is optional and is jar/war/pom/aar/etc
     * classifier is optional and provides filtering context
     * version uniquely identifies a version.
     * <p>
     * We only care about coordinates of the following form: groupId:artifactId:revision
     * where revision is a series of '.' separated numbers optionally terminated by a '+' character.
     */
    static final PlusComponent PLUS_REV = new PlusComponent();
    private static final Pattern MAVEN_PATTERN =
            Pattern.compile("([\\w\\d\\.-]+):([\\w\\d\\.-]+):([^:@]+)(@[\\w-]+)?");
    private final String mGroupId;
    private final String mArtifactId;
    private final ArtifactType mArtifactType;
    private final List<RevisionComponent> mRevisions = new ArrayList<>(3);


    /**
     * @param groupId    the group ID
     * @param artifactId The artifact ID
     * @param revisions  the revision components of the coordinate
     * @param type       the type of artifact, or null if not specified
     */
    public GradleCoordinate(@NonNull String groupId, @NonNull String artifactId,
                            @NonNull List<RevisionComponent> revisions, @Nullable ArtifactType type) {
        mGroupId = groupId;
        mArtifactId = artifactId;
        mRevisions.addAll(revisions);

        mArtifactType = type;
    }

    /**
     * Create a GradleCoordinate from a string of the form groupId:artifactId:MajorRevision.MinorRevision.(MicroRevision|+)
     *
     * @param coordinateString the string to parse
     * @return a coordinate object or null if the given string was malformed.
     */
    @Nullable
    public static GradleCoordinate parseCoordinateString(@NonNull String coordinateString) {
        Matcher matcher = MAVEN_PATTERN.matcher(coordinateString);
        if (!matcher.matches()) {
            return null;
        }

        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String revision = matcher.group(3);
        String typeString = matcher.group(4);
        ArtifactType type = null;

        if (typeString != null) {
            // Strip off the '@' symbol and try to convert
            type = ArtifactType.getArtifactType(typeString.substring(1));
        }

        List<RevisionComponent> revisions = parseRevisionNumber(revision);

        return new GradleCoordinate(groupId, artifactId, revisions, type);
    }

    /**
     * @param revision Revision number to parse
     * @return a list of revision components
     */
    @NonNull
    public static List<RevisionComponent> parseRevisionNumber(@NonNull String revision) {
        List<RevisionComponent> components = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < revision.length(); i++) {
            char c = revision.charAt(i);
            if (c == '.') {
                flushBuffer(components, buffer, true);
            } else if (c == '+') {
                if (!buffer.isEmpty()) {
                    flushBuffer(components, buffer, true);
                }
                components.add(PLUS_REV);
                break;
            } else if (c == '-') {
                flushBuffer(components, buffer, false);
                int last = components.size() - 1;
                if (last == -1) {
                    components.add(ListComponent.of(new NumberComponent(0)));
                } else if (!(components.get(last) instanceof ListComponent)) {
                    components.set(last, ListComponent.of(components.get(last)));
                }
            } else {
                buffer.append(c);
            }
        }
        if (!buffer.isEmpty() || components.isEmpty()) {
            flushBuffer(components, buffer, true);
        }
        return components;
    }

    /**
     * @param components the list of components to convert to a string
     * @param buffer     the string buffer to use
     * @param closeList  if true, close the list component if it exists
     */
    private static void flushBuffer(@NonNull List<RevisionComponent> components, StringBuilder buffer,
                                    boolean closeList) {
        RevisionComponent newComponent = getRevisionComponent(buffer);
        buffer.setLength(0);
        if (!components.isEmpty() &&
                components.get(components.size() - 1) instanceof ListComponent component &&
                !component.mClosed) {
            component.add(newComponent);
            if (closeList) {
                component.mClosed = true;
            }
            return;
        }

        components.add(newComponent);
    }

    /**
     * @param buffer the string buffer to use
     * @return a revision component based on the given string buffer
     */
    @NonNull
    private static RevisionComponent getRevisionComponent(@NonNull StringBuilder buffer) {
        RevisionComponent newComponent;
        if (buffer.isEmpty()) {
            newComponent = new NumberComponent(0);
        } else {
            String string = buffer.toString();
            try {
                int number = Integer.parseInt(string);
                if (string.length() > 1 && string.charAt(0) == '0') {
                    newComponent = new PaddedNumberComponent(number, string);
                } else {
                    newComponent = new NumberComponent(number);
                }
            } catch (NumberFormatException e) {
                newComponent = new StringComponent(string);
            }
        }
        return newComponent;
    }

    /**
     * @return the string representation of this coordinate
     */
    @Override
    public String toString() {
        String s = String.format(Locale.US, "%s:%s:%s", mGroupId, mArtifactId, getFullRevision());
        if (mArtifactType != null) {
            s += "@" + mArtifactType;
        }
        return s;
    }

    /**
     * @return the string representation of this coordinate, with the revision number
     * (i.e. without the plus sign)
     */
    @Nullable
    public String getGroupId() {
        return mGroupId;
    }

    /**
     * @return the string representation of this coordinate, with the revision number
     * (i.e. without the plus sign)
     */
    @Nullable
    public String getArtifactId() {
        return mArtifactId;
    }

    /**
     * @return the string representation of this coordinate, with the revision number
     * (i.e. without the plus sign)
     */
    @Nullable
    public String getId() {
        if (mGroupId == null || mArtifactId == null) {
            return null;
        }

        return String.format("%s:%s", mGroupId, mArtifactId);
    }

    /**
     * @return the artifact type, or null if not specified
     */
    @Nullable
    public ArtifactType getType() {
        return mArtifactType;
    }

    /**
     * @return the full revision string, including any plus sign
     */
    public String getFullRevision() {
        StringBuilder revision = new StringBuilder();
        for (RevisionComponent component : mRevisions) {
            if (!revision.isEmpty()) {
                revision.append('.');
            }
            revision.append(component.toString());
        }

        return revision.toString();
    }

    /**
     * @return if the coordinate is a preview coordinate
     */
    public boolean isPreview() {
        return !mRevisions.isEmpty() && mRevisions.get(mRevisions.size() - 1).isPreview();
    }

    /**
     * Returns true if and only if the given coordinate refers to the same group and artifact.
     *
     * @param o the coordinate to compare with
     * @return true iff the other group and artifact match the group and artifact of this
     * coordinate.
     */
    public boolean isSameArtifact(@NonNull GradleCoordinate o) {
        return o.mGroupId.equals(mGroupId) && o.mArtifactId.equals(mArtifactId);
    }

    /**
     * @param o the object to compare with
     * @return true iff the other object is a GradleCoordinate and refers to the same artifact
     * as this coordinate.
     */
    @Override
    public boolean equals(@NonNull Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GradleCoordinate that = (GradleCoordinate) o;

        if (!mRevisions.equals(that.mRevisions)) {
            return false;
        }
        if (!mArtifactId.equals(that.mArtifactId)) {
            return false;
        }
        if (!mGroupId.equals(that.mGroupId)) {
            return false;
        }
        if ((mArtifactType == null) != (that.mArtifactType == null)) {
            return false;
        }
        return mArtifactType == null || mArtifactType.equals(that.mArtifactType);
    }

    /**
     * @return the hash code for this coordinate
     */
    @Override
    public int hashCode() {
        int result = mGroupId.hashCode();
        result = 31 * result + mArtifactId.hashCode();
        for (RevisionComponent component : mRevisions) {
            result = 31 * result + component.hashCode();
        }
        if (mArtifactType != null) {
            result = 31 * result + mArtifactType.hashCode();
        }
        return result;
    }

    /**
     * List taken from <a href="http://maven.apache.org/pom.html#Maven_Coordinates">http://maven.apache.org/pom.html#Maven_Coordinates</a>
     */
    public enum ArtifactType {
        /**
         * The artifact type for a POM file.
         */
        POM("pom"),
        /**
         * The artifact type for a JAR file.
         */
        JAR("jar"),
        /**
         * The artifact type for a {@code maven-plugin}.
         */
        MAVEN_PLUGIN("maven-plugin"),
        /**
         * The artifact type for a EJB file.
         */
        EJB("ejb"),
        /**
         * The artifact type for a WAR file.
         */
        WAR("war"),
        /**
         * The artifact type for an EAR file.
         */
        EAR("ear"),
        /**
         * The artifact type for a RAR file.
         */
        RAR("rar"),
        /**
         * The artifact type for a PAR file.
         */
        PAR("par"),
        /**
         * The artifact type for an AAR file.
         */
        AAR("aar");

        /**
         * The artifact type as it appears in the string representation of the coordinate
         */
        private final String mId;

        /**
         * @param id the artifact type as it appears in the string representation of the coordinate
         */
        ArtifactType(String id) {
            mId = id;
        }

        /**
         * @param name the artifact type as it appears in the string representation of the coordinate
         * @return the artifact type, or null if not found
         */
        @Nullable
        public static ArtifactType getArtifactType(@Nullable String name) {
            if (name != null) {
                for (ArtifactType type : ArtifactType.values()) {
                    if (type.mId.equalsIgnoreCase(name)) {
                        return type;
                    }
                }
            }
            return null;
        }

        /**
         * @return the artifact type as it appears in the string representation of the coordinate
         */
        @Override
        public String toString() {
            return mId;
        }
    }

    /**
     * A single component of a revision number: either a number, a string or a list of
     * components separated by dashes.
     */
    public abstract static class RevisionComponent implements Comparable<RevisionComponent> {
        /**
         * @return the integer value of this component, or 0 if this component is not a number
         */
        public abstract int asInteger();

        /**
         * @return true if this component is a preview component, false otherwise
         */
        public abstract boolean isPreview();
    }

    /**
     * A component that represents a list of components separated by dashes.
     */
    public static class NumberComponent extends RevisionComponent {
        /**
         * The number value of this component
         */
        private final int mNumber;

        /**
         * @param number the number value of this component
         */
        public NumberComponent(int number) {
            mNumber = number;
        }

        /**
         * @return the string representation of this component
         */
        @Override
        public String toString() {
            return Integer.toString(mNumber);
        }

        /**
         * @return the integer value of this component
         */
        @Override
        public int asInteger() {
            return mNumber;
        }

        /**
         * @return false, since this component is not a preview component
         */
        @Override
        public boolean isPreview() {
            return false;
        }

        /**
         * @param o the object to compare with
         * @return true if the other object is a NumberComponent with the same number value
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof NumberComponent nc && nc.mNumber == mNumber;
        }

        /**
         * @return the hash code of this component, based on the number value
         */
        @Override
        public int hashCode() {
            return mNumber;
        }

        /**
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object is less than,
         * equal to, or greater than the specified object.
         */
        @Override
        public int compareTo(@NonNull RevisionComponent o) {
            if (o instanceof NumberComponent nc) {
                return mNumber - nc.mNumber;
            }
            if (o instanceof StringComponent) {
                return 1;
            }
            if (o instanceof ListComponent) {
                return 1; // 1.0.x > 1-1
            }
            return 0;
        }
    }

    /**
     * Like NumberComponent, but used for numeric strings that have leading zeroes which
     * we must preserve
     */
    public static class PaddedNumberComponent extends NumberComponent {
        /**
         * The string representation of this component
         */
        private final String mString;

        /**
         * @param number the number value of this component
         * @param string the string representation of this component
         */
        public PaddedNumberComponent(int number, String string) {
            super(number);
            mString = string;
        }

        /**
         * @return the string representation of this component
         */
        @Override
        public String toString() {
            return mString;
        }

        /**
         * @param o the object to compare with
         * @return true if the other object is a PaddedNumberComponent with the same string value
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof PaddedNumberComponent pnc &&
                    pnc.mString.equals(mString);
        }

        /**
         * @return the hash code of this component, based on the string value
         */
        @Override
        public int hashCode() {
            return mString.hashCode();
        }
    }

    /**
     * A component that represents a string.
     */
    public static class StringComponent extends RevisionComponent {
        /**
         * The string representation of this component
         */
        private final String mString;

        /**
         * @param string the string representation of this component
         */
        public StringComponent(String string) {
            this.mString = string;
        }

        /**
         * @return the string representation of this component
         */
        @Override
        public String toString() {
            return mString;
        }

        /**
         * @return 0, since this component is not a number component
         */
        @Override
        public int asInteger() {
            return 0;
        }

        /**
         * @return true if this component is a preview component, false otherwise
         */
        @Override
        public boolean isPreview() {
            return mString.startsWith(PREVIEW_ID);
        }

        /**
         * @param o the object to compare with
         * @return true if the other object is a StringComponent with the same string value
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof StringComponent sc && sc.mString.equals(mString);
        }

        /**
         * @return the hash code of this component, based on the string value
         */
        @Override
        public int hashCode() {
            return mString.hashCode();
        }

        /**
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object is less than,
         * equal to, or greater than the specified object.
         */
        @Override
        public int compareTo(@NonNull RevisionComponent o) {
            if (o instanceof NumberComponent) {
                return -1;
            }
            if (o instanceof StringComponent sc) {
                return mString.compareTo(sc.mString);
            }
            if (o instanceof ListComponent) {
                return -1;  // 1-sp < 1-1
            }
            return 0;
        }
    }

    /**
     * A component that represents the "+" symbol.
     */
    static class PlusComponent extends RevisionComponent {
        /**
         * @return the string representation of this component
         */
        @Override
        public String toString() {
            return "+";
        }

        /**
         * @return the integer value of this component, which is the constant PLUS_REV_VALUE
         */
        @Override
        public int asInteger() {
            return PLUS_REV_VALUE;
        }

        /**
         * @return false, since this component is not a preview component
         */
        @Override
        public boolean isPreview() {
            return false;
        }

        /**
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object is less than,
         * equal to, or greater than the specified object.
         */
        @Override
        public int compareTo(@NonNull RevisionComponent o) {
            throw new UnsupportedOperationException(
                    "Please use a specific comparator that knows how to handle +");
        }
    }

    /**
     * A list of components separated by dashes.
     */
    public static class ListComponent extends RevisionComponent {
        /**
         * The list of components
         */
        private final List<RevisionComponent> mItems = new ArrayList<>();
        /**
         * Whether this list is closed or not. A closed list cannot be modified anymore.
         */
        private boolean mClosed = false;

        /**
         * @param components the components to add to this list
         * @return a new ListComponent with the given components
         */
        @NonNull
        public static ListComponent of(@NonNull RevisionComponent... components) {
            ListComponent result = new ListComponent();
            for (RevisionComponent component : components) {
                result.add(component);
            }
            return result;
        }

        /**
         * @param component the component to add to this list
         */
        public void add(RevisionComponent component) {
            mItems.add(component);
        }

        /**
         * @return 0
         */
        @Override
        public int asInteger() {
            return 0;
        }

        /**
         * @return true if this list is a preview list, false otherwise
         */
        @Override
        public boolean isPreview() {
            return !mItems.isEmpty() && mItems.get(mItems.size() - 1).isPreview();
        }

        /**
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object is less than,
         * equal to, or greater than the specified object.
         */
        @Override
        public int compareTo(@NonNull RevisionComponent o) {
            if (o instanceof NumberComponent) {
                return -1;  // 1-1 < 1.0.x
            }
            if (o instanceof StringComponent) {
                return 1;  // 1-1 > 1-sp
            }
            if (o instanceof ListComponent rhs) {
                for (int i = 0; i < mItems.size() && i < rhs.mItems.size(); i++) {
                    int rc = mItems.get(i).compareTo(rhs.mItems.get(i));
                    if (rc != 0) return rc;
                }
                return mItems.size() - rhs.mItems.size();
            }
            return 0;
        }

        /**
         * @param o the object to compare with
         * @return true if the other object is a ListComponent with the same list of components
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof ListComponent lc && lc.mItems.equals(mItems);
        }

        /**
         * @return the hash code of this component, based on the list of components
         */
        @Override
        public int hashCode() {
            return mItems.hashCode();
        }

        /**
         * @return the string representation of this list component, joined by dashes
         */
        @Override
        public String toString() {
            return Joiner.on("-").join(mItems);
        }
    }

    /**
     * Comparator for Gradle coordinates. The comparison is based on the groupId, artifactId
     * and revision number. The revision number is compared component by component, with
     * the last component being a special case: if it is a plus component, it is considered
     * higher than any specific number.
     */
    private static class GradleCoordinateComparator implements Comparator<GradleCoordinate> {
        /**
         * The plus result
         */
        private final int mPlusResult;

        /**
         * @param plusResult the plus result to use for comparison
         */
        private GradleCoordinateComparator(int plusResult) {
            mPlusResult = plusResult;
        }

        /**
         * Compares two Gradle coordinates. The comparison is based on the groupId, artifactId
         * and revision number. The revision number is compared component by component, with
         * the last component being a special case: if it is a plus component, it is considered
         * higher than any specific number.
         *
         * @param a the first object to be compared.
         * @param b the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(@NonNull GradleCoordinate a, @NonNull GradleCoordinate b) {
            // Make sure we're comparing apples to apples. If not, compare artifactIds
            if (!a.isSameArtifact(b)) {
                return a.mArtifactId.compareTo(b.mArtifactId);
            }

            int sizeA = a.mRevisions.size();
            int sizeB = b.mRevisions.size();
            int common = Math.min(sizeA, sizeB);
            for (int i = 0; i < common; ++i) {
                Integer mPlusResult1 = getInteger(a, b, i);
                if (mPlusResult1 != null) return mPlusResult1;
            }
            if (sizeA != sizeB) {
                // Treat X.0 and X.0.0 as equal
                List<RevisionComponent> revisionList;
                int returnValueIfNonZero;
                int from;
                int to;
                if (sizeA < sizeB) {
                    revisionList = b.mRevisions;
                    from = sizeA;
                    to = sizeB;
                    returnValueIfNonZero = -1;
                } else {
                    revisionList = a.mRevisions;
                    from = sizeB;
                    to = sizeA;
                    returnValueIfNonZero = 1;
                }
                for (int i = from; i < to; ++i) {
                    Integer returnValueIfNonZero1 = getInteger2(revisionList, i, returnValueIfNonZero);
                    if (returnValueIfNonZero1 != null) return returnValueIfNonZero1;
                }
            }
            return 0;
        }

        /**
         * @param revisionList         the revision list to get the integer from
         * @param i                    the index to get the integer from
         * @param returnValueIfNonZero the return value if the integer is not zero
         * @return the integer value of the revision component, or the return value if the integer is not zero
         */
        @Nullable
        private Integer getInteger2(@NonNull List<RevisionComponent> revisionList, int i, int returnValueIfNonZero) {
            RevisionComponent revision = revisionList.get(i);
            if (revision instanceof NumberComponent) {
                if (revision.asInteger() != 0) {
                    return returnValueIfNonZero;
                }
            } else {
                return returnValueIfNonZero;
            }
            return null;
        }

        /**
         * @param a the Gradle coordinates to compare
         * @param b the Gradle coordinates to compare
         * @param i the index to get the integer from
         * @return the integer value of the revision component, or null if the component is not an integer
         */
        @Nullable
        private Integer getInteger(@NonNull GradleCoordinate a, @NonNull GradleCoordinate b, int i) {
            RevisionComponent revision1 = a.mRevisions.get(i);
            if (revision1 instanceof PlusComponent) return mPlusResult;
            RevisionComponent revision2 = b.mRevisions.get(i);
            if (revision2 instanceof PlusComponent) return -mPlusResult;
            int delta = revision1.compareTo(revision2);
            if (delta != 0) {
                return delta;
            }
            return null;
        }
    }
}
