package com.ecg.comaas.mde.postprocessor.demandreporting.client;

/**
 * Thrown when a event could not be logged due to a slow storage.
 * @author apeglow
 *
 */
public class StorageTooSlowException extends Exception {
	private static final long serialVersionUID = 2L;
	
	public StorageTooSlowException(String message) {
		super(message);
	}
	
	public StorageTooSlowException(Exception e){
		super(e);
	}

}
