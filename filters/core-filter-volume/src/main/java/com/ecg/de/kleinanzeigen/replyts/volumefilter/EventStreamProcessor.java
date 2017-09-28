package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.runtime.configadmin.ConfigurationRefreshEventListener;
import com.espertech.esper.client.*;
import com.espertech.esper.core.service.EPServiceProviderImpl;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class EventStreamProcessor implements ConfigurationRefreshEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private static final String PROVIDER_NAME = "volumefilter_provider";
    private static final String MAIL_RECEIVED_EVENT = "MailReceivedEvent";
    private static final String VELOCITY_FIELD_VALUE = "mailAddress";

    private ConcurrentMap<Window, EsperWindowLifecycle> epWindows = new ConcurrentHashMap<>();
    private EPServiceProvider epServiceProvider;

    @PostConstruct
    void initialize() {
        Configuration config = new Configuration();

        config.addEventType(MAIL_RECEIVED_EVENT, MailReceivedEvent.class);

        this.epServiceProvider = EPServiceProviderManager.getProvider(PROVIDER_NAME, config);
        this.epServiceProvider.initialize();
    }

    void register(Collection<Window> windows) {
        for (Window window : windows) {
            if (!epWindows.containsKey(window)) {
                epWindows.computeIfAbsent(window, this::createWindowLifeCycle);
            }
        }
    }

    public Summary getSummary() {
        String[] windows = ((EPServiceProviderImpl) epServiceProvider).getNamedWindowMgmtService().getNamedWindows();
        Map<String, String> eventTypeNames = ((EPServiceProviderImpl) epServiceProvider).getConfigurationInformation().getEventTypeNames();

        List<String> internalWindows = Arrays.asList(windows);
        Collection<String> eventTypes = eventTypeNames.keySet();

        return new Summary(internalWindows, Collections.unmodifiableCollection(epWindows.keySet()), eventTypes);
    }

    void mailReceivedFrom(String mailAddress) {
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(mailAddress.toLowerCase()));
    }

    public long count(String mailAddress, Window window) {
        String query = format("select count(*) from %s where mailAddress = '%s'", window.getWindowName(), mailAddress.toLowerCase());

        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery(query);
        return (Long) result.iterator().next().get("count(*)");
    }

    Map<Window, EsperWindowLifecycle> getWindowsStatements() {
        return Collections.unmodifiableMap(epWindows);
    }

    @Override
    public boolean isApplicable(Class<?> clazz) {
        return clazz.equals(VolumeFilterFactory.class);
    }

    @Override
    public void unregister(String instanceId) {
        List<Window> windows = epWindows.entrySet().stream()
                // filter the windows with instanceId
                .filter(entry -> instanceId.equals(entry.getKey().getInstanceId()))
                // get windows to delete them later on
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        windows.stream()
                .peek(window -> LOG.debug("delete window '{}'", window.getWindowName()))
                .forEach(window -> {
                    EsperWindowLifecycle lifecycle = epWindows.remove(window);
                    if (lifecycle != null) {
                        lifecycle.destroy();
                    }
                }
        );
    }

    private EsperWindowLifecycle createWindowLifeCycle(Window window) {
        String createWindow = format("create window %s.win:time(%d min) as select `%s` from `%s`",
                window.getWindowName(), window.getQuota().getDurationMinutes(), VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);
        LOG.debug(createWindow);
        EPStatement createWindowStatement = epServiceProvider.getEPAdministrator().createEPL(createWindow);

        String insertIntoWindow = format("insert into %s select `%s` from `%s`",
                window.getWindowName(), VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);
        LOG.trace(insertIntoWindow);
        EPStatement insertIntoWindowStatement = epServiceProvider.getEPAdministrator().createEPL(insertIntoWindow);

        return new EsperWindowLifecycle(createWindowStatement, insertIntoWindowStatement);
    }

    static class EsperWindowLifecycle {
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

    public static class Summary {
        private final List<String> internalWindows;
        private final Multimap<String, Quota> instanceIdToQuota;
        private final Collection<Window> windows;
        private final Collection<String> events;

        Summary(List<String> internalWindows, Collection<Window> windows, Collection<String> events) {
            this.internalWindows = internalWindows;
            this.events = events;
            this.windows = windows;

            ImmutableListMultimap.Builder<String, Quota> builder = ImmutableListMultimap.builder();
            for (Window window : windows) {
                builder.put(window.getInstanceId(), window.getQuota());
            }
            this.instanceIdToQuota = builder.build();
        }

        public Collection<Window> getWindows() {
            return windows;
        }

        public List<String> getInternalWindows() {
            return internalWindows;
        }

        public Multimap<String, Quota> getInstanceIdToQuota() {
            return instanceIdToQuota;
        }

        public Collection<String> getEvents() {
            return events;
        }
    }
}
