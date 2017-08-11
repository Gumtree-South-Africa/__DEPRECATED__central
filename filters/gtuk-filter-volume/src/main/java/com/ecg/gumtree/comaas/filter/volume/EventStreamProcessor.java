package com.ecg.gumtree.comaas.filter.volume;

import com.ecg.replyts.core.runtime.configadmin.ConfigurationRefreshEventListener;
import com.espertech.esper.client.*;
import com.espertech.esper.core.service.EPServiceProviderImpl;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

@Component
public class EventStreamProcessor implements ConfigurationRefreshEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private static final String PROVIDER_NAME = "gumtree_volume_filter_provider";
    private static final String MAIL_RECEIVED_EVENT = "MailReceivedEvent";
    private static final String VELOCITY_FIELD_VALUE = "volumeFieldValue";
    static final String VOLUME_NAME_PREFIX = "volume";

    private EPServiceProvider epServiceProvider;

    private ConcurrentMap<String, EPStatement> epWindows = new ConcurrentHashMap<>();
    private Map<String, VelocityFilterConfig> velocityFilterConfigs = new ConcurrentHashMap<>();

    @PostConstruct
    private void initialize() {
        Configuration config = new Configuration();

        config.addEventType(MAIL_RECEIVED_EVENT, MailReceivedEvent.class);

        this.epServiceProvider = EPServiceProviderManager.getProvider(PROVIDER_NAME, config);
        this.epServiceProvider.initialize();
    }

    void register(String instanceId, VelocityFilterConfig filterConfig) {
        String windowName = windowName(instanceId);
        if (epWindows.containsKey(windowName)) {
            LOG.info("EP Window '{}' already exists, not creating again", windowName);
            return;
        }

        this.velocityFilterConfigs.put(instanceId, filterConfig);
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
    public boolean isApplicable(Class<?> clazz) {
        return clazz.equals(GumtreeVolumeFilterConfiguration.VolumeFilterFactory.class);
    }

    @Override
    public void unregister(String instanceId) {
        String windowName = windowName(instanceId);
        EPStatement toBeUnregistered = epWindows.get(windowName);
        if (toBeUnregistered != null) {
            toBeUnregistered.destroy();
            epWindows.remove(windowName);
            velocityFilterConfigs.remove(instanceId);
        }
    }

    public Summary getSummary() {
        String[] windows = ((EPServiceProviderImpl) epServiceProvider).getNamedWindowMgmtService().getNamedWindows();
        Map<String, String> eventTypeNames = ((EPServiceProviderImpl) epServiceProvider).getConfigurationInformation().getEventTypeNames();

        List<String> internalWindows = Arrays.asList(windows);
        List<String> eventTypes = new ArrayList<>(eventTypeNames.keySet());

        return new Summary(internalWindows, eventTypes, velocityFilterConfigs);
    }

    void mailReceivedFrom(String volumeFieldValue) {
        LOG.debug(format("received event [%s] to store internally", volumeFieldValue));
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(volumeFieldValue.toLowerCase()));
    }

    public long count(String volumeFieldValue, String instanceId) {
        String query = format("select count(*) from %s where %s = '%s'", windowName(instanceId), VELOCITY_FIELD_VALUE, volumeFieldValue.toLowerCase());
        LOG.trace("esper count query: {}", query);
        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery(query);

        LOG.trace("query result: {}", result.iterator().next());
        return (Long) result.iterator().next().get("count(*)");
    }

    String windowName(String instanceId) {
        String sanitisedFilterName = instanceId.replaceAll("[- %+()]", "_");
        return (VOLUME_NAME_PREFIX + sanitisedFilterName).toLowerCase();
    }

    public static class Summary {

        private final List<String> internalWindows;
        private final List<String> events;
        private final Map<String, VelocityFilterConfig> velocityFilterConfigs;

        Summary(List<String> internalWindows, List<String> events, Map<String, VelocityFilterConfig> velocityFilterConfigs) {
            this.internalWindows = internalWindows;
            this.events = events;
            this.velocityFilterConfigs = velocityFilterConfigs;
        }

        public List<String> getInternalWindows() {
            return internalWindows;
        }

        public List<String> getEvents() {
            return events;
        }

        public Map<String, VelocityFilterConfig> getVelocityFilterConfigs() {
            return velocityFilterConfigs;
        }
    }
}
