package nl.inl.blacklab.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.inl.blacklab.index.complex.ComplexFieldUtil;
import nl.inl.util.StringUtil;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;

/**
 * Determines the structure of a BlackLab index.
 */
class IndexMetadata {
	/** Possible types of metadata fields. */
	public enum FieldType {
		TEXT,
		NUMERIC
	}

	/** Types of property alternatives */
	public enum AltType {
		UNKNOWN,
		SENSITIVE
	}

	/** Description of a complex field */
	public static class ComplexFieldDesc {

		private final static List<String> bookkeepingSubfields = Arrays.asList("cid", "fiid", "length_tokens", "starttag", "endtag");

		/** Complex field's name */
		private String fieldName;

		/** This complex field's properties */
		private Map<String, PropertyDesc> props;

		/** Does the field have an associated content store? */
		private boolean contentStore;

		/** Is the field length in tokens stored? */
		private boolean lengthTokens;

		/** Are there XML tag locations stored for this field? */
		private boolean xmlTags;

		public ComplexFieldDesc(String name) {
			fieldName = name;
			props = new HashMap<String, PropertyDesc>();
			contentStore = false;
			lengthTokens = false;
			xmlTags = false;
		}

		@Override
		public String toString() {
			return fieldName + " [" + StringUtil.join(props.values(), ", ") + "]";
		}

		/** Get this complex field's name
		 * @return this field's name */
		public String getName() {
			return fieldName;
		}

		/** Get the set of property names for this complex field
		 * @return the set of properties
		 */
		public Collection<String> getProperties() {
			return props.keySet();
		}

		/**
		 * Get a property description.
		 * @param name name of the property
		 * @return the description
		 */
		public PropertyDesc getPropertyDesc(String name) {
			return props.get(name);
		}

		public boolean hasContentStore() {
			return contentStore;
		}

		public boolean hasLengthTokens() {
			return lengthTokens;
		}

		public boolean hasXmlTags() {
			return xmlTags;
		}

		/**
		 * An index field was found and split into parts, and belongs
		 * to this complex field. See what type it is and update our
		 * fields accordingly.
		 * @param parts parts of the Lucene index field name
		 */
		void processIndexField(String[] parts) {

			// See if this is a builtin bookkeeping field or a property.
			String propPart = parts.length == 1 ? "" : parts[1];
			int bookkeepingFieldIndex = bookkeepingSubfields.indexOf(propPart);
			switch (bookkeepingFieldIndex) {
			case 0: /* cid */
				// Complex field has content store
				contentStore = true;
				return;
			case 1: /* fiid */
				// Main property has forward index
				getOrCreateProperty("").setForwardIndex(true);
				return;
			case 2: /* length_tokens */
				// Complex field has length in tokens
				lengthTokens = true;
				return;
			case 3: case 4: /* starttags, endtags */
				xmlTags = true;
				return;
			}

			// Not a bookkeeping field; must be a property (alternative).
			PropertyDesc pd = getOrCreateProperty(propPart);
			if (parts.length > 2) {
				if (parts[2].equals("fiid")) {
					pd.setForwardIndex(true);
				} else {
					pd.addAlternative(parts[2]);
				}
			}
		}

		private PropertyDesc getOrCreateProperty(String name) {
			PropertyDesc pd = props.get(name);
			if (pd == null) {
				pd = new PropertyDesc(name);
				props.put(name, pd);
			}
			return pd;
		}
	}

	/** Description of a property */
	public static class PropertyDesc {
		/** The property name */
		private String propName;

		/** Any alternatives this property may have */
		private Map<String, AltDesc> alternatives;

		private boolean forwardIndex;

		public PropertyDesc(String name) {
			propName = name;
			alternatives = new HashMap<String, AltDesc>();
			forwardIndex = false;
		}

		@Override
		public String toString() {
			String altDesc = alternatives.size() > 0 ? "{" + StringUtil.join(alternatives.values(), ", ") + "}" : "";
			return (propName.length() == 0 ? "<default>" : propName) + altDesc;
		}

		public boolean hasForwardIndex() {
			return forwardIndex;
		}

		public void addAlternative(String name) {
			AltDesc altDesc = new AltDesc(name);
			alternatives.put(name, altDesc);
		}

