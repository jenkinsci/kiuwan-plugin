package com.kiuwan.plugins.kiuwanJenkinsPlugin.client;

import java.util.Iterator;
import java.util.List;

public class KiuwanClientErrorResponse {
	
	private List<Error> errors;
	
	public List<Error> getErrors() { return errors; }
	
	/** 
	 * Returns the first error found in the error response.
	 * @return The first {@link Error} found or <code>null</code> if no errors are available.
	 */
	public Error getError() {
		if (errors != null && !errors.isEmpty()) return errors.iterator().next();
		return null;
	}
	
	@Override
	public String toString() {
		if (errors == null || errors.isEmpty()) return "No errors";
		StringBuilder sb = new StringBuilder();
		
		if (errors.size() > 1) {
			sb.append(errors.size() + " error(s) found: ");
		}
		
		for (Iterator<Error> it = errors.iterator(); it.hasNext(); ) {
			sb.append(it.next().toString());
			if (it.hasNext()) sb.append(", ");
		}
		return sb.toString();
	}
	
	public class Error {
		
		private String code;
		private String message;
		
		@Override
		public String toString() {
			return this.code + " - " + this.message;
		}
		
		public String getCode() { return code; }
		public String getMessage() { return message; }
		
	}
}