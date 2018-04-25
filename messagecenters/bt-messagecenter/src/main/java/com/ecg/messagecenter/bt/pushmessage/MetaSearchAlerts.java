package com.ecg.messagecenter.bt.pushmessage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class MetaSearchAlerts extends Meta {

	@JsonSerialize
	@JsonDeserialize
	private String alertId;
	
	@JsonSerialize
	@JsonDeserialize
	private String notificationId;

	public String getAlertId() {
		return alertId;
	}

	public void setAlertId(String alertId) {
		this.alertId = alertId;
	}

	public String getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(String notificationId) {
		this.notificationId = notificationId;
	}

}
