package com.ecg.comaas.bt.filter.identicalcap;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.search.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_AR;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MX;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_SG;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_ZA;

@ComaasPlugin
@Profile({TENANT_MX, TENANT_AR, TENANT_ZA, TENANT_SG})
@Component
public class IdenticalCapFilterFactory implements FilterFactory {
	public static final String IDENTIFIER = "com.ebay.ecg.bolt.replyts.identicalcapfilter.IdenticalCapFilterFactory";

    private static final Logger LOG = LoggerFactory.getLogger(IdenticalCapFilterFactory.class);

	@Autowired
	private SearchService searchService;

	@Autowired
	private ConversationRepository conversationRepository;

	@Override
	public Filter createPlugin(String instanceName, JsonNode jsonNode) {
		LOG.debug("Instance Name for IdenticalCapFilter is {}", instanceName);

		return new IdenticalCapFilter(searchService, conversationRepository, ConfigurationParser.parse(jsonNode));
	}

	@Override
	public String getIdentifier() {
	    return IDENTIFIER;
	}
}
