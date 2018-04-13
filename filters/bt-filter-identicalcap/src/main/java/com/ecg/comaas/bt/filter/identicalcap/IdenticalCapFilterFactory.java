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
import org.springframework.stereotype.Component;

@ComaasPlugin
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
