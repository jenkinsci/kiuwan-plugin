package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

public class KiuwanException extends Exception {

	private static final long serialVersionUID = -6103772123682683158L;

	public KiuwanException() {
	}

	public KiuwanException(String message) {
		super(message);
	}

	public KiuwanException(Throwable cause) {
		super(cause);
	}

	public KiuwanException(String message, Throwable cause) {
		super(message, cause);
	}
}
