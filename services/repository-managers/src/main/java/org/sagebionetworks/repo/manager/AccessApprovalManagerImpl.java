package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessApprovalInfo;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.SelfSignAccessRequirementInterface;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class AccessApprovalManagerImpl implements AccessApprovalManager {
	public static final Long DEFAULT_LIMIT = 50L;
	public static final Long MAX_LIMIT = 50L;
	public static final Long DEFAULT_OFFSET = 0L;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private NodeDAO nodeDao;

	public static void populateCreationFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setState(ApprovalState.APPROVED);
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		populateModifiedFields(userInfo, a);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}

	@Override
	public AccessApproval getAccessApproval(UserInfo userInfo, String approvalId) 
			throws DatastoreException, NotFoundException {
		return accessApprovalDAO.get(approvalId);
	}

	@WriteTransaction
	@Override
	public AccessApproval createAccessApproval(UserInfo userInfo, AccessApproval accessApproval) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessApproval, "accessApproval");
		ValidateArgument.required(accessApproval.getRequirementId(), "accessRequirementId");
		AccessRequirement ar = accessRequirementDAO.get(accessApproval.getRequirementId().toString());

		ValidateArgument.requirement(!(ar instanceof LockAccessRequirement)
				&& !(ar instanceof PostMessageContentAccessRequirement), "Cannot apply an approval to a "+ar.getConcreteType());
		if (ar instanceof SelfSignAccessRequirementInterface) {
			accessApproval.setAccessorId(userInfo.getId().toString());
		} else if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("User is not an ACT Member.");
		}

		ValidateArgument.required(accessApproval.getAccessorId(), "accessorId");
		ValidateArgument.requirement(
				!BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString().equals(accessApproval.getAccessorId()),
				"Cannot create an AccessApproval for anonymous user.");
		if (ar instanceof HasAccessorRequirement) {
			authorizationManager.validateHasAccessorRequirement((HasAccessorRequirement) ar,
					Sets.newHashSet(accessApproval.getAccessorId()));
		}
		if (accessApproval.getRequirementVersion() == null) {
			accessApproval.setRequirementVersion(ar.getVersionNumber());
		}
		if (accessApproval.getSubmitterId() == null) {
			accessApproval.setSubmitterId(accessApproval.getAccessorId());
		}
		populateCreationFields(userInfo, accessApproval);
		return accessApprovalDAO.create(accessApproval);
	}

	@WriteTransaction
	@Override
	public void revokeAccessApprovals(UserInfo userInfo, String accessRequirementId, String accessorId)
			throws UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member may delete access approvals.");
		}
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessRequirementId);
		ValidateArgument.requirement(accessRequirement.getConcreteType().equals(ACTAccessRequirement.class.getName()),
				"Do not support access approval deletion for access requirement type: "+accessRequirement.getConcreteType());
		accessApprovalDAO.revokeAll(accessRequirementId, accessorId, userInfo.getId().toString());
	}

	@Override
	public AccessorGroupResponse listAccessorGroup(UserInfo userInfo, AccessorGroupRequest request){
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<AccessorGroup> groups = accessApprovalDAO.listAccessorGroup(
				request.getAccessRequirementId(), request.getSubmitterId(), 
				request.getExpireBefore(), nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());
		AccessorGroupResponse response = new AccessorGroupResponse();
		response.setResults(groups);
		response.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(groups));
		return response;
	}

	@WriteTransaction
	@Override
	public void revokeGroup(UserInfo userInfo, AccessorGroupRevokeRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "requirementId");
		ValidateArgument.required(request.getSubmitterId(), "submitterId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		accessApprovalDAO.revokeGroup(request.getAccessRequirementId(), request.getSubmitterId(), userInfo.getId().toString());
	}

	@Override
	public BatchAccessApprovalInfoResponse getAccessApprovalInfo(UserInfo userInfo, BatchAccessApprovalInfoRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getUserId(), "BatchAccessApprovalInfoRequest.userId");
		ValidateArgument.required(request.getAccessRequirementIds(), "BatchAccessApprovalInfoRequest.accessRequirementIds");
		BatchAccessApprovalInfoResponse response = new BatchAccessApprovalInfoResponse();
		List<AccessApprovalInfo> results = new LinkedList<AccessApprovalInfo>();
		response.setResults(results);
		if (!request.getAccessRequirementIds().isEmpty()) {
			Set<String> requirementsUserHasApproval = accessApprovalDAO.getRequirementsUserHasApprovals(request.getUserId(), request.getAccessRequirementIds());
			for (String requirementId : request.getAccessRequirementIds()) {
				AccessApprovalInfo info = new AccessApprovalInfo();
				info.setUserId(request.getUserId());
				info.setAccessRequirementId(requirementId);
				info.setHasAccessApproval(requirementsUserHasApproval.contains(requirementId));
				results.add(info);
			}
		}
		return response;
	}
}
