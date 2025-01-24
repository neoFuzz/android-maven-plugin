/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.manifmerger;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.concurrency.Immutable;
import com.android.ide.common.blame.MessageJsonSerializer;
import com.android.ide.common.blame.SourceFile;
import com.android.ide.common.blame.SourceFilePosition;
import com.android.utils.ILogger;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.LineReader;
import com.google.gson.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Contains all actions taken during a merging invocation.
 */
@Immutable
public class Actions {

    // TODO: i18n
    @VisibleForTesting
    static final String HEADER = "-- Merging decision tree log ---\n";

    // defines all the records for the merging tool activity, indexed by element name+key.
    // iterator should be ordered by the key insertion order.
    private final Map<XmlNode.NodeKey, DecisionTreeRecord> mRecords;

    /**
     * @param records the records to serialize/deserialize
     */
    public Actions(Map<XmlNode.NodeKey, DecisionTreeRecord> records) {
        mRecords = records;
    }

    /**
     * @param inputStream the input stream to deserialize from
     * @return the {@link Actions} object deserialized from the input stream
     * @throws IOException if the input stream cannot be read
     */
    @Nullable
    public static Actions load(@NonNull InputStream inputStream) throws IOException {

        return getGsonParser().fromJson(new InputStreamReader(inputStream), Actions.class);
    }

    /**
     * Deserializes an {@link Actions} object from a string.
     *
     * @param xml the xml string to deserialize
     * @return the {@link Actions} object deserialized from the xml string
     */
    @Nullable
    public static Actions load(String xml) {
        return getGsonParser().fromJson(xml, Actions.class);
    }

