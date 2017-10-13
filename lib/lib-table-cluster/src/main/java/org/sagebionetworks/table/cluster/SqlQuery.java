package org.sagebionetworks.table.cluster;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Represents a SQL query for a table.
 * 
 * @author John
 *
 */
public class SqlQuery {
	
	/**
	 * The input SQL is parsed into this object model.
	 * 
	 */
	QuerySpecification model;
	
	/**
	 * The model transformed to execute against the actual table.
	 */
	QuerySpecification transformedModel;
	
	/**
	 * The full list of all of the columns of this table
	 */
	List<ColumnModel> tableSchema;
	
	/**
	 * This map will contain all of the bind variable values for the translated query.
	 */
	Map<String, Object> parameters;
	
	/**
	 * The map of column names to column models.
	 */
	LinkedHashMap<String, ColumnModel> columnNameToModelMap;
	
	/**
	 * The translated SQL.
	 */
	String outputSQL;
	
	/**
	 * The Id of the table.
	 */
	String tableId;
	
	/**
	 * The maximum size of each query result row returned by this query.
	 */
	int maxRowSizeBytes;
	
	/**
	 * The maximum number of rows per page for the given query
	 */
	Long maxRowsPerPage;
	
	/**
	 * Does this query include ROW_ID and ROW_VERSION?
	 */
	boolean includesRowIdAndVersion;
	
	/**
	 * Should the query results include the row's etag?
	 * Note: This is true for view queries.
	 */
	boolean includeRowEtag;
	
	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 */
	boolean isAggregatedResult;
	
	/**
	 * The list of all columns referenced in the select column.
	 */
	List<SelectColumn> selectColumns;
	
	Long overrideOffset;
	Long overrideLimit;
	Long maxBytesPerPage;
	
	
	/**
	 * Create a new SQLQuery from an input SQL string and mapping of the column names to column IDs.
	 * 
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(String sql, List<ColumnModel> tableSchema) throws ParseException {
		this(sql, tableSchema, false);
	}
	
	/**
	 * 
	 * @param sql
	 * @param tableSchema
	 * @param includeEtag
	 * @throws ParseException
	 */
	public SqlQuery(String sql, List<ColumnModel> tableSchema, boolean includeEtag) throws ParseException {
		if(sql == null) throw new IllegalArgumentException("The input SQL cannot be null");
		QuerySpecification parsedQuery = TableQueryParser.parserQuery(sql);
		Long overrideOffset = null;
		Long overrideLimit = null;
		Long maxBytesPerPage = null;
		init(parsedQuery, tableSchema, overrideOffset, overrideLimit, maxBytesPerPage, includeEtag);
	}
	
	/**
	 * Create a query with a parsed model.
	 * 
	 * @param model
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(QuerySpecification model, List<ColumnModel> tableSchema, String tableId) {
		if (model == null)
			throw new IllegalArgumentException("The input model cannot be null");
		Long overrideOffset = null;
		Long overrideLimit = null;
		Long maxBytesPerPage = null;
		boolean includeEtag = false;
		init(model, tableSchema, overrideOffset, overrideLimit, maxBytesPerPage, includeEtag);
	}
	
	/**
	 * Create a new
	 * @param model
	 * @param tableSchema
	 * @param tableId
	 * @param overrideOffset Optional parameter to override the offset in the passed SQL.
	 * @param overrideLimit Optional parameter to override the limit in the passed SQL.
	 * @param maxBytesPerPage Optional parameter to limit the number or rows returned by the query.
	 */
	public SqlQuery(QuerySpecification model, List<ColumnModel> tableSchema,
			Long overrideOffset, Long overrideLimit,
			Long maxBytesPerPage) {
		if (model == null)
			throw new IllegalArgumentException("The input model cannot be null");
		boolean includeEtag = false;
		init(model, tableSchema, overrideOffset, overrideLimit, maxBytesPerPage, includeEtag);
	}
	
