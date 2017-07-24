package nl.inl.blacklab.index.xpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.inl.blacklab.search.indexstructure.FieldType;
import nl.inl.blacklab.search.indexstructure.MetadataFieldDesc.UnknownCondition;

/** Configuration for metadata field(s). */
public class ConfigMetadataField {

    /** Metadata field name (or name XPath, if forEach) */
    private String fieldName;

    /** How to display the field in the interface (optional) */
    private String displayName = "";

    /** How to describe the field in the interface (optional) */
    private String description = "";

    /** Where to find metadata value */
    private String valuePath;

    /** If null: regular metadata field definition. Otherwise, find all nodes matching this XPath,
     *  then evaluate fieldName and valuePath as XPaths for each matching node.
     */
    private String forEachPath;

    /** How to index the field (tokenized|untokenized|numeric) */
    private FieldType type = FieldType.TOKENIZED;

    /** What UI element to show in the interface (optional) */
    private String uiType = "";

    /** When to index the unknownValue: NEVER|MISSING|EMPTY|MISSING_OR_EMPTY (default: NEVER) */
    private UnknownCondition unknownCondition = UnknownCondition.NEVER;

    /** What to index when unknownCondition is true (default: unknown) */
    private String unknownValue = "unknown";

    /** Analyzer to use for this field */
    private String analyzer = "";

    /** Mapping from value to displayValue (optional) */
    private Map<String, String> displayValues = new HashMap<>();

    /** Order in which to display the values (optional) */
    private List<String> displayOrder = new ArrayList<>();

    public ConfigMetadataField() {
    }

    public ConfigMetadataField(String name, String valuePath) {
        this(name, valuePath, null);
    }

    public ConfigMetadataField(String fieldName, String valuePath, String forEachPath) {
        setFieldName(fieldName);
        setValuePath(valuePath);
        setForEachPath(forEachPath);
    }

    public ConfigMetadataField copy() {
        return new ConfigMetadataField(fieldName, valuePath, forEachPath);
    }

    public void setForEachPath(String forEachPath) {
        this.forEachPath = forEachPath;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setValuePath(String valuePath) {
        this.valuePath = valuePath;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getValuePath() {
        return valuePath;
    }

    public String getForEachPath() {
        return forEachPath;
    }

    public boolean isForEach() {
        return forEachPath != null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUiType() {
        return uiType;
    }

    public void setUiType(String uiType) {
        this.uiType = uiType;
    }

    public UnknownCondition getUnknownCondition() {
        return unknownCondition;
    }

    public void setUnknownCondition(UnknownCondition unknownCondition) {
        this.unknownCondition = unknownCondition;
    }

    public String getUnknownValue() {
        return unknownValue;
    }

    public void setUnknownValue(String unknownValue) {
        this.unknownValue = unknownValue;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public Map<String, String> getDisplayValues() {
        return Collections.unmodifiableMap(displayValues);
    }

    public void addDisplayValue(String value, String displayValue) {
        displayValues.put(value, displayValue);
    }

    public List<String> getDisplayOrder() {
        return Collections.unmodifiableList(displayOrder);
    }

    public void addDisplayOrder(String field) {
        displayOrder.add(field);
    }

}
