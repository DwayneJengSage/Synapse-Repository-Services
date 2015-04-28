package org.sagebionetworks.repo.web.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simplistic test to see if things are wired up correctly
 * All messages are retrieved oldest first
 */
public class TeamControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	public UserManager userManager;

	private Long adminUserId;
	
	private static final String TEAM_NAME = "MIS_CONTRL_AW_TEST";
	private Team teamToDelete;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// create a Team
		Team team = new Team();
		team.setName(TEAM_NAME);
		teamToDelete = servletTestHelper.createTeam(dispatchServlet, adminUserId, team);

	}

	@After
	public void after() throws Exception {
		servletTestHelper.deleteTeam(dispatchServlet, adminUserId, teamToDelete);
		 teamToDelete = null;
	}

	@Test
	public void testListTeams() throws Exception {
		IdList idList = new IdList();
		idList.setList(Collections.EMPTY_LIST);
		List<Team> teams = servletTestHelper.listTeams(dispatchServlet, idList);
	}
}
