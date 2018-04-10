package com.ecg.comaas.mde.postprocessor.sms;

public class PhoneNumber {

	private int countryCode;
	
	private String displayNumber;

	private PhoneNumber() {
	}

	public PhoneNumber(int countryCode, String displayNumber){
		this.countryCode = countryCode;
		this.displayNumber = displayNumber;
	}

	public int getCountryCode() {
		return countryCode;
	}

	public String getDisplayNumber() {
		return displayNumber;
	}

	public void setCountryCode(int countryCode) {
		this.countryCode = countryCode;
	}

	public void setDisplayNumber(String displayNumber) {
		this.displayNumber = displayNumber;
	}

	@Override
	public String toString() {
		return "PhoneNumber [countryCode=" + countryCode + ", displayNumber="
				+ displayNumber + "]";
	}
	
	
}
