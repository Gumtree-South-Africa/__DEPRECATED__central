package com.ebay.ecg.bolt.platform.module.push.persistence.repository;

import java.util.List;
import java.util.Locale;

import com.ebay.ecg.bolt.api.server.push.model.PushProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PushRegistration;
import org.springframework.stereotype.Component;

@Component
public class PushServiceRepository {
	@Autowired
	@Qualifier("pushServiceDatabase")
	private MongoOperations pushServiceDatabase;

	public List<PushRegistration> find(String registerUserId, String notificationType, String appType, Locale locale) {
		return pushServiceDatabase.find(
				new Query(
						new Criteria("registerUserId").is(registerUserId).
								and("notificationType").is(notificationType).
								and("appType").is(appType).
								and("locale").is(locale)), PushRegistration.class);
	}

	public List<PushRegistration> find(String registerUserId, String notificationType, Locale locale) {
		return pushServiceDatabase.find(
				new Query(
						new Criteria("registerUserId").is(registerUserId).
								and("notificationType").is(notificationType).
								and("locale").is(locale)), PushRegistration.class);
	}

	public void save(PushRegistration pushRegistration) {
		pushServiceDatabase.save(pushRegistration);
	}

	public void remove(String registerUserId, String notificationType, String pushProvider, Locale locale) {
		pushServiceDatabase.remove(
				new Query(
						new Criteria("registerUserId").is(registerUserId).
								and("notificationType").is(notificationType).
								and("pushProvider").is(pushProvider).
								and("locale").is(locale)), PushRegistration.class);
	}

	public PushRegistration find(String registerUserId, String notificationType, PushProvider pushProvider, String appType, Locale locale) {
		return pushServiceDatabase.findOne(
				new Query(
						new Criteria("registerUserId").is(registerUserId).
								and("notificationType").is(notificationType).
								and("pushProvider").is(pushProvider.name()).
								and("locale").is(locale).
								and("appType").is(appType)), PushRegistration.class);
	}

    public void remove(String id) {
		pushServiceDatabase.remove(
				new Query(new Criteria("_id").is(id)), PushRegistration.class);

	}
}