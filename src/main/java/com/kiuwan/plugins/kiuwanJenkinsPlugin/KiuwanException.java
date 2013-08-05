package com.kiuwan.plugins.kiuwanJenkinsPlugin;

public class KiuwanException extends Exception {

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
