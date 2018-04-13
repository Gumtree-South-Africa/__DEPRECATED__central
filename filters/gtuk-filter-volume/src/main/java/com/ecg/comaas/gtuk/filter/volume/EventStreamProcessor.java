package com.ecg.comaas.gtuk.filter.volume;

import com.ecg.replyts.core.runtime.configadmin.ConfigurationRefreshEventListener;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
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

import static java.lang.String.format;

@Component
    public class EventStreamProcessor implements ConfigurationRefreshEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private static final String PROVIDER_NAME = "gumtree_volume_filter_provider";
    private static final String MAIL_RECEIVED_EVENT = "MailReceivedEvent";
    private static final String VELOCITY_FIELD_VALUE = "volumeFieldValue";
    static final String VOLUME_NAME_PREFIX = "volume";

    private EPServiceProvider epServiceProvider;

    private final Map<String, EsperWindowLifecycle> epWindowLifecycles = new ConcurrentHashMap<>();
    private final Map<String, VelocityFilterConfig> velocityFilterConfigs = new ConcurrentHashMap<>();

    @PostConstruct
    private void initialize() {
        Configuration config = new Configuration();

        config.addEventType(MAIL_RECEIVED_EVENT, MailReceivedEvent.class);

        this.epServiceProvider = EPServiceProviderManager.getProvider(PROVIDER_NAME, config);
        this.epServiceProvider.initialize();
    }

    void register(String instanceId, VelocityFilterConfig filterConfig) {
        String windowName = windowName(instanceId);
        if (epWindowLifecycles.containsKey(windowName)) {
            LOG.info("EP Window '{}' already exists, not creating again", windowName);
            return;
        }

        this.velocityFilterConfigs.put(instanceId, filterConfig);
        epWindowLifecycles.put(windowName, createWindow(instanceId, filterConfig));
    }

    @Override
    public boolean isApplicable(String identifier) {
        return identifier.equals(GumtreeVolumeFilterFactory.IDENTIFIER);
    }

    @Override
    public void unregister(String instanceId) {
        String windowName = windowName(instanceId);
        EsperWindowLifecycle lifecycle = epWindowLifecycles.remove(windowName);
        if (lifecycle != null) {
            lifecycle.destroy();
            velocityFilterConfigs.remove(instanceId);
        }
    }

    private EsperWindowLifecycle createWindow(String instanceId, VelocityFilterConfig filterConfig) {
        String windowName = windowName(instanceId);
        String createWindow = format("create window %s.win:time(%d sec) as select `%s` from `%s`",
                windowName, filterConfig.getSeconds(), VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);
        LOG.debug(createWindow);
        EPStatement createWindowStatement = epServiceProvider.getEPAdministrator().createEPL(createWindow);
        String insertIntoWindow = format("insert into %s select `%s` from `%s`", windowName,
                VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);
        LOG.trace(insertIntoWindow);
        EPStatement insertIntoWindowStatement = epServiceProvider.getEPAdministrator().createEPL(insertIntoWindow);
        return new EsperWindowLifecycle(createWindowStatement, insertIntoWindowStatement);
    }

    private static class EsperWindowLifecycle {
        private final EPStatement createWindowStatement;
        private final EPStatement insertIntoWindowStatement;

        EsperWindowLifecycle(EPStatement createWindowStatement, EPStatement insertIntoWindowStatement) {
            this.createWindowStatement = createWindowStatement;
            this.insertIntoWindowStatement = insertIntoWindowStatement;
        }

        void destroy() {
            if (!this.createWindowStatement.isDestroyed()) {
                this.createWindowStatement.destroy();
            }
            if (!this.insertIntoWindowStatement.isDestroyed()) {
                this.insertIntoWindowStatement.destroy();
            }
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
        LOG.trace("received event [{}] to store internally", volumeFieldValue);
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(volumeFieldValue.toLowerCase()));
    }

    public long count(String volumeFieldValue, String instanceId) {
        String query = format("select count(*) from %s where %s = '%s'", windowName(instanceId), VELOCITY_FIELD_VALUE, volumeFieldValue.toLowerCase());
        LOG.trace("esper count query: {}", query);
        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery(query);

        EventBean eventBean = result.iterator().next();
        LOG.trace("query result: {}", eventBean);
        return (Long) eventBean.get("count(*)");
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
