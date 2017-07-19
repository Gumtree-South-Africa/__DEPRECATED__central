package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.runtime.configadmin.ConfigurationRefreshEventListener;
import com.espertech.esper.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class EventStreamProcessor implements ConfigurationRefreshEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private static final String PROVIDER_NAME = "volumefilter_provider";
    private static final String MAIL_RECEIVED_EVENT = "MailReceivedEvent";
    private static final String VELOCITY_FIELD_VALUE = "mailAddress";
    private static final String VOLUME_NAME_PREFIX = "volume";
    private static final Pattern VOLUME_PATTERN = Pattern.compile("[- %+()]");

    private ConcurrentMap<String, List<EPStatement>> epWindows = new ConcurrentHashMap<>();
    private EPServiceProvider epServiceProvider;

    @PostConstruct
    void initialize() {
        Configuration config = new Configuration();

        config.addEventType(MAIL_RECEIVED_EVENT, MailReceivedEvent.class);

        this.epServiceProvider = EPServiceProviderManager.getProvider(PROVIDER_NAME, config);
        this.epServiceProvider.initialize();
    }

    void register(String instanceId, List<Quota> queues) {
        if (epWindows.containsKey(instanceId)) {
            LOG.warn("EP Window '{}' already exists, not creating again", instanceId);
            return;
        }

        List<EPStatement> statements = new ArrayList<>();
        for (Quota quota : queues) {
            String windowName = windowName(instanceId, quota);

            String createWindow = format("create window %s.win:time(%d min) as select `%s` from `%s`",
                    windowName, quota.getDurationMinutes(), VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);

            LOG.debug(createWindow);
            EPStatement epl = epServiceProvider.getEPAdministrator().createEPL(createWindow);
            statements.add(epl);

            String insertIntoWindow = format("insert into %s select `%s` from `%s`", windowName, VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);
            LOG.debug(insertIntoWindow);
            epServiceProvider.getEPAdministrator().createEPL(insertIntoWindow);
        }

        epWindows.put(instanceId, statements);
    }

    private String windowName(String instanceId, Quota q) {
        String sanitisedFilterName = VOLUME_PATTERN.matcher(instanceId).replaceAll("_");
        return (VOLUME_NAME_PREFIX + sanitisedFilterName + "quota" + q.getPerTimeValue() + q.getPerTimeUnit() + q.getScore()).toLowerCase();
    }

    public void mailReceivedFrom(String mailAddress) {
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(mailAddress.toLowerCase()));
    }

    long count(String mailAddress, String instanceId, Quota quota) {
        String query = format("select count(*) from %s where mailAddress = '%s'",
                windowName(instanceId, quota), mailAddress.toLowerCase());

        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery(query);
        return (Long) result.iterator().next().get("count(*)");
    }

    @Override
    public boolean isApplicable(Class<?> clazz) {
        return clazz.equals(VolumeFilterFactory.class);
    }

    @Override
    public void unregister(String instanceId) {
        epWindows.get(instanceId).stream()
                .filter(Objects::nonNull)
                .forEach(EPStatement::destroy);

        epWindows.remove(instanceId);
    }
}
