package org.sagebionetworks.markdown;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.utils.HttpClientHelper;

public class MarkdownHttpClientProvider {

	private static final int DEFAULT_CONNECTION_TIMEOUT = 20*1000;
	private static final int DEFAULT_SOCKET_TIMEOUT = 20*1000;
	private Integer connectionTimeout;
	private Integer socketTimeout;
	private HttpClient httpClient;

	public void setConnectionTimeout(Integer timeout) {
		connectionTimeout =  timeout;
	}

	public void setSocketTimeout(Integer timeout) {
		socketTimeout = timeout;
	}

	public HttpClient getHttpClient() {
		if (httpClient == null) {
			_init();
		}
		return httpClient;
	}

	// Called by Spring
	public void _init() {
		ThreadSafeClientConnManager connectionManager;
		try {
			if (connectionTimeout == null) {
				connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
			}
			if (socketTimeout == null) {
				socketTimeout = DEFAULT_SOCKET_TIMEOUT;
			}
			
			connectionManager = HttpClientHelper.createClientConnectionManager(true);
			connectionManager.setDefaultMaxPerRoute(StackConfiguration.getHttpClientMaxConnsPerRoute());
			HttpParams clientParams = new BasicHttpParams();
			clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,connectionTimeout);
			clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
			httpClient = new DefaultHttpClient(connectionManager, clientParams);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
