package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.query.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:table-cluster-spb.xml" })
public class TableIndexDAOImplTest {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration config;
	// This is not a bean
	TableIndexDAO tableIndexDAO;
	
	String tableId;
	
	@Before
	public void before(){
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		tableId = "syn123";
		// First get a connection for this table
		tableIndexDAO = tableConnectionFactory.getConnection(tableId);
	}
	
	@After
	public void after(){
		// Drop the table
		if(tableId != null && tableIndexDAO != null){
			tableIndexDAO.deleteTable(tableId);
			tableIndexDAO.deleteStatusTable(tableId);
		}
	}
	
	@Test
	public void testGetCurrentTableColumnsDoesNotExist(){
		// There should not be any columns for this table as it does not exist
		List<String> names = tableIndexDAO.getCurrentTableColumns(tableId);
		assertEquals(null, names);
	}
	
	@Test
	public void testCRUD(){
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		// Create the table
		boolean updated = tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		assertTrue("The table should not have existed so update should be true",updated);
		updated = tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		assertFalse("The table already existed in that state so it should not have been updated",updated);
		// Now we should be able to see the columns that were created
		List<String> names = tableIndexDAO.getCurrentTableColumns(tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now remove a column and update the table
		allTypes.remove(0);
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now we should be able to see the columns that were created
		names = tableIndexDAO.getCurrentTableColumns(tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now add a column
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setId("9");
		allTypes.add(cm);
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now we should be able to see the columns that were created
		names = tableIndexDAO.getCurrentTableColumns(tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now delete the table
		assertTrue(tableIndexDAO.deleteTable(tableId));
		names = tableIndexDAO.getCurrentTableColumns(tableId);
		assertEquals(null, names);
	}
	
	@Test
	public void testCreateOrUpdateRows(){
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 5);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(allTypes));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Create the table
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		List<Map<String, Object>> result = tableIndexDAO.getConnection().queryForList("SELECT * FROM "+SQLUtils.getTableNameForId("syn123"));
		assertNotNull(result);
		assertEquals(5, result.size());
		// Row zero
		Map<String, Object> row = result.get(0);
		assertEquals(100l, row.get(ROW_ID));
		assertEquals(4000l, row.get("C4"));
		// row four
		row = result.get(4);
		assertEquals(104l, row.get(ROW_ID));
		assertEquals(16.76, row.get("C1"));
		assertEquals(4004l, row.get("C4"));
		
		// We should be able to update all of the rows
		rows.get(4).setValues(Arrays.asList("update", "99.99", "3", "false", "123", "123"));
		rows.get(4).setVersionNumber(5L);
		// This should not fail
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// Check the update
		result = tableIndexDAO.getConnection().queryForList("SELECT * FROM "+SQLUtils.getTableNameForId("syn123"));
		assertNotNull(result);
		assertEquals(5, result.size());
		// row four
		row = result.get(4);
		// Check all values on the updated row.
		assertEquals(104l, row.get(ROW_ID));
		assertEquals(5L, row.get(ROW_VERSION));
		assertEquals("update", row.get("C0"));
		assertEquals(99.99, row.get("C1"));
		assertEquals(3L, row.get("C2"));
		assertEquals(Boolean.FALSE, row.get("C3"));
		assertEquals(123L, row.get("C4"));
	}
	
	@Test
	public void testGetRowCountForTable(){
		// Before the table exists the max version should be null
		Long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals("The row count should be null when the table does not exist",null, count);
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// the table now exists
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals("The row count should be 0 when the table is empty", new Long(0), count);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(allTypes));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// Check again
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(new Long(rows.size()), count);
	}
	
	@Test
	public void testGetMaxVersionForTable(){
		// Before the table exists the max version should be -1L
		Long maxVersion = tableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(-1L, maxVersion.longValue());

		// Create the table
		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 2L);

