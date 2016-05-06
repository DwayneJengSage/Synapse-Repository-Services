package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltlike predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class LikePredicate extends SQLElement {
	
	ColumnReference columnReferenceLHS;
	Boolean not;
	Pattern pattern;
	EscapeCharacter escapeCharacter;
	
	public LikePredicate(ColumnReference columnReferenceLHS, Boolean not, Pattern pattern, EscapeCharacter escapeCharacter) {
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}
	
	public Boolean getNot() {
		return not;
	}
	public Pattern getPattern() {
		return pattern;
	}
	public EscapeCharacter getEscapeCharacter() {
		return escapeCharacter;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	public void visit(Visitor visitor) {
		visit(columnReferenceLHS, visitor);
		visit(pattern, visitor);
		if (escapeCharacter != null) {
			visit(escapeCharacter, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visit(columnReferenceLHS, visitor);
		if (not != null) {
			visitor.append(" NOT");
		}
		visitor.append(" LIKE ");
		visit(pattern, visitor);
		if (escapeCharacter != null) {
			visitor.append(" ESCAPE ");
			visit(escapeCharacter, visitor);
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		columnReferenceLHS.toSql(builder);
		if (not != null) {
			builder.append(" NOT");
		}
		builder.append(" LIKE ");
		pattern.toSql(builder);
		if (escapeCharacter != null) {
			builder.append(" ESCAPE ");
			escapeCharacter.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReferenceLHS);
		checkElement(elements, type, pattern);
		checkElement(elements, type, escapeCharacter);
	}
}