		void setForwardIndex(boolean b) {
			forwardIndex = b;
		}

		/** Get this property's name
		 * @return the name */
		public String getName() {
			return propName;
		}

		/** Get the set of names of alternatives for this property
		 * @return the names
		 */
		public Collection<String> getAlternatives() {
			return alternatives.keySet();
		}

		/**
		 * Get an alternative's description.
		 * @param name name of the alternative
		 * @return the description
		 */
		public AltDesc getPropertyDesc(String name) {
			return alternatives.get(name);
		}
	}

	/** Description of a property alternative */
	public static class AltDesc {
		/** name of this alternative */
		private String altName;

		/** type of this alternative */
		private AltType type;

		public AltDesc(String name) {
			altName = name;
			type = name.equals("s") ? AltType.SENSITIVE : AltType.UNKNOWN;
		}

		@Override
		public String toString() {
			return altName;
		}

		/** Get the name of this alternative
		 * @return the name
		 */
		public String getName() {
			return altName;
		}

		/** Get the type of this alternative
		 * @return the type
		 */
		public AltType getType() {
			return type;
		}
	}

	/** All non-complex fields in our index (metadata fields) and their types. */
	private Map<String, FieldType> metadataFields;

	/** The complex fields in our index */
	private Map<String, ComplexFieldDesc> complexFields;

	/**
	 * Construct an IndexMetadata object, querying the index for the available
	 * fields and their types.
	 * @param reader the index for which we want metadata
	 */
	public IndexMetadata(IndexReader reader) {
		metadataFields = new HashMap<String, FieldType>();
		complexFields = new HashMap<String, ComplexFieldDesc>();

		FieldInfos fis = ReaderUtil.getMergedFieldInfos(reader);
		//reader.getFieldInfos();
		for (int i = 0; i < fis.size(); i++) {
			FieldInfo fi = fis.fieldInfo(i);
			String name = fi.name;

			// Parse the name to see if it is a metadata field or part of a complex field.
			String[] parts;
			if (name.endsWith("_ALT_numeric")) {
				// Special case: this is not a property alternative, but a numeric
				// alternative for a metadata field.
				// (TODO: this should probably be changed or removed)
				parts = new String[] {name};
			} else {
				parts = ComplexFieldUtil.getNameComponents(name);
			}
			if (parts.length == 1 && !complexFields.containsKey(parts[0])) {
				// Probably a metadata field (or the main field of a complex field;
				// if so, we'll figure that out later)
				// Detect type by finding the first document that includes this
				// field and inspecting the Fieldable. This assumes that the field type
				// is the same for all documents.
				FieldType type = FieldType.TEXT;
				for (int n = 0; n < reader.maxDoc(); n++) {
					if (!reader.isDeleted(n)) {
						Document d;
						try {
							d = reader.document(n);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
						Fieldable f = d.getFieldable(name);
						if (f != null) {
							if (f instanceof NumericField)
								type = FieldType.NUMERIC;
							break;
						}
					}
				}
				metadataFields.put(name, type);
			} else {
				// Part of complex field.
				if (metadataFields.containsKey(parts[0])) {
					// This complex field was incorrectly identified as a metadata field at first.
					// Correct this now.
					metadataFields.remove(parts[0]);
				}

				// Get or create descriptor object.
				ComplexFieldDesc cfd = getOrCreateComplexField(parts[0]);
				cfd.processIndexField(parts);
			}
		}
	}

	private ComplexFieldDesc getOrCreateComplexField(String name) {
		ComplexFieldDesc cfd = getComplexFieldDesc(name);
		if (cfd == null) {
			cfd = new ComplexFieldDesc(name);
			complexFields.put(name, cfd);
		}
		return cfd;
	}

	/** Get the names of all the complex fields in our index */
	public Collection<String> getComplexFields() {
		return complexFields.keySet();
	}

	/** Get the description of one complex field */
	public ComplexFieldDesc getComplexFieldDesc(String fieldName) {
		return complexFields.get(fieldName);
	}

	/** Get the names of all the metadata fields in our index */
	public Collection<String> getMetadataFields(String fieldName) {
		return metadataFields.keySet();
	}

	/** Get the type of one metadata field */
	public IndexMetadata.FieldType getMetadataType(String fieldName) {
		return metadataFields.get(fieldName);
	}

}