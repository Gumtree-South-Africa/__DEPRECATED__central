package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by fmaffioletti on 10/28/15.
 */
public class FakeWordfilterPluginFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.it.kijiji.replyts.FakeWordfilterPluginFactory";

    @Override public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new FakeWordfilterPlugin();
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