		maxVersion = tableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(2L, maxVersion.longValue());

		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 4L);

		maxVersion = tableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(4L, maxVersion.longValue());

		tableIndexDAO.deleteStatusTable(tableId);
		maxVersion = tableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(-1L, maxVersion.longValue());
	}
	
	@Test
	public void testSimpleQuery() throws ParseException{
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<String> headers = TableModelUtils.getHeaders(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// This is our query
		SqlQuery query = new SqlQuery("select * from "+tableId, allTypes);
		// Now query for the results
		RowSet results = tableIndexDAO.query(query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(2, results.getRows().size());
		// the first row
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(100), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string0", "3.12", "3000", "false", "4000", "5000");
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList("string1", "6.53", "3001", "true", "4001", "5001");
		assertEquals(expectedValues, row.getValues());
		

	}
	
	@Test
	public void testSimpleQueryWithDeletedRows() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		Row deletion = new Row();
		rows.add(deletion);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<String> headers = TableModelUtils.getHeaders(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setMaximumUpdateId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);

		// now delete the second and fourth row
		set.getRows().remove(0);
		set.getRows().remove(1);
		set.getRows().get(0).getValues().clear();
		set.getRows().get(1).getValues().clear();
		range.setVersionNumber(4L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// This is our query
		SqlQuery query = new SqlQuery("select * from " + tableId, allTypes);
		// Now query for the results
		RowSet results = tableIndexDAO.query(query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(2, results.getRows().size());
		assertEquals(100L, results.getRows().get(0).getRowId().longValue());
		assertEquals(102L, results.getRows().get(1).getRowId().longValue());
	}

	@Test
	public void testNullQuery() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createNullRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<String> headers = TableModelUtils.getHeaders(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// Now query for the results
		SqlQuery query = new SqlQuery("select * from " + tableId, allTypes);
		RowSet results = tableIndexDAO.query(query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(2, results.getRows().size());
		// the first row
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(100), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList(null, null, null, null, null, null);
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList(null, null, null, null, null, null);
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testQueryAggregate() throws ParseException{
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<String> headers = TableModelUtils.getHeaders(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// Now a count query
		SqlQuery query = new SqlQuery("select count(*) from "+tableId, allTypes);
		// Now query for the results
		RowSet results = tableIndexDAO.query(query);
		assertNotNull(results);
		List<String> expectedHeaders = Arrays.asList("COUNT(*)");
		assertEquals(expectedHeaders, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals("RowId should be null for an aggregate function.",null, row.getRowId());
		assertEquals("RowVersion should be null for an aggregate function", null, row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("2");
		assertEquals(expectedValues, row.getValues());
	}
	
	@Test
	public void testQueryAllParts() throws ParseException{
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.STRING);
		foo.setName("foo");
		foo.setId("111");
		foo.setMaximumSize(10L);
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.LONG);
		bar.setId("222");
		bar.setName("bar");
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		schema.add(foo);
		schema.add(bar);
		// Create the table.
		tableIndexDAO.createOrUpdateTable(schema, tableId);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<String> headers = TableModelUtils.getHeaders(schema);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, schema);
		// Now create the query
		SqlQuery query = new SqlQuery("select foo, bar from "+tableId+" where foo is not null group by foo order by bar desc limit 1 offset 0", schema);
		// Now query for the results
		RowSet results = tableIndexDAO.query(query);
		assertNotNull(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(199), row.getRowId());
		assertEquals(new Long(4), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string99", "3099");
		assertEquals(expectedValues, row.getValues());
	}
	
	@Test
	public void testQueryRowIdAndRowVersion() throws ParseException{
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.STRING);
		foo.setName("foo");
		foo.setId("111");
		foo.setMaximumSize(10L);
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.LONG);
		bar.setId("222");
		bar.setName("bar");
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		schema.add(foo);
		schema.add(bar);
		// Create the table.
		tableIndexDAO.createOrUpdateTable(schema, tableId);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<String> headers = TableModelUtils.getHeaders(schema);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, schema);
		// Now create the query
		SqlQuery query = new SqlQuery("select * from "+tableId+" where ROW_ID = 104 AND Row_Version > 1 limit 1 offset 0", schema);
		// Now query for the results
		RowSet results = tableIndexDAO.query(query);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(104), row.getRowId());
		assertEquals(new Long(4), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string4", "3004");
		assertEquals(expectedValues, row.getValues());
	}
}
