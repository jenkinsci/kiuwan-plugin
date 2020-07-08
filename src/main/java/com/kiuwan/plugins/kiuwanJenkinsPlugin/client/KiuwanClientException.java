package com.kiuwan.plugins.kiuwanJenkinsPlugin.client;

import com.google.gson.JsonSyntaxException;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.JSON;

public class KiuwanClientException extends Exception {
	
	private static final long serialVersionUID = -4940952505703754667L;
	
	private KiuwanClientErrorResponse errorResponse;
	
	public KiuwanClientException(String message) {
		super(message);
	}
	
	public KiuwanClientException(ApiException cause) {
		super(cause);
	}
	
	public KiuwanClientException(String message, ApiException cause) {
		super(message, cause);
	}
	
	public KiuwanClientException(KiuwanClientErrorResponse errorResponse, ApiException cause) {
		super(errorResponse.toString(), cause);
		this.errorResponse = errorResponse;
	}
	
	@Override public synchronized ApiException getCause() { return (ApiException) super.getCause(); }
	public KiuwanClientErrorResponse getErrorResponse() { return errorResponse; }
	
	public static KiuwanClientException from(ApiException e) {
		if (e.getCode() == 401 || e.getCode() == 419) {
			return new KiuwanClientException(e.getCode() + " - Unauthorized");
		}
		
		if (e.getResponseBody() == null) {
			return new KiuwanClientException(e);
		}
		
		KiuwanClientErrorResponse errorResponse = null;
		try {
			errorResponse = JSON.createGson().create().fromJson(e.getResponseBody(), KiuwanClientErrorResponse.class);
		} catch (JsonSyntaxException jse) {
			if (e.getCode() == 404) {
				return new KiuwanClientException(e.getCode() + 
					" - A server responded but Kiuwan is not available in the specified URL");
			}
			
			return new KiuwanClientException("Cannot deserialize error response", e);
		}
		
		return new KiuwanClientException(errorResponse, e);
	}
	
}