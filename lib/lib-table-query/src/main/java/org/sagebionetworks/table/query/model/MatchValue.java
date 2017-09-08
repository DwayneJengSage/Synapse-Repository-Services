package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltmatch value&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class MatchValue extends SQLElement {
	
	CharacterValueExpression characterValueExpression;

	
	public MatchValue(CharacterValueExpression characterValueExpression) {
		this.characterValueExpression = characterValueExpression;
	}

	public CharacterValueExpression getCharacterValueExpression() {
		return characterValueExpression;
	}

	@Override
	public void toSql(StringBuilder builder) {
		characterValueExpression.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, characterValueExpression);
	}
}