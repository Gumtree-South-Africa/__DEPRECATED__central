package com.ecg.messagebox.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecg.messagebox.labs.LabsTesting.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Configuration class that controls for who is the new model enabled and used.
 * <p>
 * It supports both percentage range (using MurmurHash3 hashing of userId) and specific user ids.
 * </p>
 */
@Component
public class NewModelConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewModelConfiguration.class);

    private int[] modelEnabledRange;
    private List<String> modelEnabledUserIds;

    private int[] useModelRange;
    private List<String> useModelUserIds;

    @Autowired
    public NewModelConfiguration(
            @Value("${messagebox.newModel.enabled.range:}") String modelEnabledRangeStr,
            @Value("${messagebox.newModel.enabled.userIds:}") String modelEnabledUserIdsStr,
            @Value("${messagebox.useNewModel.range:}") String useModelRangeStr,
            @Value("${messagebox.useNewModel.userIds:}") String useModelUserIdsStr) {

        if (isNotBlank(modelEnabledRangeStr)) {
            modelEnabledRange = getRange(modelEnabledRangeStr);
            LOGGER.info("New model is enabled for % range: {}-{}", modelEnabledRange[0], modelEnabledRange[1]);
        }

        if (isNotBlank(modelEnabledUserIdsStr)) {
            modelEnabledUserIds = getUserIds(modelEnabledUserIdsStr);
            LOGGER.info("New model is enabled for users: {}", String.join(", ", modelEnabledUserIds));
        }

        if (isNotBlank(useModelRangeStr)) {
            useModelRange = getRange(useModelRangeStr);
            LOGGER.info("Use new model is enabled for % range: {}-{}", useModelRange[0], useModelRange[1]);
        }

        if (isNotBlank(useModelUserIdsStr)) {
            useModelUserIds = getUserIds(useModelUserIdsStr);
            LOGGER.info("Use new model is enabled for users: {}", String.join(", ", useModelUserIds));
        }
    }

    public boolean newModelEnabled(String userId) {
        boolean newModelEnabled = false;
        if (modelEnabledRange != null) {
            newModelEnabled = matches(userId, modelEnabledRange);
        }
        if (!newModelEnabled && modelEnabledUserIds != null) {
            newModelEnabled = modelEnabledUserIds.contains(userId);
        }
        return newModelEnabled;
    }

    public boolean useNewModel(String userId) {
        boolean useNewModel = false;
        if (useModelRange != null) {
            useNewModel = matches(userId, useModelRange);
        }
        if (!useNewModel && useModelUserIds != null) {
            useNewModel = useModelUserIds.contains(userId);
        }
        return useNewModel;
    }
}