package nl.inl.blacklab.resultproperty;

import java.util.Arrays;
import java.util.List;

import nl.inl.blacklab.forwardindex.Terms;
import nl.inl.blacklab.search.BlackLabIndex;
import nl.inl.blacklab.search.indexmetadata.AnnotatedField;
import nl.inl.blacklab.search.indexmetadata.Annotation;
import nl.inl.blacklab.search.indexmetadata.MatchSensitivity;
import nl.inl.util.ArrayUtil;

public class HitPropValueContextWords extends HitPropValueContext {
    int[] valueTokenId;

    int[] valueSortOrder;

    private MatchSensitivity sensitivity;

    public HitPropValueContextWords(BlackLabIndex index, Annotation annotation, MatchSensitivity sensitivity, int[] value) {
        super(index, annotation);
        this.sensitivity = sensitivity;
        this.valueTokenId = value;
        this.valueSortOrder = new int[value.length];
        terms.toSortOrder(value, valueSortOrder, sensitivity);
    }

    @Override
    public int compareTo(Object o) {
        return ArrayUtil.compareArrays(valueSortOrder, ((HitPropValueContextWords) o).valueSortOrder);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(valueSortOrder);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof HitPropValueContextWords)
            return Arrays.equals(valueSortOrder, ((HitPropValueContextWords) obj).valueSortOrder);
        return false;
    }

    public static HitPropValue deserialize(BlackLabIndex index, AnnotatedField field, String info) {
        String[] parts = PropValSerializeUtil.splitParts(info);
        String propName = parts[0];
        Annotation annotation = field.annotation(propName);
        MatchSensitivity sensitivity = MatchSensitivity.fromLuceneFieldSuffix(parts[1]);
        int[] ids = new int[parts.length - 2];
        Terms termsObj = index.annotationForwardIndex(annotation).terms();
        for (int i = 2; i < parts.length; i++) {
            ids[i - 2] = termsObj.deserializeToken(parts[i]);
        }
        return new HitPropValueContextWords(index, annotation, sensitivity, ids);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int v : valueTokenId) {
            String word = v < 0 ? "-" : terms.get(v);
            if (word.length() > 0) {
                if (b.length() > 0)
                    b.append(" ");
                b.append(word);
            }
        }
        return b.toString();
    }

    @Override
    public String serialize() {
        String[] parts = new String[valueTokenId.length + 3];
        parts[0] = "cws";
        parts[1] = annotation.name();
        parts[2] = sensitivity.luceneFieldSuffix();
        for (int i = 0; i < valueTokenId.length; i++) {
            parts[i + 3] = terms.serializeTerm(valueTokenId[i]);
        }
        return PropValSerializeUtil.combineParts(parts);
    }

    @Override
    public List<String> getPropValues() {
        return Arrays.asList(this.toString());
    }
}
