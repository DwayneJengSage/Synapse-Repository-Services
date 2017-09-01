package org.sagebionetworks.repo.manager.principal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.principal.*;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.util.SerializationUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/**
 * Basic implementation of the PrincipalManager.
 * @author John
 *
 */
public class PrincipalManagerImpl implements PrincipalManager {
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private SynapseEmailService sesClient;
	
	@Autowired
	private UserProfileDAO userProfileDAO;

	public static final String PARAMETER_CHARSET = Charset.forName("utf-8").name();
	public static final String EMAIL_VALIDATION_FIRST_NAME_PARAM = "firstname";
	public static final String EMAIL_VALIDATION_DOMAIN_PARAM = "domain";
	public static final String EMAIL_VALIDATION_LAST_NAME_PARAM = "lastname";
	public static final String EMAIL_VALIDATION_USER_ID_PARAM = "userid";
	public static final String EMAIL_VALIDATION_EMAIL_PARAM = "email";
	public static final String EMAIL_VALIDATION_TIME_STAMP_PARAM = "timestamp";
	public static final String EMAIL_VALIDATION_SIGNATURE_PARAM = "mac";
	public static final String DATE_FORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	public static final String AMPERSAND = "&";
	public static final String EQUALS = "=";
	public static final long EMAIL_VALIDATION_TIME_LIMIT_MILLIS = 24*3600*1000L; // 24 hours as milliseconds

	@Override
	public boolean isAliasAvailable(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		return this.principalAliasDAO.isAliasAvailable(alias);
	}

