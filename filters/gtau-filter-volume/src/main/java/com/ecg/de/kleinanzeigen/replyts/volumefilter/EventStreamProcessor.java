package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EventStreamProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private static final String PROVIDER_NAME = "volumefilter_provider";

    private final EPServiceProvider epServiceProvider;

    public EventStreamProcessor(List<Quota> queues) {

        Configuration config = new Configuration();
        config.addEventType("MailReceivedEvent", MailReceivedEvent.class);
        EPServiceProviderManager.getProvider(PROVIDER_NAME, config).destroy();
        epServiceProvider = EPServiceProviderManager.getProvider(PROVIDER_NAME, config);

        for (Quota quota : queues) {

            String windowName = windowName(quota);

            String createWindow = "create window " + windowName + ".win:time(" + quota.getDurationMinutes() + " min) as select `mailAddress` from `MailReceivedEvent`";
            LOG.debug(createWindow);
            epServiceProvider.getEPAdministrator().createEPL(createWindow);

            String insertIntoWindow = "insert into " + windowName + " select `mailAddress` from `MailReceivedEvent`";
            LOG.trace(insertIntoWindow);
            epServiceProvider.getEPAdministrator().createEPL(insertIntoWindow);
        }
    }

    private String windowName(Quota q) {
        return ("quota" + q.getPerTimeValue() + q.getPerTimeUnit() + q.getScore()).toLowerCase();
    }

    public void mailReceivedFrom(String mailAddress) {
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(mailAddress.toLowerCase()));
    }

    long count(String mailAddress, Quota q) {
        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery("select count(*) from " + windowName(q) + " where mailAddress = '" + mailAddress.toLowerCase() + "'");
        return (Long) result.iterator().next().get("count(*)");
    }

}
