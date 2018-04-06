package com.ecg.replyts.bolt.filter.user;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@ComaasPlugin
@Component
public class BoltUserFilterFactory implements FilterFactory {
    public static final String IDENTIFIER = "com.ecg.replyts.bolt.filter.user.BoltUserFilterFactory";

    private static final Logger LOG = LoggerFactory.getLogger(BoltUserFilterFactory.class);

    private static final int DEFAULT_SCORE = 50;
    private static final int NEW_USER_HOURS = 480;

    @Value( "${replyts.user.snapshot.kernel.url:http://kernel.service.consul/api/users/snapshots}" )
    private String kernelApiUrl;

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        BoltUserFilterConfig config = buildFilterConfig(configuration);

        return config != null ? new BoltUserFilter(config, new RestTemplate()) : null;
    }

    private BoltUserFilterConfig buildFilterConfig(JsonNode configuration) {

        BoltUserFilterConfig config = new BoltUserFilterConfig();

        config.setKernelApiUrl(kernelApiUrl);

        JsonNode urlNode = configuration.get("block_user_score");
        int blockUserScore = DEFAULT_SCORE;
        try {
            blockUserScore = Integer.parseInt(urlNode.textValue());
        } catch (NumberFormatException nfe) {
            LOG.error("Invalid score value: {}. Using default score: {}", urlNode.textValue(), DEFAULT_SCORE);
        }
        config.setBlockUserScore(blockUserScore);

        urlNode = configuration.get("blacklist_user_score");
        int blackListUserScore = DEFAULT_SCORE;
        try {
            blackListUserScore = Integer.parseInt(urlNode.textValue());
        } catch (NumberFormatException nfe) {
            LOG.error("Invalid score value: {}. Using default score: {}", urlNode.textValue(), DEFAULT_SCORE);
        }
        config.setBlackListUserScore(blackListUserScore);

        urlNode = configuration.get("new_user_score");
        int newUserScore = DEFAULT_SCORE;
        try {
            newUserScore = Integer.parseInt(urlNode.textValue());
        } catch (NumberFormatException nfe) {
            LOG.error("Invalid score value: {}. Using default score: {}", urlNode.textValue(), DEFAULT_SCORE);
        }
        config.setNewUserScore(newUserScore);

        urlNode = configuration.get("new_user_hours");
        int newUserHours = NEW_USER_HOURS;
        try {
            newUserHours = Integer.parseInt(urlNode.textValue());
        } catch (NumberFormatException nfe) {
            LOG.error("Invalid hours value: "+urlNode.textValue());
        }
        config.setNewUserHours(newUserHours);

        JsonNode node = configuration.get("blacklist");
        if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
            ArrayNode arrayNode = (ArrayNode) node;
            LOG.info("Found #{} elements in blacklist", arrayNode.size());
            for (int i = 0; i < arrayNode.size(); i++) {
                String email = arrayNode.get(i).get("email").textValue();
                config.addToBlackList(email);
            }
        }

        JsonNode rulesNode = configuration.get("blackListRules");
        if (rulesNode != null && rulesNode.getNodeType().equals(JsonNodeType.ARRAY)) {
            ArrayNode arrayNode = (ArrayNode) rulesNode;
            LOG.info("Found #{} elements in blacklistRules", arrayNode.size());
            for (int i = 0; i < arrayNode.size(); i++) {
                String regExpression = arrayNode.get(i).get("regExp").asText();
                config.addToBlackListEmailPattern(Pattern.compile(regExpression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
        }

        JsonNode userNameNode = configuration.get("blackListUserNameRules");
        if (userNameNode != null && userNameNode.getNodeType().equals(JsonNodeType.ARRAY)) {
            ArrayNode arrayNode = (ArrayNode) userNameNode;
            LOG.info("Found #{} elements in blackListUserNameRules", arrayNode.size());
            for (int i = 0; i < arrayNode.size(); i++) {
                String regExpression = arrayNode.get(i).get("regExp").asText();
                config.addToBlackListUserNamePattern(Pattern.compile(regExpression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
        }

        return config;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}