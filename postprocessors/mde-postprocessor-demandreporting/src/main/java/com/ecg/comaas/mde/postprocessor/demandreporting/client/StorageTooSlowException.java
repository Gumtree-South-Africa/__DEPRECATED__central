package com.ecg.comaas.mde.postprocessor.demandreporting.client;

public class StorageTooSlowException extends Exception {
	private static final long serialVersionUID = 2L;
	
	public StorageTooSlowException(String message) {
		super(message);
	}
}
