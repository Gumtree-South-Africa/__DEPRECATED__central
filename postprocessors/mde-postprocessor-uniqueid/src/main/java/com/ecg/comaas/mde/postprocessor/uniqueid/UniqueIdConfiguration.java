package com.ecg.comaas.mde.postprocessor.uniqueid;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@ComaasPlugin
@Configuration
public class UniqueIdConfiguration {

    @Value("${replyts.uniqueid.pepper:wz1239yjdsfhqyOEedd}")
    private String uniquePepper;

    @Value("${replyts.uniqueid.ignoredBuyerAddresses:}")
    private String ignoredBuyerAddresses;

    @Value("${replyts.uniqueid.order}")
    private int pluginOrder;

    @Bean
    public UniqueIdPostProcessor uniqueIdPostProcessor() {
        return new UniqueIdPostProcessor(new UniqueIdGenerator(uniquePepper), delimitedListToSet(ignoredBuyerAddresses), pluginOrder);
    }

    private Set<String> delimitedListToSet(String ignoreEmailAddressDelimitedList) {
        Set<String> resultSet = new HashSet<>();
        for (String ignoredMailAddress : ignoreEmailAddressDelimitedList.split(",")) {
            if (StringUtils.hasText(ignoredMailAddress)) {
                resultSet.add(ignoredMailAddress.trim().toLowerCase());
            }
        }

        return ImmutableSet.copyOf(resultSet);
    }
}
