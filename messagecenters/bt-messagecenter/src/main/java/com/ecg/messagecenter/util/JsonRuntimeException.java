package com.ecg.messagecenter.util;
/**
 * Converter to runtime exception.
 * @author nbarkhatov
 *
 */
public class JsonRuntimeException extends RuntimeException {

	public JsonRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	private static final long serialVersionUID = 20120522183848L;

}