	/**
	 * Create a new query as a copy of the passed query model.
	 * @param model
	 * @param toCopy
	 */
	public SqlQuery(QuerySpecification model, SqlQuery toCopy) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(toCopy, "toCopy");
		init(model, toCopy.getTableSchema(), toCopy.overrideOffset, toCopy.overrideLimit, toCopy.maxBytesPerPage, toCopy.includeRowEtag());
	}
	
	/**
	 * Copy with override to include the etag.
	 * @param model
	 * @param toCopy
	 * @param includeRowEtag
	 */
	public SqlQuery(QuerySpecification model, SqlQuery toCopy, boolean includeRowEtag) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(toCopy, "toCopy");
		init(model, toCopy.getTableSchema(), toCopy.overrideOffset, toCopy.overrideLimit, toCopy.maxBytesPerPage, includeRowEtag);
	}

	/**
	 * @param tableId
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public void init(QuerySpecification parsedModel,
			List<ColumnModel> tableSchema, Long overrideOffset,
			Long overrideLimit, Long maxBytesPerPage, boolean includeRowEtag) {
		ValidateArgument.required(tableSchema, "TableSchema");
		if(tableSchema.isEmpty()){
			throw new IllegalArgumentException("Table schema cannot be empty");
		}
		this.tableSchema = tableSchema;
		this.model = parsedModel;
		this.tableId = parsedModel.getTableName();
		this.overrideOffset = overrideOffset;
		this.overrideLimit = overrideLimit;
		this.maxBytesPerPage = maxBytesPerPage;
		this.includeRowEtag = includeRowEtag;

		// This map will contain all of the 
		this.parameters = new HashMap<String, Object>();	
		this.columnNameToModelMap = TableModelUtils.createColumnNameToModelMap(tableSchema);
		// SELECT * is replaced with a select including each column in the schema.
		if (BooleanUtils.isTrue(this.model.getSelectList().getAsterisk())) {
			SelectList expandedSelectList = SQLTranslatorUtils.createSelectListFromSchema(tableSchema);
			this.model.replaceSelectList(expandedSelectList);
		}
		// Track if this is an aggregate query.
		this.isAggregatedResult = model.hasAnyAggregateElements();
		// Build headers that describe how the client should read the results of this query.
		this.selectColumns = SQLTranslatorUtils.getSelectColumns(this.model.getSelectList(), columnNameToModelMap, this.isAggregatedResult);
		// Maximum row size is a function of both the select clause and schema.
		this.maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(selectColumns, columnNameToModelMap);
		if(maxBytesPerPage != null){
			this.maxRowsPerPage =  Math.max(1, maxBytesPerPage / this.maxRowSizeBytes);
		}
		// paginated model includes all overrides and max rows per page.
		QuerySpecification paginatedModel = SqlElementUntils.overridePagination(model, overrideOffset, overrideLimit, maxRowsPerPage);

		// Create a copy of the paginated model.
		try {
			transformedModel = new TableQueryParser(paginatedModel.toSql()).querySpecification();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		// Add ROW_ID and ROW_VERSION only if all columns have an Id.
		if (SQLTranslatorUtils.doAllSelectMatchSchema(selectColumns)) {
			// we need to add the row count and row version columns
			SelectList expandedSelectList = SQLTranslatorUtils.addMetadataColumnsToSelect(this.transformedModel.getSelectList(), this.includeRowEtag);
			transformedModel.replaceSelectList(expandedSelectList);
			this.includesRowIdAndVersion = true;
		}else{
			this.includesRowIdAndVersion = false;
		}
		SQLTranslatorUtils.translateModel(transformedModel, parameters, columnNameToModelMap);
		this.outputSQL = transformedModel.toSql();
	}
	
	/**
	 * Does this query include ROW_ID and ROW_VERSION
	 * 
	 * @return
	 */
	public boolean includesRowIdAndVersion(){
		return this.includesRowIdAndVersion;
	}
	
	/**
	 * Does this query include ROW_ETAG
	 * @return
	 */
	public boolean includeRowEtag(){
		return this.includeRowEtag;
	}

	/**
	 * The input SQL was parsed into this model object.
	 * 
	 * @return
	 */
	public QuerySpecification getModel() {
		return model;
	}


	/**
	 * This map contains the values of all bind variables referenced in the translated output SQL.
	 * @return
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}


	/**
	 * The column name to column ID mapping.
	 * @return
	 */
	public Map<String, ColumnModel> getColumnNameToModelMap() {
		return columnNameToModelMap;
	}


	/**
	 * The translated output SQL.
	 * @return
	 */
	public String getOutputSQL() {
		return outputSQL;
	}

	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 * @return
	 */
	public boolean isAggregatedResult() {
		return isAggregatedResult;
	}

	/**
	 * The ID of the table.
	 * @return
	 */
	public String getTableId() {
		return tableId;
	}

	/**
	 * The list of column models from the select clause.
	 * @return
	 */
	public List<SelectColumn> getSelectColumns() {
		return selectColumns;
	}

	/**
	 * All of the Columns of the table.
	 * @return
	 */
	public List<ColumnModel> getTableSchema() {
		return tableSchema;
	}

	/**
	 * The maximum size of each query result row returned by this query.
	 * @return
	 */
	public int getMaxRowSizeBytes() {
		return maxRowSizeBytes;
	}

	/**
	 * The query model that has been transformed to execute against the actual table index.
	 * @return
	 */
	public QuerySpecification getTransformedModel() {
		return transformedModel;
	}

	/**
	 * 
	 * @return
	 */
	public Long getMaxRowsPerPage() {
		return maxRowsPerPage;
	}
	
	
}