	@Override
	public boolean isAliasValid(String alias, AliasType type) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		// Check the value
		try {
			AliasEnum.valueOf(type.name()).validateAlias(alias);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@Deprecated
	public static String generateSignature(String payload) {
		try {
			byte[] secretKey = StackConfiguration.getEncryptionKey().getBytes(PARAMETER_CHARSET);
			byte[] signatureAsBytes = HMACUtils.generateHMACSHA1SignatureFromRawKey(payload, secretKey);
			return new String(signatureAsBytes, PARAMETER_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Deprecated
	public static String generateSignatureForNewAccount(String firstName, String lastName,
			String email, String timestamp, String domain) {
		return generateSignature(firstName+lastName+email+timestamp+domain);
	}

	@Deprecated
	// note, this assumes that first name, last name and email are valid in 'user'
	public static String createTokenForNewAccount(NewUser user, Date now) {
		try {
			StringBuilder sb = new StringBuilder();
			String urlEncodedFirstName = URLEncoder.encode(user.getFirstName(), PARAMETER_CHARSET);
			sb.append(EMAIL_VALIDATION_FIRST_NAME_PARAM+EQUALS+urlEncodedFirstName);
			String urlEncodedLastName = URLEncoder.encode(user.getLastName(), PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_LAST_NAME_PARAM+EQUALS+urlEncodedLastName);
			String urlEncodedEmail = URLEncoder.encode(user.getEmail(), PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_EMAIL_PARAM+EQUALS+urlEncodedEmail);
			DateFormat df = new SimpleDateFormat(DATE_FORMAT_ISO8601);
			String timestampString = df.format(now);
			String urlEncodedTimeStampString = URLEncoder.encode(timestampString, PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS+urlEncodedTimeStampString);
			String urlEncodedDomain = URLEncoder.encode("Synapse", PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_DOMAIN_PARAM+EQUALS+urlEncodedDomain);
			String mac = generateSignatureForNewAccount(
					urlEncodedFirstName, urlEncodedLastName, urlEncodedEmail,
					urlEncodedTimeStampString, urlEncodedDomain);
			sb.append(AMPERSAND+EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS+URLEncoder.encode(mac, PARAMETER_CHARSET));
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private String createAccountCreationToken(NewUser user, Date now) {
		AccountCreationToken accountCreationToken = new AccountCreationToken();
		accountCreationToken.setMembershipInvtnSignedToken(user.getMembershipInvtnSignedToken());
		EmailValidationSignedToken emailValidationSignedToken = new EmailValidationSignedToken();
		emailValidationSignedToken.setEmail(user.getEmail());
		emailValidationSignedToken.setCreatedOn(now);
		SignedTokenUtil.signToken(emailValidationSignedToken);
		accountCreationToken.setEmailValidationSignedToken(emailValidationSignedToken);
		return SerializationUtils.serializeAndHexEncode(accountCreationToken);
	}

	public static String validateAccountSetupInfo(AccountSetupInfo accountSetupInfo, Date now) {
		// Find out which kind of token is being used
		String token = accountSetupInfo.getEmailValidationToken();
		EmailValidationSignedToken signedToken = accountSetupInfo.getEmailValidationSignedToken();
		if (signedToken != null && token == null) {
			return validateEmailSignedToken(signedToken, new Date());
		} else if (token != null && signedToken == null) {
			return validateEmailToken(token, new Date());
		} else {
			throw new IllegalArgumentException("One and only one type of email validation token must be provided.");
		}
	}

	@Deprecated
	// returns the validated email address
	// note, we pass the current time as a parameter to facilitate testing
	public static String validateEmailToken(String token, Date now) {
		String urlEncodedFirstName = null;
		String urlEncodedLastName = null;
		String urlEncodedEmail = null;
		String urlEncodedTokenTimestampString = null;
		String urlEncodedDomain = null;
		String urlEncodedMac = null;
		String[] requestParams = token.split(AMPERSAND);
		for (String param : requestParams) {
			if (param.startsWith(EMAIL_VALIDATION_FIRST_NAME_PARAM+EQUALS)) {
				urlEncodedFirstName = param.substring((EMAIL_VALIDATION_FIRST_NAME_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_LAST_NAME_PARAM+EQUALS)) {
				urlEncodedLastName = param.substring((EMAIL_VALIDATION_LAST_NAME_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_EMAIL_PARAM+EQUALS)) {
				urlEncodedEmail = param.substring((EMAIL_VALIDATION_EMAIL_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS)) {
				urlEncodedTokenTimestampString = param.substring((EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_DOMAIN_PARAM+EQUALS)) {
				urlEncodedDomain = param.substring((EMAIL_VALIDATION_DOMAIN_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS)) {
				urlEncodedMac = param.substring((EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS).length());
			}
		}
		if (urlEncodedFirstName==null) throw new IllegalArgumentException("first name is missing.");
		if (urlEncodedLastName==null) throw new IllegalArgumentException("last name is missing.");
		if (urlEncodedEmail==null) throw new IllegalArgumentException("email is missing.");
		if (urlEncodedTokenTimestampString==null) throw new IllegalArgumentException("time stamp is missing.");
		if (urlEncodedDomain==null) throw new IllegalArgumentException("domain is missing.");
		if (urlEncodedMac==null) throw new IllegalArgumentException("digital signature is missing.");
		String email;
		String tokenTimestampString;
		try {
			email = URLDecoder.decode(urlEncodedEmail, PARAMETER_CHARSET);
			tokenTimestampString = URLDecoder.decode(urlEncodedTokenTimestampString, PARAMETER_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		Date tokenTimestamp;
		DateFormat df = new SimpleDateFormat(DATE_FORMAT_ISO8601);
		try {
			tokenTimestamp = df.parse(tokenTimestampString);
		} catch (ParseException e) {
			throw new IllegalArgumentException(tokenTimestampString+" is not a properly formatted time stamp", e);
		}
		if (now.getTime()-tokenTimestamp.getTime()>EMAIL_VALIDATION_TIME_LIMIT_MILLIS) 
			throw new IllegalArgumentException("Email validation link is out of date.");
		String mac = generateSignatureForNewAccount(
				urlEncodedFirstName, urlEncodedLastName, 
				urlEncodedEmail, urlEncodedTokenTimestampString, urlEncodedDomain);
		String newUrlEncodedMac;
		try {
			newUrlEncodedMac = URLEncoder.encode(mac, PARAMETER_CHARSET);
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if (!urlEncodedMac.equals(newUrlEncodedMac))
			throw new IllegalArgumentException("Invalid digital signature.");
		return email;
	}

	public static String validateEmailSignedToken(EmailValidationSignedToken token, Date now) {
		if (token.getUserId() != null)
			throw new IllegalArgumentException("EmailValidationSignedToken.token must be null");
		String email = token.getEmail();
		ValidateArgument.required(email, "EmailValidationSignedToken.email");
		Date createdOn = token.getCreatedOn();
		ValidateArgument.required(createdOn, "EmailValidationSignedToken.createdOn");
		if (now.getTime() - createdOn.getTime() > EMAIL_VALIDATION_TIME_LIMIT_MILLIS)
			throw new IllegalArgumentException("Email validation link is out of date.");
		SignedTokenUtil.validateToken(token);
		return email;
	}

	// will throw exception for invalid email, invalid endpoint, or an email which is already taken
	@Override
	public void newAccountEmailValidation(NewUser user, String portalEndpoint) {
		AliasEnum.USER_EMAIL.validateAlias(user.getEmail());
		// is the email taken?
		if (!principalAliasDAO.isAliasAvailable(user.getEmail())) {
			throw new NameConflictException("The email address provided is already used.");
		}
		String token = createAccountCreationToken(user, new Date());
		String url = portalEndpoint+token;
		EmailUtils.validateSynapsePortalHost(url);
		String subject = "Welcome to Synapse!";
		Map<String,String> fieldValues = new HashMap<>();
		fieldValues.put(EmailUtils.TEMPLATE_KEY_ORIGIN_CLIENT, "Synapse");
		fieldValues.put(EmailUtils.TEMPLATE_KEY_WEB_LINK, url);
		String messageBody = EmailUtils.readMailTemplate("message/CreateAccountTemplate.html", fieldValues);
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(user.getEmail())
				.withSubject(subject)
				.withBody(messageBody, BodyType.HTML)
				.withIsNotificationMessage(true)
				.build();	
		sesClient.sendRawEmail(sendEmailRequest);
	}

	@WriteTransaction
	@Override
	public Session createNewAccount(AccountSetupInfo accountSetupInfo) throws NotFoundException {
		String validatedEmail = validateAccountSetupInfo(accountSetupInfo, new Date());
		NewUser newUser = new NewUser();
		newUser.setEmail(validatedEmail);
		newUser.setFirstName(accountSetupInfo.getFirstName());
		newUser.setLastName(accountSetupInfo.getLastName());
		newUser.setUserName(accountSetupInfo.getUsername());
		long newPrincipalId = userManager.createUser(newUser);
		
		authManager.changePassword(newPrincipalId, accountSetupInfo.getPassword());
		return authManager.authenticate(newPrincipalId, accountSetupInfo.getPassword());
	}

	@Deprecated
	public static String generateSignatureForAdditionalEmail(String userId, String email, String timestamp, String domain) {
		return generateSignature(userId+email+timestamp+domain);
	}

	@Deprecated
	public static String createTokenForAdditionalEmail(Long userId, String email, Date now) {
		try {
			StringBuilder sb = new StringBuilder();
			String urlEncodedUserId = URLEncoder.encode(userId.toString(), PARAMETER_CHARSET);
			sb.append(EMAIL_VALIDATION_USER_ID_PARAM+EQUALS+urlEncodedUserId);
			String urlEncodedEmail = URLEncoder.encode(email, PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_EMAIL_PARAM+EQUALS+urlEncodedEmail);
			DateFormat df = new SimpleDateFormat(DATE_FORMAT_ISO8601);
			String timestampString = df.format(now);
			String urlEncodedTimeStampString = URLEncoder.encode(timestampString, PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS+urlEncodedTimeStampString);
			String urlEncodedDomain = URLEncoder.encode("Synapse", PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_DOMAIN_PARAM+EQUALS+urlEncodedDomain);
			String mac = generateSignatureForAdditionalEmail(urlEncodedUserId, urlEncodedEmail,
					urlEncodedTimeStampString, urlEncodedDomain);
			sb.append(AMPERSAND+EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS+URLEncoder.encode(mac, PARAMETER_CHARSET));
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private String createAddEmailInfo(Long userId, String email, Date now) {
		AddEmailInfo addEmailInfo = new AddEmailInfo();
		EmailValidationSignedToken emailValidationSignedToken = new EmailValidationSignedToken();
		emailValidationSignedToken.setUserId(userId + "");
		emailValidationSignedToken.setEmail(email);
		emailValidationSignedToken.setCreatedOn(now);
		SignedTokenUtil.signToken(emailValidationSignedToken);
		addEmailInfo.setEmailValidationSignedToken(emailValidationSignedToken);
		return SerializationUtils.serializeAndHexEncode(addEmailInfo);
	}

	private static class ValidatedEmailInfo {
		public String userId;
		public String newEmail;
	}

	public static ValidatedEmailInfo validateAddEmailInfo(AddEmailInfo addEmailInfo, String userId, Date now) {
	    ValidatedEmailInfo result = new ValidatedEmailInfo();
		// Find out which kind of token is being used
		String token = addEmailInfo.getEmailValidationToken();
		EmailValidationSignedToken signedToken = addEmailInfo.getEmailValidationSignedToken();
		if (signedToken != null && token == null) {
			validateAdditionalEmailSignedToken(signedToken, new Date());
			result.newEmail = signedToken.getEmail();
			result.userId = signedToken.getUserId();
		} else if (token != null && signedToken == null) {
			validateAdditionalEmailToken(token, new Date());
			result.newEmail = getParameterValueFromToken(token, EMAIL_VALIDATION_EMAIL_PARAM);
			result.userId = getParameterValueFromToken(token, EMAIL_VALIDATION_USER_ID_PARAM);
		} else {
			throw new IllegalArgumentException("One and only one type of email validation token must be provided.");
		}
		if (!result.userId.equals(userId))
			throw new IllegalArgumentException("Invalid token for userId " + userId);
		return result;
	}

	public static String validateAdditionalEmailSignedToken(EmailValidationSignedToken token, Date now) {
	    ValidateArgument.required(token.getUserId(), "EmailValidationSignedToken.userId");
		String email = token.getEmail();
		ValidateArgument.required(email, "EmailValidationSignedToken.email");
		Date createdOn = token.getCreatedOn();
		ValidateArgument.required(createdOn, "EmailValidationSignedToken.createdOn");
		if (now.getTime() - createdOn.getTime() > EMAIL_VALIDATION_TIME_LIMIT_MILLIS)
			throw new IllegalArgumentException("Email validation link is out of date.");
		SignedTokenUtil.validateToken(token);
		return email;
	}

	@Deprecated
	// returns the validated email address
	// note, we pass the current time as a parameter to facilitate testing
	public static void validateAdditionalEmailToken(String token, Date now) {
		String urlEncodedUserId = null;
		String urlEncodedEmail = null;
		String urlEncodedTimestampString = null;
		String urlEncodedDomain = null;
		String urlEncodedMac = null;
		String[] requestParams = token.split(AMPERSAND);
		for (String param : requestParams) {
			if (param.startsWith(EMAIL_VALIDATION_USER_ID_PARAM+EQUALS)) {
				urlEncodedUserId = param.substring((EMAIL_VALIDATION_USER_ID_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_EMAIL_PARAM+EQUALS)) {
				urlEncodedEmail = param.substring((EMAIL_VALIDATION_EMAIL_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS)) {
				urlEncodedTimestampString = param.substring((EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_DOMAIN_PARAM+EQUALS)) {
				urlEncodedDomain = param.substring((EMAIL_VALIDATION_DOMAIN_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS)) {
				urlEncodedMac = param.substring((EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS).length());
			}
		}
		if (urlEncodedUserId==null) throw new IllegalArgumentException("userId is missing.");
		if (urlEncodedEmail==null) throw new IllegalArgumentException("email is missing.");
		if (urlEncodedTimestampString==null) throw new IllegalArgumentException("time stamp is missing.");
		if (urlEncodedDomain==null) throw new IllegalArgumentException("domain is missing.");
		if (urlEncodedMac==null) throw new IllegalArgumentException("digital signature is missing.");
		String tokenTimestampString;
		try {
			tokenTimestampString = URLDecoder.decode(urlEncodedTimestampString, PARAMETER_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		Date tokenTimestamp;
		DateFormat df = new SimpleDateFormat(DATE_FORMAT_ISO8601);
		try {
			tokenTimestamp = df.parse(tokenTimestampString);
		} catch (ParseException e) {
			throw new IllegalArgumentException(tokenTimestampString+" is not a properly formatted time stamp", e);
		}
		if (now.getTime()-tokenTimestamp.getTime()>EMAIL_VALIDATION_TIME_LIMIT_MILLIS) 
			throw new IllegalArgumentException("Email validation link is out of date.");
		String mac = generateSignatureForAdditionalEmail(
				urlEncodedUserId, urlEncodedEmail, urlEncodedTimestampString, urlEncodedDomain);
		String newUrlEncodedMac;
		try {
			newUrlEncodedMac = URLEncoder.encode(mac, PARAMETER_CHARSET);
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if (!urlEncodedMac.equals(newUrlEncodedMac))
			throw new IllegalArgumentException("Invalid digital signature.");
	}

	@Override
	public void additionalEmailValidation(UserInfo userInfo, Username email,
			String portalEndpoint) throws NotFoundException {
		if (AuthorizationUtils.isUserAnonymous(userInfo.getId()))
			throw new UnauthorizedException("Anonymous user may not add email address.");
		AliasEnum.USER_EMAIL.validateAlias(email.getEmail());
		// is the email taken?
		if (!principalAliasDAO.isAliasAvailable(email.getEmail())) {
			throw new NameConflictException("The email address provided is already used.");
		}
		String token = createAddEmailInfo(userInfo.getId(), email.getEmail(), new Date());
		String url = portalEndpoint+token;
		EmailUtils.validateSynapsePortalHost(url);

		// all requirements are met, so send the email
		String subject = "Request to add or change new email";
		Map<String,String> fieldValues = new HashMap<String,String>();
		UserProfile userProfile = userProfileDAO.get(userInfo.getId().toString());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_DISPLAY_NAME, userProfile.getFirstName()+" "+userProfile.getLastName());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_WEB_LINK, url);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_EMAIL, email.getEmail());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_ORIGIN_CLIENT, "Synapse");
		fieldValues.put(EmailUtils.TEMPLATE_KEY_USERNAME, principalAliasDAO.getUserName(userInfo.getId()));
		String messageBody = EmailUtils.readMailTemplate("message/AdditionalEmailTemplate.html", fieldValues);
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(email.getEmail())
				.withSubject(subject)
				.withBody(messageBody, BodyType.HTML)
				.withIsNotificationMessage(true)
				.build();
		sesClient.sendRawEmail(sendEmailRequest);
	}

	public static String getParameterValueFromToken(String token, String paramName) {
		String[] requestParams = token.split(AMPERSAND);
		for (String param : requestParams) {
			if (param.startsWith(paramName+EQUALS)) {
				String urlEncodedParamValue = param.substring((paramName+EQUALS).length());
				try {
					return URLDecoder.decode(urlEncodedParamValue, PARAMETER_CHARSET);
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}
		throw new IllegalArgumentException("token does not contain parameter "+paramName);
	}

	@WriteTransaction
	@Override
	public void addEmail(UserInfo userInfo, AddEmailInfo addEmailInfo,
			Boolean setAsNotificationEmail) throws NotFoundException {
		ValidatedEmailInfo info = validateAddEmailInfo(addEmailInfo, Long.toString(userInfo.getId()), new Date());
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(info.newEmail);
		alias.setPrincipalId(userInfo.getId());
		alias.setType(AliasType.USER_EMAIL);
		alias = principalAliasDAO.bindAliasToPrincipal(alias);
		if (setAsNotificationEmail!=null && setAsNotificationEmail==true) notificationEmailDao.update(alias);
	}

	@WriteTransaction
	@Override
	public void removeEmail(UserInfo userInfo, String email) throws NotFoundException {
		if (email.equals(notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId())))
				throw new IllegalArgumentException("To remove this email from your account, first establish a different notification address.");
		PrincipalAlias emailAlias = findAliasForEmail(userInfo.getId(), email);
		principalAliasDAO.removeAliasFromPrincipal(userInfo.getId(), emailAlias.getAliasId());
	}
	
	private PrincipalAlias findAliasForEmail(Long principalId, String email) throws NotFoundException {
		List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_EMAIL, email);
		if (aliases.size()==0) {
			throw new NotFoundException("Cannot find alias for "+principalId+" matching "+email);			
		} else if (aliases.size()==1) {
			return aliases.get(0);
		} else {
			throw new DatastoreException("Expected 0-1 results but found "+aliases.size());
		}
	}

	/**
	 * Set the email which is used for notification.
	 * 
	 * @param userInfo
	 * @param email
	 * @throws NotFoundException 
	 */
	@WriteTransaction
	@Override
	public void setNotificationEmail(UserInfo userInfo, String email) throws NotFoundException {
		PrincipalAlias emailAlias = findAliasForEmail(userInfo.getId(), email);
		notificationEmailDao.update(emailAlias);
	}
	
	/**
	 * Get the email which is used for notification.
	 * 
	 * @param userInfo
	 * @return
	 * @throws NotFoundException
	 */
	@Override
	public Username getNotificationEmail(UserInfo userInfo) throws NotFoundException {
		String email= notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		Username dto = new Username();
		dto.setEmail(email);
		return dto;
	}

	@Override
	public PrincipalAliasResponse lookupPrincipalId(PrincipalAliasRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAlias(), "PrincipalAliasRequest.alias");
		ValidateArgument.required(request.getType(), "PrincipalAliasRequest.type");
		ValidateArgument.requirement(request.getType() == AliasType.USER_NAME, "Unsupported alias type "+request.getType());
		long principalId = principalAliasDAO.lookupPrincipalID(request.getAlias(), request.getType());
		PrincipalAliasResponse response = new PrincipalAliasResponse();
		response.setPrincipalId(principalId);
		return response;
	}

}
