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
 * Configuration class that controls for who is the diff tool enabled.
 * <p>
 * It supports both percentage range (using MurmurHash3 hashing of userId) and specific user ids.
 * </p>
 */
@Component
public class DiffConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiffConfiguration.class);

    private int[] range;
    private List<String> userIds;

    @Autowired
    public DiffConfiguration(
            @Value("${messagebox.useDiff.range:}") String rangeStr,
            @Value("${messagebox.useDiff.userIds:}") String userIdsStr) {

        if (isNotBlank(rangeStr)) {
            range = getRange(rangeStr);
            LOGGER.info("Diff tool is enabled for % range: {}-{}", range[0], range[1]);
        }

        if (isNotBlank(userIdsStr)) {
            userIds = getUserIds(userIdsStr);
            LOGGER.info("Diff tool is enabled for users: {}", String.join(", ", userIds));
        }
    }

    public boolean useDiff(String userId) {
        boolean useDiff = false;
        if (range != null) {
            useDiff = matches(userId, range);
        }
        if (!useDiff && userIds != null) {
            useDiff = userIds.contains(userId);
        }
        return useDiff;
    }
}