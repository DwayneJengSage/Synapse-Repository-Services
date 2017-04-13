package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserGroupDAOImplTest {
	
	
	@Autowired
	private UserGroupDAO userGroupDAO;
		
	private List<String> groupsToDelete;
	

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		for (String todelete: groupsToDelete) {
			userGroupDAO.delete(todelete);
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		// Give it an ID
		String startingId = "123";
		group.setId(""+startingId);
		long initialCount = userGroupDAO.getCount();
		String groupId = userGroupDAO.create(group).toString();
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		assertFalse("A new ID should have been issued to the principal",groupId.equals(startingId));
		UserGroup clone = userGroupDAO.get(Long.parseLong(groupId));
		assertEquals(groupId, clone.getId());
		assertEquals(group.getIsIndividual(), clone.getIsIndividual());
		assertEquals(1+initialCount, userGroupDAO.getCount());
	}
	
	@Test (expected=NotFoundException.class)
	public void testIsIndividualDoesNotExist(){
		userGroupDAO.isIndividual(-1L);
	}
	
	@Test
	public void testIsIndividualTrue() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(true);
		Long principalId = userGroupDAO.create(group);
		assertNotNull(principalId);
		groupsToDelete.add(principalId.toString());
		assertTrue(userGroupDAO.isIndividual(principalId));
	}
	
	@Test
	public void testIsIndividualFalse() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		Long principalId = userGroupDAO.create(group);
		assertNotNull(principalId);
		groupsToDelete.add(principalId.toString());
		assertFalse(userGroupDAO.isIndividual(principalId));
	}


	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<BootstrapPrincipal> boots = this.userGroupDAO.getBootstrapPrincipals();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		for(BootstrapPrincipal bootUg: boots){
			assertTrue(userGroupDAO.doesIdExist(bootUg.getId()));
			UserGroup ug = userGroupDAO.get(bootUg.getId());
			assertEquals(bootUg.getId().toString(), ug.getId());
		}
	}

}
