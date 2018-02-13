package com.ebay.ecg.bolt.api.server.push.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class Error {
	@JsonSerialize
	@JsonDeserialize
	private String msg;
	
	public Error(String errMsg) {
		msg = errMsg;
	}
}