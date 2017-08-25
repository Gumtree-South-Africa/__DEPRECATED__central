package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.espertech.esper.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EventStreamProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private final EPServiceProvider epServiceProvider;
    private final String name;
    private final Configuration config;

    public EventStreamProcessor(final String name, List<Quota> queues) {
        this.name = name;
        this.config = new Configuration();

        config.addEventType("MailReceivedEvent", MailReceivedEvent.class);
        epServiceProvider = EPServiceProviderManager.getProvider(this.name, config);


        for (Quota quota : queues) {

            String windowName = windowName(quota);


            //private static final String CREATE_WIN = "create window ${windowName}.win:time(${windowLength}) as select `${entityField}` from `${eventType}`";
            String createWindow = "create window " + windowName + ".win:time(" + quota.getDurationSeconds() + " sec) as select `mailAddress` from `MailReceivedEvent`";
            LOG.debug(createWindow);
            epServiceProvider.getEPAdministrator().createEPL(createWindow);

            String insertIntoWindow = "insert into " + windowName + " select `mailAddress` from `MailReceivedEvent`";
            LOG.debug(insertIntoWindow);
            epServiceProvider.getEPAdministrator().createEPL(insertIntoWindow);

        }

    }

    // This should really be replaced with a proper unload() method, but this
    // requires core to call it when destroying filter instances. No support
    // for this yet.
    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        EPServiceProviderManager.getProvider(name, config).destroy();
    }

    private String windowName(Quota q) {
        return ("quota" + q.getPerTimeValue() + q.getPerTimeUnit() + q.getScore()).toLowerCase();
    }

    public void mailReceivedFrom(String mailAddress) {
        LOG.trace("register a received mail occurrence from '{}'", mailAddress);
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(mailAddress.toLowerCase()));
    }

    long count(String mailAddress, Quota q) {
        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery("select count(*) from " + windowName(q) + " where mailAddress = '" + mailAddressForEsper(mailAddress) + "'");
        return (Long) result.iterator().next().get("count(*)");
    }

    private String mailAddressForEsper(String mailAddress) {
        return mailAddress.toLowerCase().replace("'", "\\'");
    }
}
