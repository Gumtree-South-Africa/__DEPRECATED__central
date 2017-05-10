package com.gumtree.comaas.common.filter;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableBiMap;
import com.gumtree.filters.comaas.config.ConfigWithExemptedCategories;
import com.gumtree.filters.comaas.config.Result;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public final class GumtreeFilterUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeFilterUtil.class);

    private static final String FILTER_NAME = "filterName";
    private static final String FILTER_INSTANCE = "filterInstance";
    private static final String FILTER_VERSION = "filterVersion";
    private static final String DESCRIPTION = "description";

    public static final ImmutableBiMap<Result, FilterResultState> resultFilterResultMap = new ImmutableBiMap.Builder<Result, FilterResultState>()
            .put(Result.DROP, FilterResultState.DROPPED)
            .put(Result.HOLD, FilterResultState.HELD)
            .put(Result.STOP_FILTERING, FilterResultState.ACCEPT_AND_TERMINATE)
            .build();

    public static String longDescription(Class<?> clazz, String instanceId, String version, String shortDescription) {
        try {
            return new JSONObject()
                    .put(FILTER_NAME, clazz.getName())
                    .put(FILTER_INSTANCE, instanceId)
                    .put(FILTER_VERSION, version)
                    .put(DESCRIPTION, shortDescription)
                    .toString();
        } catch (JSONException e) {
            LOG.error("Error converting processing feedback description as JSON", e);
            return "{ }";
        }
    }

    public static boolean hasExemptedCategory(ConfigWithExemptedCategories config, MessageProcessingContext messageProcessingContext) {
        Set<Long> categoryBreadCrumb = (Set<Long>) messageProcessingContext.getFilterContext().get("categoryBreadCrumb");
        return hasExemptedCategory(config.getExemptedCategories(), categoryBreadCrumb);
    }

    public static boolean hasExemptedCategory(List<Long> exemptedCategories, Set<Long> breadCrumbs) {
        return !(breadCrumbs == null || exemptedCategories == null) && exemptedCategories.stream().anyMatch(breadCrumbs::contains);
    }
}
