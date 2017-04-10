package com.gumtree.comaas.common.filter;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.google.common.collect.ImmutableBiMap;
import org.json.JSONException;
import org.json.JSONObject;
import com.gumtree.filters.comaas.config.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public final class GumtreeFilterUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeFilterUtil.class);

    static final String FILTER_NAME = "filterName";
    static final String FILTER_INSTANCE = "filterInstance";
    static final String FILTER_VERSION = "filterVersion";
    static final String DESCRIPTION = "description";

    public static final ImmutableBiMap<Result, FilterResultState> resultFilterResultMap = new ImmutableBiMap.Builder()
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

    public static boolean hasExemptedCategory(List<Long> exemptedCategories, Set<Integer> breadCrumbs) {
        return breadCrumbs != null && exemptedCategories != null ? exemptedCategories.stream().anyMatch(id -> breadCrumbs.contains(id)) : false;
    }
}
