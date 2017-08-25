package com.ecg.de.mobile.replyts.uniqueid;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableSet;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * User: beckart
 */
class MailUniqueIdPostProcessor implements PostProcessor {
    private UniqueIdGenerator uniqueIdGenerator;

    private final Set<String> ignoredEmailAddresses;

    private final Integer order;


    MailUniqueIdPostProcessor(UniqueIdGenerator uniqueIdGenerator, String ignoreEmailAddressDelimitedList, String order) {
        this.uniqueIdGenerator = uniqueIdGenerator;

        this.ignoredEmailAddresses = delimitedListToSet(ignoreEmailAddressDelimitedList);

        this.order = Integer.parseInt(order);

    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {


        new MailUniqueIdHandler(messageProcessingContext, uniqueIdGenerator, ignoredEmailAddresses).handle();
    }

    private Set<String> delimitedListToSet(String ignoreEmailAddressDelimitedList) {

        Set<String> resultSet = new HashSet<String>();

        for(String ignoredMailAddress : ignoreEmailAddressDelimitedList.split(",")) {
            if(StringUtils.hasText(ignoredMailAddress)) {
                resultSet.add(ignoredMailAddress.trim().toLowerCase());
            }
        }

        return ImmutableSet.copyOf(resultSet);

    }

    Set<String> getIgnoredEmailAddresses() {
        return ignoredEmailAddresses;
    }
}
