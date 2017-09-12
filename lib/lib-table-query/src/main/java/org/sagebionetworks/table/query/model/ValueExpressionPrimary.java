package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltvalue expression primary&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class ValueExpressionPrimary extends SQLElement implements HasReferencedColumn {

	ValueSpecifictation valueSpecifictation;
	ColumnReference columnReference;
	SetFunctionSpecification setFunctionSpecification;
	
	public ValueExpressionPrimary(ValueSpecifictation valueSpecifictation) {
		this.valueSpecifictation = valueSpecifictation;
	}
	
	public ValueExpressionPrimary(ColumnReference columnReference) {
		this.columnReference = columnReference;
	}

	public ValueExpressionPrimary(SetFunctionSpecification setFunctionSpecification) {
		this.setFunctionSpecification = setFunctionSpecification;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}
	public SetFunctionSpecification getSetFunctionSpecification() {
		return setFunctionSpecification;
	}

	@Override
	public void toSql(StringBuilder builder) {
		// only one element at a time will be no null
		if (valueSpecifictation != null) {
			valueSpecifictation.toSql(builder);
		} else if (columnReference != null) {
			columnReference.toSql(builder);
		} else {
			setFunctionSpecification.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, valueSpecifictation);
		checkElement(elements, type, columnReference);
		checkElement(elements, type, setFunctionSpecification);
	}

	@Override
	public HasQuoteValue getReferencedColumn() {
		// Handle functions first
		if(setFunctionSpecification != null){
			if(setFunctionSpecification.getCountAsterisk() != null){
				// count(*) does not reference a column
				return null;
			}else{
				// first unquoted value starting at the value expression.
				return setFunctionSpecification.getValueExpression().getFirstElementOfType(HasQuoteValue.class);
			}
		}else{
			// This is not a function so get the first unquoted.
			return this.getFirstElementOfType(HasQuoteValue.class);
		}
	}

	@Override
	public boolean isReferenceInFunction() {
		if(setFunctionSpecification != null && setFunctionSpecification.getCountAsterisk() != null){
			throw new IllegalArgumentException("COUNT(*) does not have a column reference");
		}
		return setFunctionSpecification != null;
	}

	
	
}
