package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * Modified subset of &ltqualified join&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * Omits the optional [ NATURAL ] and [ <join type> ]
 * and substitutes  <join specification> for <join condition>, which  <join specification> holds.
 *
 * Effectively:
 * <qualified join> ::= <table reference> JOIN <table reference> [ <join condition> ]
 */
public class QualifiedJoin extends SQLElement{
	TableReference tableReferenceLHS;// left side of join
	TableReference tableReferenceRHS;// right side of join
	JoinCondition joinCondition;// optional

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		tableReferenceLHS.toSql(builder, parameters);
		builder.append(" JOIN ");
		tableReferenceRHS.toSql(builder, parameters);
		if(joinCondition != null){
			builder.append(" ");
			joinCondition.toSql(builder, parameters);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, tableReferenceLHS);
		checkElement(elements, type, tableReferenceRHS);
		checkElement(elements, type, joinCondition);
	}

}
