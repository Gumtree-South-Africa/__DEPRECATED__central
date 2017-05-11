package com.ecg.gumtree.comaas.filter.volume;

import com.ecg.replyts.core.runtime.configadmin.ConfigurationRefreshEventListener;
import com.espertech.esper.client.*;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

@Component
public class EventStreamProcessor implements ConfigurationRefreshEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private static final String PROVIDER_NAME = "gumtree_volume_filter_provider";
    private static final String MAIL_RECEIVED_EVENT = "MailReceivedEvent";
    private static final String VELOCITY_FIELD_VALUE = "volumeFieldValue";

    private EPServiceProvider epServiceProvider;

    private ConcurrentMap<String, EPStatement> epWindows = new ConcurrentHashMap<>();

    @PostConstruct
    private void initialize() {
        Configuration config = new Configuration();

        config.addEventType(MAIL_RECEIVED_EVENT, MailReceivedEvent.class);

        this.epServiceProvider = EPServiceProviderManager.getProvider(PROVIDER_NAME, config);
        this.epServiceProvider.initialize();
    }

    void register(String instanceId, VelocityFilterConfig filterConfig) {
        String windowName = windowName(instanceId);

        String createWindow = format("create window %s.win:time(%d sec) as select `%s` from `%s`",
                windowName, filterConfig.getSeconds(), VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);

        LOG.info(createWindow);
        EPStatement epl = epServiceProvider.getEPAdministrator().createEPL(createWindow);
        epWindows.put(windowName, epl);

        String insertIntoWindow = format("insert into %s select `%s` from `%s`", windowName, VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);
        LOG.info(insertIntoWindow);
        epServiceProvider.getEPAdministrator().createEPL(insertIntoWindow);
    }

    @Override
    public boolean notify(Class<?> clazz) {
        return clazz.equals(GumtreeVolumeFilterConfiguration.VolumeFilterFactory.class);
    }

    @Override
    public void unregister(String instanceId) {
        String windowName = windowName(instanceId);
        EPStatement toBeUnregistered = epWindows.get(windowName);
        if (toBeUnregistered != null) {
            toBeUnregistered.destroy();
            epWindows.remove(windowName);
        }
    }

    void mailReceivedFrom(String volumeFieldValue) {
        LOG.debug(format("received event [%s] to store internally", volumeFieldValue));
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(volumeFieldValue.toLowerCase()));
    }

    long count(String volumeFieldValue, String instanceId) {
        String query = format("select count(*) from %s where %s = '%s'",
                windowName(instanceId), VELOCITY_FIELD_VALUE, volumeFieldValue.toLowerCase());
        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery(query);

        return (Long) result.iterator().next().get("count(*)");
    }

    private String windowName(String instanceId) {
        String sanitisedFilterName = instanceId.replaceAll(" ", "_").replaceAll("-", "_");
        return ("volume" + sanitisedFilterName).toLowerCase();
    }
}
