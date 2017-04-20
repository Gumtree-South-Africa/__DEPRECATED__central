package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Created by fmaffioletti on 10/28/15.
 */
public class FakeWordfilterPluginFactory implements FilterFactory {

    @Override public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new FakeWordfilterPlugin();
    }
}
