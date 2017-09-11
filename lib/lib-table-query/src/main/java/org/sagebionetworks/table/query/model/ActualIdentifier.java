package org.sagebionetworks.table.query.model;

import java.util.List;


/**
 * This matches &ltactual identifier&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class ActualIdentifier extends SQLElement implements HasQuoteValue {
	
	private String regularIdentifier;
	private String delimitedIdentifier;
	public ActualIdentifier(String regularIdentifier, String delimitedIdentifier) {
		if(regularIdentifier != null && delimitedIdentifier != null) throw new IllegalArgumentException("An actual identifier must be either a regular-identifier or a delimited-identifier but not both"); 
		this.regularIdentifier = regularIdentifier;
		this.delimitedIdentifier = delimitedIdentifier;
	}
	public String getRegularIdentifier() {
		return regularIdentifier;
	}
	public String getDelimitedIdentifier() {
		return delimitedIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(regularIdentifier != null){
			// Regular identifiers can be written without modification.
			builder.append(regularIdentifier);
		}else{
			// Delimited identifiers must be within double quotes.
			// And double quote characters must be escaped with another double quote.
			builder.append("\"");
			builder.append(delimitedIdentifier.replaceAll("\"", "\"\""));
			builder.append("\"");
		}
	}
	@Override
	public String getValueWithoutQuotes() {
		if (regularIdentifier != null) {
			return regularIdentifier;
		} else {
			return delimitedIdentifier;
		}
	}
	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this element does not contain any SQLElements
	}
	
	@Override
	public boolean isSurrounedeWithQuotes() {
		return delimitedIdentifier != null;
	}
	@Override
	public void replaceUnquoted(String newValue) {
		regularIdentifier = newValue;
		delimitedIdentifier = null;
	}
}
