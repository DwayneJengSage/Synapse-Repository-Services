package org.sagebionetworks.repo.manager.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

/*
 * Need this non-final implementation of SimpleHttpClient to allow
 * Spring MVC to inject into other classes.
 */
public class HttpClientImpl implements SimpleHttpClient {
	private SimpleHttpClient client;
	
	private static final Integer TIME_OUT = 30 * 1000; // 30 seconds

	public HttpClientImpl() {
		SimpleHttpClientConfig httpClientConfig = new SimpleHttpClientConfig();
		httpClientConfig.setSocketTimeoutMs(TIME_OUT);
		this.client = new SimpleHttpClientImpl(httpClientConfig);
	}

	@Override
	public SimpleHttpResponse get(SimpleHttpRequest request) throws ClientProtocolException, IOException {
		return client.get(request);
	}

	@Override
	public SimpleHttpResponse post(SimpleHttpRequest request, String requestBody)
			throws ClientProtocolException, IOException {
		return client.post(request, requestBody);
	}

	@Override
	public SimpleHttpResponse put(SimpleHttpRequest request, String requestBody)
			throws ClientProtocolException, IOException {
		return client.put(request, requestBody);
	}

	@Override
	public SimpleHttpResponse delete(SimpleHttpRequest request) throws ClientProtocolException, IOException {
		return delete(request);
	}

	@Override
	public SimpleHttpResponse putFile(SimpleHttpRequest request, File toUpload)
			throws ClientProtocolException, IOException {
		return client.putFile(request, toUpload);
	}

	@Override
	public SimpleHttpResponse getFile(SimpleHttpRequest request, File result)
			throws ClientProtocolException, IOException {
		return client.getFile(request, result);
	}

	@Override
	public SimpleHttpResponse putToURL(SimpleHttpRequest request, InputStream toUpload, long inputLength)
			throws ClientProtocolException, IOException {
		return client.putToURL(request, toUpload, inputLength);
	}

	@Override
	public String getFirstCookieValue(String domain, String name) {
		return client.getFirstCookieValue(domain, name);
	}

	@Override
	public void addCookie(String domain, String name, String value) {
		client.addCookie(domain, name, value);	
	}

	@Override
	public void clearAllCookies() {
		client.clearAllCookies();
	}
}