    /**
     * @return a {@link Gson} instance that can be used to serialize and deserialize this object.
     */
    @NonNull
    private static Gson getGsonParser() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.enableComplexMapKeySerialization();
        gsonBuilder.registerTypeAdapter(XmlNode.NodeName.class, new NodeNameDeserializer());
        MessageJsonSerializer.registerTypeAdapters(gsonBuilder);
        return gsonBuilder.create();
    }

    /**
     * Finds the {@link NodeRecord} for the element in question.
     *
     * @param decisionTreeRecord the decision tree record for the element in question
     * @return the {@link NodeRecord} for the element in question, or {@code null} if none was found.
     */
    @Nullable
    private static Actions.NodeRecord findNodeRecord(@NonNull DecisionTreeRecord decisionTreeRecord) {
        for (Actions.NodeRecord nodeRecord : decisionTreeRecord.getNodeRecords()) {
            if (nodeRecord.getActionType() == Actions.ActionType.ADDED) {
                return nodeRecord;
            }
        }
        return null;
    }

    /**
     * Finds the {@link AttributeRecord} for the attribute in question.
     *
     * @param decisionTreeRecord the decision tree record for the element in question
     * @param xmlAttribute       the attribute in question
     * @return the attribute record for the attribute in question, or {@code null} if none was found.
     */
    @Nullable
    private static Actions.AttributeRecord findAttributeRecord(
            @NonNull DecisionTreeRecord decisionTreeRecord,
            @NonNull XmlAttribute xmlAttribute) {
        for (Actions.AttributeRecord attributeRecord : decisionTreeRecord
                .getAttributeRecords(xmlAttribute.getName())) {
            if (attributeRecord.getActionType() == Actions.ActionType.ADDED) {
                return attributeRecord;
            }
        }
        return null;
    }

    /**
     * Returns a {@link com.google.common.collect.ImmutableSet} of all the element's keys that have
     * at least one {@link NodeRecord}.
     *
     * @return the set of element keys with at least one node record.
     */
    @NonNull
    public Set<XmlNode.NodeKey> getNodeKeys() {
        return mRecords.keySet();
    }

    /**
     * Returns an {@link ImmutableList} of {@link NodeRecord} for the element identified with the
     * passed key.
     *
     * @param key the element key
     * @return the list of node records, or an empty list if none were found.
     */
    @NonNull
    public ImmutableList<NodeRecord> getNodeRecords(XmlNode.NodeKey key) {
        return mRecords.containsKey(key)
                ? mRecords.get(key).getNodeRecords()
                : ImmutableList.of();
    }

    /**
     * Returns a {@link ImmutableList} of all attributes names that have at least one record for
     * the element identified with the passed key.
     *
     * @param nodeKey the element key
     * @return the list of attribute names, or an empty list if none were found.
     */
    @NonNull
    public ImmutableList<XmlNode.NodeName> getRecordedAttributeNames(XmlNode.NodeKey nodeKey) {
        DecisionTreeRecord decisionTreeRecord = mRecords.get(nodeKey);
        if (decisionTreeRecord == null) {
            return ImmutableList.of();
        }
        return decisionTreeRecord.getAttributesRecords().keySet().asList();
    }

    /**
     * Returns the {@link com.google.common.collect.ImmutableList} of {@link AttributeRecord} for
     * the attribute identified by attributeName of the element identified by elementKey.
     *
     * @param elementKey    the element key
     * @param attributeName the attribute name
     * @return the list of attribute records, or an empty list if none were found.
     */
    @NonNull
    public ImmutableList<AttributeRecord> getAttributeRecords(XmlNode.NodeKey elementKey,
                                                              XmlNode.NodeName attributeName) {

        DecisionTreeRecord decisionTreeRecord = mRecords.get(elementKey);
        if (decisionTreeRecord == null) {
            return ImmutableList.of();
        }
        return decisionTreeRecord.getAttributeRecords(attributeName);
    }

    /**
     * Initial dump of the merging tool actions, need to be refined and spec'd out properly.
     *
     * @param logger logger to log to at INFO level.
     */
    void log(@NonNull ILogger logger) {
        logger.verbose(getLogs());
    }

    /**
     * Dump merging tool actions to a text file.
     *
     * @param fileWriter the file to write all actions into.
     * @throws IOException if the file cannot be written.
     */
    void log(@NonNull FileWriter fileWriter) throws IOException {
        fileWriter.append(getLogs());
    }

    /**
     * @return a string representation of all the actions taken during the merging invocation.
     */
    @NonNull
    private String getLogs() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HEADER);
        for (Map.Entry<XmlNode.NodeKey, Actions.DecisionTreeRecord> record : mRecords.entrySet()) {
            stringBuilder.append(record.getKey()).append("\n");
            for (Actions.NodeRecord nodeRecord : record.getValue().getNodeRecords()) {
                nodeRecord.print(stringBuilder);
                stringBuilder.append('\n');
            }
            for (Map.Entry<XmlNode.NodeName, List<Actions.AttributeRecord>> attributeRecords :
                    record.getValue().mAttributeRecords.entrySet()) {
                stringBuilder.append('\t').append(attributeRecords.getKey()).append('\n');
                for (Actions.AttributeRecord attributeRecord : attributeRecords.getValue()) {
                    stringBuilder.append("\t\t");
                    attributeRecord.print(stringBuilder);
                    stringBuilder.append('\n');
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * @return the json representation of this object.
     */
    @NonNull
    public String persist() {
        GsonBuilder gson = new GsonBuilder().setPrettyPrinting();
        gson.enableComplexMapKeySerialization();
        MessageJsonSerializer.registerTypeAdapters(gson);
        return gson.create().toJson(this);
    }

    /**
     * @param xmlDocument the xml document to process for source mapping
     * @return the mapping from the original source to the generated source.
     * @throws ParserConfigurationException if the xml document cannot be parsed
     * @throws SAXException                 if the xml document cannot be parsed
     * @throws IOException                  if the xml document cannot be parsed
     */
    public ImmutableMultimap<Integer, Record> getResultingSourceMapping(@NonNull XmlDocument xmlDocument)
            throws ParserConfigurationException, SAXException, IOException {

        SourceFile inMemory = SourceFile.UNKNOWN;

        XmlDocument loadedWithLineNumbers = XmlLoader.load(
                xmlDocument.getSelectors(),
                xmlDocument.getSystemPropertyResolver(),
                inMemory,
                xmlDocument.prettyPrint(),
                XmlDocument.Type.MAIN,
                Optional.empty() /* mainManifestPackageName */);

        ImmutableMultimap.Builder<Integer, Record> mappingBuilder = ImmutableMultimap.builder();
        for (XmlElement xmlElement : loadedWithLineNumbers.getRootNode().getMergeableElements()) {
            parse(xmlElement, mappingBuilder);
        }
        return mappingBuilder.build();
    }

    /**
     * @param element  the element to process for source mapping
     * @param mappings the mapping builder to add the mapping to
     */
    private void parse(@NonNull XmlElement element,
                       @NonNull ImmutableMultimap.Builder<Integer, Record> mappings) {
        DecisionTreeRecord decisionTreeRecord = mRecords.get(element.getId());
        if (decisionTreeRecord != null) {
            Actions.NodeRecord nodeRecord = findNodeRecord(decisionTreeRecord);
            if (nodeRecord != null) {
                mappings.put(element.getPosition().getStartLine(), nodeRecord);
            }
            for (XmlAttribute xmlAttribute : element.getAttributes()) {
                Actions.AttributeRecord attributeRecord = findAttributeRecord(decisionTreeRecord,
                        xmlAttribute);
                if (attributeRecord != null) {
                    mappings.put(xmlAttribute.getPosition().getStartLine(), attributeRecord);
                }
            }
        }
        for (XmlElement xmlElement : element.getMergeableElements()) {
            parse(xmlElement, mappings);
        }
    }

    /**
     * @param xmlDocument the xml document to process for source mapping
     * @return the blame string for the xml document
     * @throws IOException                  if the xml document cannot be parsed
     * @throws SAXException                 if the xml document cannot be parsed
     * @throws ParserConfigurationException if the xml document cannot be parsed
     */
    @NonNull
    public String blame(@NonNull XmlDocument xmlDocument)
            throws IOException, SAXException, ParserConfigurationException {

        ImmutableMultimap<Integer, Record> resultingSourceMapping =
                getResultingSourceMapping(xmlDocument);
        LineReader lineReader = new LineReader(
                new StringReader(xmlDocument.prettyPrint()));

        StringBuilder actualMappings = new StringBuilder();
        String line;
        int count = 0;
        while ((line = lineReader.readLine()) != null) {
            actualMappings.append(count + 1).append(line).append("\n");
            if (resultingSourceMapping.containsKey(count)) {
                for (Record record : resultingSourceMapping.get(count)) {
                    actualMappings.append(count + 1).append("-->")
                            .append(record.getActionLocation())
                            .append("\n");
                }
            }
            count++;
        }
        return actualMappings.toString();
    }

    /**
     * Defines all possible actions taken from the merging tool for an XML element or attribute.
     */
    public enum ActionType {
        /**
         * The element was added into the resulting merged manifest.
         */
        ADDED,
        /**
         * The element was injected from the merger invocation parameters.
         */
        INJECTED,
        /**
         * The element was merged with another element into the resulting merged manifest.
         */
        MERGED,
        /**
         * The element was rejected.
         */
        REJECTED,
        /**
         * The implied element was added when importing a library that expected the
         * element to be present by default while targeted SDK requires its declaration.
         */
        IMPLIED,
    }

    /**
     * Defines an abstract record contain common metadata for elements and attributes actions.
     */
    public abstract static class Record {

        /**
         * Defines a record for an XML element or attribute action.
         */
        @NonNull
        protected final ActionType mActionType;
        /**
         * The location of the action, or {@link SourceFilePosition#UNKNOWN} if none was specified
         */
        @NonNull
        protected final SourceFilePosition mActionLocation;
        /**
         * The element key for this record
         */
        @NonNull
        protected final XmlNode.NodeKey mTargetId;
        /**
         * The reason for this action, or {@code null} if none was specified
         */
        @Nullable
        protected final String mReason;

        /**
         * Creates a new record.
         *
         * @param actionType     the action type
         * @param actionLocation the location of the action, or {@link SourceFilePosition#UNKNOWN} if
         *                       none was specified
         * @param targetId       the element key for this record
         * @param reason         the reason for this action, or {@code null} if none was specified
         */
        private Record(@NonNull ActionType actionType,
                       @NonNull SourceFilePosition actionLocation,
                       @NonNull XmlNode.NodeKey targetId,
                       @Nullable String reason) {
            mActionType = Preconditions.checkNotNull(actionType);
            mActionLocation = Preconditions.checkNotNull(actionLocation);
            mTargetId = Preconditions.checkNotNull(targetId);
            mReason = reason;
        }

        /**
         * @return the action type
         */
        @NonNull
        public ActionType getActionType() {
            return mActionType;
        }

        /**
         * @return the location of the action, or {@link SourceFilePosition#UNKNOWN} if none was
         * specified
         */
        @NonNull
        public SourceFilePosition getActionLocation() {
            return mActionLocation;
        }

        /**
         * @return the element key for this record
         */
        @NonNull
        public XmlNode.NodeKey getTargetId() {
            return mTargetId;
        }

        /**
         * @return the reason for this action, or {@code null} if none was specified
         */
        @Nullable
        public String getReason() {
            return mReason;
        }

        /**
         * Prints the record to the passed string builder.
         *
         * @param stringBuilder the string builder to append to
         */
        public void print(@NonNull StringBuilder stringBuilder) {
            stringBuilder.append(mActionType)
                    .append(" from ")
                    .append(mActionLocation);
            if (mReason != null) {
                stringBuilder.append(" reason: ")
                        .append(mReason);
            }
        }
    }

    /**
     * Defines a merging tool action for an XML element.
     */
    public static class NodeRecord extends Record {

        /**
         * The operation type for this node record.
         */
        @NonNull
        private final NodeOperationType mNodeOperationType;

        /**
         * Creates a new node record.
         *
         * @param actionType        the action type
         * @param actionLocation    the location of the action, or {@link SourceFilePosition#UNKNOWN} if
         *                          none was specified
         * @param targetId          the element key for this record
         * @param reason            the reason for this action, or {@code null} if none was specified
         * @param nodeOperationType the operation type for this node record
         */
        NodeRecord(@NonNull ActionType actionType,
                   @NonNull SourceFilePosition actionLocation,
                   @NonNull XmlNode.NodeKey targetId,
                   @Nullable String reason,
                   @NonNull NodeOperationType nodeOperationType) {
            super(actionType, actionLocation, targetId, reason);
            this.mNodeOperationType = Preconditions.checkNotNull(nodeOperationType);
        }

        /**
         * Get the object as a string.
         *
         * @return the operation type for this node record
         */
        @NonNull
        @Override
        public String toString() {
            return "Id=" + mTargetId + " actionType=" + getActionType()
                    + " location=" + getActionLocation()
                    + " opType=" + mNodeOperationType;
        }
    }

    /**
     * Defines a merging tool action for an XML attribute
     */
    public static class AttributeRecord extends Record {

        // first in wins which should be fine, the first
        // operation type will be the highest priority one
        @Nullable
        private final AttributeOperationType mOperationType;

        AttributeRecord(
                @NonNull ActionType actionType,
                @NonNull SourceFilePosition actionLocation,
                @NonNull XmlNode.NodeKey targetId,
                @Nullable String reason,
                @Nullable AttributeOperationType operationType) {
            super(actionType, actionLocation, targetId, reason);
            this.mOperationType = operationType;
        }

        /**
         * Get the OperationType.
         *
         * @return the operation type for this attribute record, or {@code null} if none was specified
         */
        @Nullable
        public AttributeOperationType getOperationType() {
            return mOperationType;
        }

        /**
         * @return the object as a string
         */
        @NonNull
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("Id", mTargetId)
                    .add("actionType=", getActionType())
                    .add("location", getActionLocation())
                    .add("opType", getOperationType()).toString();
        }
    }

    /**
     * Deserializer for {@link XmlNode.NodeName} that handles the case where the namespace is not
     * present.
     */
    private static class NodeNameDeserializer implements JsonDeserializer<XmlNode.NodeName> {

        /**
         * Deserializes a {@link XmlNode.NodeName} from a JsonElement.
         *
         * @param json    The Json data being deserialized
         * @param typeOfT The type of the Object to deserialize to
         * @param context The JsonDeserializationContext
         * @return the deserialized {@link XmlNode.NodeName}
         * @throws JsonParseException if the json is not a valid representation of a XmlNode.NodeName
         */
        @Override
        public XmlNode.NodeName deserialize(@NonNull JsonElement json, Type typeOfT,
                                            @NonNull JsonDeserializationContext context) throws JsonParseException {
            if (json.getAsJsonObject().get("mNamespaceURI") != null) {
                return context.deserialize(json, XmlNode.NamespaceAwareName.class);
            } else {
                return context.deserialize(json, XmlNode.Name.class);
            }
        }
    }

    /**
     * Internal structure on how {@link com.android.manifmerger.Actions.Record}s are kept for an XML element.
     * <p>
     * Each xml element should have an associated DecisionTreeRecord which keeps a list of
     * {@link com.android.manifmerger.Actions.NodeRecord} for all the node actions related
     * to this xml element.
     * <p>
     * It will also contain a map indexed by attribute name on all the attribute actions related
     * to that particular attribute within the xml element.
     */
    static class DecisionTreeRecord {
        // all attributes decisions indexed by attribute name.
        @NonNull
        final Map<XmlNode.NodeName, List<AttributeRecord>> mAttributeRecords = new HashMap<>();
        // all other occurrences of the nodes decisions, in order of decisions.
        private final List<NodeRecord> mNodeRecords = new ArrayList<>();

        /**
         * Creates a new decision tree record.
         */
        DecisionTreeRecord() {
        }

        /**
         * Returns the list of all node records for this element.
         *
         * @return the list of all node records for this element
         */
        @NonNull
        ImmutableList<NodeRecord> getNodeRecords() {
            return ImmutableList.copyOf(mNodeRecords);
        }

        @NonNull
        ImmutableMap<XmlNode.NodeName, List<AttributeRecord>> getAttributesRecords() {
            return ImmutableMap.copyOf(mAttributeRecords);
        }

        /**
         * Adds a node record to this decision tree record.
         *
         * @param nodeRecord the node record to add
         */
        void addNodeRecord(@NonNull NodeRecord nodeRecord) {
            mNodeRecords.add(Preconditions.checkNotNull(nodeRecord));
        }

        /**
         * Adds an attribute record to this decision tree record.
         *
         * @param attributeName the attribute name for which to add the attribute record
         * @return the list of all attribute records for this element and attribute name.
         */
        @NonNull
        ImmutableList<AttributeRecord> getAttributeRecords(XmlNode.NodeName attributeName) {
            List<AttributeRecord> attributeRecords = mAttributeRecords.get(attributeName);
            return attributeRecords == null
                    ? ImmutableList.of()
                    : ImmutableList.copyOf(attributeRecords);
        }
    }
}
