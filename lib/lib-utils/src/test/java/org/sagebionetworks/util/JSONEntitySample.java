package org.sagebionetworks.util;


import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class JSONEntitySample implements JSONEntity {

	public JSONEntitySample() {}

	public String getHmac() {
		return hmac;
	}
	public void setHmac(String hmac) {
		this.hmac = hmac;
	}
	public String getStringField() {
		return stringField;
	}
	public void setStringField(String stringField) {
		this.stringField = stringField;
	}
	private String hmac;
	private String stringField;


	/**
	 * @see JSONEntity#initializeFromJSONObject(JSONObjectAdapter)
	 * @see JSONEntity#writeToJSONObject(JSONObjectAdapter)
	 * 
	 * @param adapter
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter)
			throws JSONObjectAdapterException
	{
		if (adapter == null) {
			throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
		}
		if (!adapter.isNull("hmac")) {
			hmac = adapter.getString("hmac");
		} else {
			hmac = null;
		}
		if (!adapter.isNull("stringField")) {
			stringField = adapter.getString("stringField");
		} else {
			stringField = null;
		}
		return adapter;
	}

	/**
	 * @see JSONEntity#initializeFromJSONObject(JSONObjectAdapter)
	 * @see JSONEntity#writeToJSONObject(JSONObjectAdapter)
	 * 
	 * @param adapter
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter)
			throws JSONObjectAdapterException
	{
		if (adapter == null) {
			throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
		}
		if (hmac!= null) {
			adapter.put("hmac", hmac);
		}
		if (stringField!= null) {
			adapter.put("stringField", stringField);
		}
		return adapter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hmac == null) ? 0 : hmac.hashCode());
		result = prime * result
				+ ((stringField == null) ? 0 : stringField.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JSONEntitySample other = (JSONEntitySample) obj;
		if (hmac == null) {
			if (other.hmac != null)
				return false;
		} else if (!hmac.equals(other.hmac))
			return false;
		if (stringField == null) {
			if (other.stringField != null)
				return false;
		} else if (!stringField.equals(other.stringField))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JSONEntitySample [hmac=" + hmac + ", stringField="
				+ stringField + "]";
	}
	
	
}

