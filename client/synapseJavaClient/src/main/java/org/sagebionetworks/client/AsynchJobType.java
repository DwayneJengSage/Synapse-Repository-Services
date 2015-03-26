package org.sagebionetworks.client;


import static org.sagebionetworks.client.SynapseClientImpl.ASYNC_GET;
import static org.sagebionetworks.client.SynapseClientImpl.ASYNC_START;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_APPEND;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_DOWNLOAD_CSV;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_QUERY;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_QUERY_NEXTPAGE;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_UPLOAD_CSV;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_UPLOAD_CSV_PREVIEW;

import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.HasEntityId;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
/**
 * Maps job types to the URL prefix needed for each type.
 * 
 * @author John
 *
 */
public enum AsynchJobType {
	
	TableAppendRowSet(TABLE_APPEND, RowReferenceSetResults.class),
	TableQuery(TABLE_QUERY, QueryResultBundle.class),
	TableQueryNextPage(TABLE_QUERY_NEXTPAGE, QueryResult.class),
	TableCSVUpload(TABLE_UPLOAD_CSV, UploadToTableResult.class),
	TableCSVUploadPreview(TABLE_UPLOAD_CSV_PREVIEW, UploadToTablePreviewResult.class),
	TableCSVDownload(TABLE_DOWNLOAD_CSV, DownloadFromTableResult.class);
	
	String prefix;
	Class<? extends AsynchronousResponseBody> responseClass;
	
	AsynchJobType(String prefix, Class<? extends AsynchronousResponseBody> responseClass){
		this.prefix = prefix;
		this.responseClass = responseClass;
	}
	
	/**
	 * Get the URL used to start this job type.
	 * @param request
	 */
	public  String getStartUrl(AsynchronousRequestBody request){
		if (request instanceof UploadToTableRequest) {
			UploadToTableRequest obj = (UploadToTableRequest) request;
			return "/entity/" + obj.getTableId() + prefix + ASYNC_START;
		}
		if (request instanceof HasEntityId) {
			HasEntityId obj = (HasEntityId) request;
			if (obj.getEntityId() == null) 
				throw new IllegalArgumentException("entityId cannot be null");
			return "/entity/" + obj.getEntityId() + prefix + ASYNC_START;
		}
		return prefix+ASYNC_START;
	}

	/**
	 * Get the URL used to get the results for this job type.
	 * @param token
	 * @param request
	 * @return
	 */
	public String getResultUrl(String token, AsynchronousRequestBody request){
		if (request instanceof UploadToTableRequest) {
			UploadToTableRequest obj = (UploadToTableRequest) request;
			return "/entity/" + obj.getTableId() + prefix + ASYNC_GET + token;
		}
		if (request instanceof HasEntityId) {
			HasEntityId obj = (HasEntityId) request;
			if (obj.getEntityId() == null) 
				throw new IllegalArgumentException("entityId cannot be null");
			return "/entity/" + obj.getEntityId() + prefix + ASYNC_GET + token;
		}
		return prefix+ASYNC_GET + token;
	}

	/**
	 * Get the URL used to get the results for this job type.
	 * @param token
	 * @param entityId
	 * @return
	 */
	public String getResultUrl(String token, String entityId){
		if (entityId != null) {
			return "/entity/" + entityId + prefix + ASYNC_GET + token;
		}
		return prefix+ASYNC_GET + token;
	}

	/**
	 * Get the response class.
	 * @return
	 */
	public Class<? extends AsynchronousResponseBody> getReponseClass(){
		return responseClass;
	}
}
