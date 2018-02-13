package com.ebay.ecg.bolt.api.server.push.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class MetaChatMessage extends Meta {
	@JsonSerialize
	@JsonDeserialize
	private String conversationId;
	
	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}
}