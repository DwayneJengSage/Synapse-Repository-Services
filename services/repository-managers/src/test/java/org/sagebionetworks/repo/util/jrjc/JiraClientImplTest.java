package org.sagebionetworks.repo.util.jrjc;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JiraClientImplTest {

    @Mock
    private StackConfiguration mockConfig;
    @Mock
    private SimpleHttpClient mockHttpClient;

    @Mock
    private SimpleHttpResponse mockResponse;

    private static final String USERNAME = "userName";
    private static final String USERAPIKEY = "userApiKey";

    @InjectMocks
    private JiraClientImpl jiraClient;

    @Test
    public void testGetProjectInfo() throws Exception {
        String expectedJson =
                "{  \"id\": \"10000\"," +
                "  \"issueTypes\": [" +
                "    {" +
                "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/3\"," +
                "      \"id\": \"3\"," +
                "      \"description\": \"A task that needs to be done.\"," +
                "      \"iconUrl\": \"https://your-domain.atlassian.net//secure/viewavatar?size=xsmall&avatarId=10299&avatarType=issuetype\\\",\"," +
                "      \"name\": \"Task\"," +
                "      \"subtask\": false," +
                "      \"avatarId\": 1" +
                "    }," +
                "    {" +
                "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/1\"," +
                "      \"id\": \"1\"," +
                "      \"description\": \"A problem with the software.\"," +
                "      \"iconUrl\": \"https://your-domain.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10316&avatarType=issuetype\\\",\"," +
                "      \"name\": \"Bug\"," +
                "      \"subtask\": false," +
                "      \"avatarId\": 10002," +
                "      \"entityId\": \"9d7dd6f7-e8b6-4247-954b-7b2c9b2a5ba2\"," +
                "      \"scope\": {" +
                "        \"type\": \"PROJECT\"," +
                "        \"project\": {" +
                "          \"id\": \"10000\"," +
                "          \"key\": \"KEY\"," +
                "          \"name\": \"Next Gen Project\"" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}";

        when(mockConfig.getJiraUserEmail()).thenReturn(USERNAME);
        when(mockConfig.getJiraUserApikey()).thenReturn(USERAPIKEY);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        ProjectInfo pInfo = jiraClient.getProjectInfo("SG", "Task");

        assertNotNull(pInfo);
        String projId = (String) pInfo.getProjectId();
        assertEquals("10000", projId);
        Long issueTypeId = (Long) pInfo.getIssueTypeId();
        assertTrue(3L==issueTypeId);
    }

    @Test
    public void testGetProjectInfoIssueTypeNotFound() throws Exception {
        String expectedJson =
                "{  \"id\": \"10000\"," +
                        "  \"issueTypes\": [" +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/3\"," +
                        "      \"id\": \"3\"," +
                        "      \"description\": \"A task that needs to be done.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net//secure/viewavatar?size=xsmall&avatarId=10299&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Flag\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 1" +
                        "    }," +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/1\"," +
                        "      \"id\": \"1\"," +
                        "      \"description\": \"A problem with the software.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10316&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Bug\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 10002," +
                        "      \"entityId\": \"9d7dd6f7-e8b6-4247-954b-7b2c9b2a5ba2\"," +
                        "      \"scope\": {" +
                        "        \"type\": \"PROJECT\"," +
                        "        \"project\": {" +
                        "          \"id\": \"10000\"," +
                        "          \"key\": \"KEY\"," +
                        "          \"name\": \"Next Gen Project\"" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  ]}";

        when(mockConfig.getJiraUserEmail()).thenReturn(USERNAME);
        when(mockConfig.getJiraUserApikey()).thenReturn(USERAPIKEY);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        Assertions.assertThrows(JiraClientException.class, () -> {
                    jiraClient.getProjectInfo("SG", "Task");
                }
        );
    }

    @Test
    public void testGetProjectInfoInvalidJson() throws Exception {
        String expectedJson =
                "{  \"id\": \"10000\"," +
                        "  \"issueTypes\": [" +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/3\"," +
                        "      \"id\": \"3\"," +
                        "      \"description\": \"A task that needs to be done.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net//secure/viewavatar?size=xsmall&avatarId=10299&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Flag\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 1" +
                        "    ," +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/1\"," +
                        "      \"id\": \"1\"," +
                        "      \"description\": \"A problem with the software.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10316&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Bug\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 10002," +
                        "      \"entityId\": \"9d7dd6f7-e8b6-4247-954b-7b2c9b2a5ba2\"," +
                        "      \"scope\": {" +
                        "        \"type\": \"PROJECT\"," +
                        "        \"project\": {" +
                        "          \"id\": \"10000\"," +
                        "          \"key\": \"KEY\"," +
                        "          \"name\": \"Next Gen Project\"" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  ]}";

        when(mockConfig.getJiraUserEmail()).thenReturn(USERNAME);
        when(mockConfig.getJiraUserApikey()).thenReturn(USERAPIKEY);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        Assertions.assertThrows(JiraClientException.class, () -> {
                    jiraClient.getProjectInfo("SG", "Task");
                }
        );
    }

    @Test
    public void testGetFields() throws Exception {
        String expectedJson =
                "[" +
                "  {" +
                "    \"id\": \"description\"," +
                "    \"name\": \"Description\"," +
                "    \"custom\": false," +
                "    \"orderable\": true," +
                "    \"navigable\": true," +
                "    \"searchable\": true," +
                "    \"clauseNames\": [" +
                "      \"description\"" +
                "    ]," +
                "    \"schema\": {" +
                "      \"type\": \"string\"," +
                "      \"system\": \"description\"" +
                "    }" +
                "  }," +
                "  {" +
                "    \"id\": \"summary\"," +
                "    \"key\": \"summary\"," +
                "    \"name\": \"Summary\"," +
                "    \"custom\": false," +
                "    \"orderable\": true," +
                "    \"navigable\": true," +
                "    \"searchable\": true," +
                "    \"clauseNames\": [" +
                "      \"summary\"" +
                "    ]," +
                "    \"schema\": {" +
                "      \"type\": \"string\"," +
                "      \"system\": \"summary\"" +
                "    }" +
                "  }" +
                "]";

        when(mockConfig.getJiraUserEmail()).thenReturn(USERNAME);
        when(mockConfig.getJiraUserApikey()).thenReturn(USERAPIKEY);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        Map<String, String> m = jiraClient.getFields();

        assertNotNull(m);
        assertEquals(2, m.size());
        assertEquals("description", m.get("Description"));
        assertEquals("summary", m.get("Summary"));
    }

    @Test
    public void testGetFieldsInvalidJson() throws Exception {
        String expectedJson =
                "[" +
                        "  {" +
                        "    \"id\": \"description\"," +
                        "    \"name\": \"Description\"," +
                        "    \"custom\": false," +
                        "    \"orderable\": true," +
                        "    \"navigable\": true," +
                        "    \"searchable\": true," +
                        "    \"clauseNames\": [" +
                        "      \"description\"" +
                        "    ]," +
                        "    \"schema\": {" +
                        "      \"type\": \"string\"," +
                        "      \"system\": \"description\"" +
                        "    }" +
                        "  ," +
                        "  {" +
                        "    \"id\": \"summary\"," +
                        "    \"key\": \"summary\"," +
                        "    \"name\": \"Summary\"," +
                        "    \"custom\": false," +
                        "    \"orderable\": true," +
                        "    \"navigable\": true," +
                        "    \"searchable\": true," +
                        "    \"clauseNames\": [" +
                        "      \"summary\"" +
                        "    ]," +
                        "    \"schema\": {" +
                        "      \"type\": \"string\"," +
                        "      \"system\": \"summary\"" +
                        "    }" +
                        "  }" +
                        "]";

        when(mockConfig.getJiraUserEmail()).thenReturn(USERNAME);
        when(mockConfig.getJiraUserApikey()).thenReturn(USERAPIKEY);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        Assertions.assertThrows(JiraClientException.class, () -> {
                    jiraClient.getFields();
                }
        );
    }

    @Test
    public void testCreateIssue() throws Exception {
        String expectedJson =
            "{" +
            "  \"id\": \"10000\"," +
            "  \"key\": \"SG-24\"," +
            "  \"self\": \"https://your-domain.atlassian.net/rest/api/3/issue/10000\"," +
            "  \"transition\": {" +
            "    \"status\": 200," +
            "    \"errorCollection\": {" +
            "      \"errorMessages\": []," +
            "      \"errors\": {}" +
            "    }" +
            "  }" +
            "}";
        when(mockConfig.getJiraUserEmail()).thenReturn(USERNAME);
        when(mockConfig.getJiraUserApikey()).thenReturn(USERAPIKEY);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.post(any(SimpleHttpRequest.class), anyString())).thenReturn(mockResponse);

        BasicIssue issue = new BasicIssue();

        // Call under test
        CreatedIssue i = jiraClient.createIssue(issue);

        assertNotNull(i);
        assertEquals("SG-24", i.getKey());
    }

    @Test
    public void testCreateRequest() throws Exception {

        when(mockConfig.getJiraUserEmail()).thenReturn(USERNAME);
        when(mockConfig.getJiraUserApikey()).thenReturn(USERAPIKEY);
        SimpleHttpRequest req;
        req = jiraClient.createRequest("/aPath/", "aResource");
        assertEquals("https://sagebionetworks.jira.com/aPath/aResource", req.getUri());
        req = jiraClient.createRequest("/aPath/", null);
        assertEquals("https://sagebionetworks.jira.com/aPath/", req.getUri());

        Map<String, String> headers = req.getHeaders();
        assertEquals("Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + USERAPIKEY).getBytes("utf-8")), headers.get(HttpHeaders.AUTHORIZATION));
        assertEquals("application/json", headers.get(HttpHeaders.CONTENT_TYPE));
        assertEquals("Synapse", headers.get(HttpHeaders.USER_AGENT));

    }

    @Test
    public void testCreateRequestBadPath() throws JiraClientException {
        Assertions.assertThrows(JiraClientException.class, () -> {
                    jiraClient.createRequest("/aPath", "aResource");
                }
        );
    }

    @Test
    public void testHandleResponseStatusOK() {
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        JiraClientImpl.handleResponseStatus(mockResponse.getStatusCode()); // Should not fail
    }

    @Test
    public void testHandleResponseStatusError() {
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        Assertions.assertThrows(JiraClientException.class, () -> {
                    JiraClientImpl.handleResponseStatus(mockResponse.getStatusCode());
                }
        );
    }
}