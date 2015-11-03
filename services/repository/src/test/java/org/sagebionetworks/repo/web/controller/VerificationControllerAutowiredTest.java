package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simplistic test to see if things are wired up correctly
 * All messages are retrieved oldest first
 */
public class VerificationControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private FileHandleDao fileMetadataDao;


	private Long adminUserId;
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	
	private static final String FIRST_NAME = "fname";
	private static final String LAST_NAME = "lname";
	private static final String COMPANY = "company";
	private static final String LOCATION = "location";
	private static final String ORCID = "http://www.orcid.org/0000-1111-2222-3333";
	String NOTIFICATION_UNSUBSCRIBE_ENDPOINT = "https://synapse.org/#notificationUnsubscribeEndpoint:";
	
	private List<String> emails;
	
	private VerificationSubmission verificationToDelete;
	private ExternalFileHandle fileHandleToDelete;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		emails = Arrays.asList(user.getEmail());
		user.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.getUserInfo(userManager.createUser(user));

		userManager.bindAlias(ORCID, AliasType.USER_ORCID, userInfo.getId());
		
		UserProfile userProfile = userProfileManager.getUserProfile(userInfo.getId().toString());
		assertNotNull(userProfile.getEmails());
		userProfile.setFirstName(FIRST_NAME);
		userProfile.setLastName(LAST_NAME);
		userProfile.setCompany(COMPANY);
		userProfile.setLocation(LOCATION);
		userProfileManager.updateUserProfile(userInfo, userProfile);
		
		
		fileHandleToDelete = new ExternalFileHandle();
		fileHandleToDelete.setCreatedBy(userInfo.getId().toString());
		fileHandleToDelete.setCreatedOn(new Date());
		fileHandleToDelete.setEtag("etag");
		fileHandleToDelete.setFileName("foo.bar");
		fileHandleToDelete.setContentMd5("handleOneContentMd5");
		fileHandleToDelete.setExternalURL("http://foo.bar/baz.txt");
		fileHandleToDelete = fileMetadataDao.createFile(fileHandleToDelete);
	}

	@After
	public void after() throws Exception {
		servletTestHelper.deleteVerificationSubmission(dispatchServlet, 
				userInfo.getId(), Long.parseLong(verificationToDelete.getId()));
		verificationToDelete = null;
		 
	}

	@Test
	public void testRoundTrip() throws Exception {
		VerificationSubmission vs = new VerificationSubmission();
		vs.setFirstName(FIRST_NAME);
		vs.setLastName(LAST_NAME);
		vs.setCompany(COMPANY);
		vs.setLocation(LOCATION);
		vs.setEmails(emails);
		String fileHandleId = fileHandleToDelete.getId();
		vs.setFiles(Collections.singletonList(fileHandleId));
		vs.setOrcid(ORCID);
		vs = servletTestHelper.createVerificationSubmission(dispatchServlet, 
				userInfo.getId(), vs, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		assertNotNull(vs.getId());
		
		VerificationPagedResults list = servletTestHelper.listVerificationSubmissions(
				dispatchServlet, VerificationStateEnum.SUBMITTED, 
				userInfo.getId(), 10L, 0L, adminUserId);
		assertEquals(1L, list.getTotalNumberOfResults().longValue());
		assertEquals(1, list.getResults().size());
		
		VerificationState newState = new VerificationState();
		newState.setState(VerificationStateEnum.APPROVED);
		servletTestHelper.updateVerificationState(dispatchServlet, adminUserId, Long.parseLong(vs.getId()),
				newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setFileHandleId(fileHandleId);
		fha.setAssociateObjectType(FileHandleAssociateType.VerificationSubmission);
		fha.setAssociateObjectId(vs.getId());
		String url = servletTestHelper.getFileHandleUrl(dispatchServlet, fha, adminUserId);
		assertEquals(fileHandleToDelete.getExternalURL(), url);
	}
}
