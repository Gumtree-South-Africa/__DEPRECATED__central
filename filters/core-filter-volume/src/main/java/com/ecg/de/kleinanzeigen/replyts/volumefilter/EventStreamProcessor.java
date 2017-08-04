package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.runtime.configadmin.ConfigurationRefreshEventListener;
import com.espertech.esper.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class EventStreamProcessor implements ConfigurationRefreshEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(EventStreamProcessor.class);

    private static final String PROVIDER_NAME = "volumefilter_provider";
    private static final String MAIL_RECEIVED_EVENT = "MailReceivedEvent";
    private static final String VELOCITY_FIELD_VALUE = "mailAddress";

    private ConcurrentMap<Window, EPStatement> epWindows = new ConcurrentHashMap<>();
    private EPServiceProvider epServiceProvider;

    @PostConstruct
    void initialize() {
        Configuration config = new Configuration();

        config.addEventType(MAIL_RECEIVED_EVENT, MailReceivedEvent.class);

        this.epServiceProvider = EPServiceProviderManager.getProvider(PROVIDER_NAME, config);
        this.epServiceProvider.initialize();
    }

    void register(String instanceId, List<Quota> quotas) {
        for (Quota quota : quotas) {
            Window window = new Window(instanceId, quota);

            if (epWindows.containsKey(window)) {
                LOG.warn("EP Window '{}' already exists, not creating again", window);
                return;
            }

            String createWindow = format("create window %s.win:time(%d min) as select `%s` from `%s`",
                    window, quota.getDurationMinutes(), VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);

            LOG.debug(createWindow);
            EPStatement statement = epServiceProvider.getEPAdministrator().createEPL(createWindow);

            String insertIntoWindow = format("insert into %s select `%s` from `%s`", window, VELOCITY_FIELD_VALUE, MAIL_RECEIVED_EVENT);
            LOG.debug(insertIntoWindow);
            epServiceProvider.getEPAdministrator().createEPL(insertIntoWindow);

            epWindows.putIfAbsent(window, statement);
        }
    }

    public void mailReceivedFrom(String mailAddress) {
        epServiceProvider.getEPRuntime().sendEvent(new MailReceivedEvent(mailAddress.toLowerCase()));
    }

    long count(String mailAddress, String instanceId, Quota quota) {
        String query = format("select count(*) from %s where mailAddress = '%s'",
                Window.name(instanceId, quota), mailAddress.toLowerCase());

        EPOnDemandQueryResult result = epServiceProvider.getEPRuntime().executeQuery(query);
        return (Long) result.iterator().next().get("count(*)");
    }

    Map<Window, EPStatement> getWindows() {
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
                .filter(entry -> instanceId.equals(entry.getKey().instanceId))
                // destroy Statements belonging to the filtered windows
                .peek(entry -> entry.getValue().destroy())
                // get windows to delete them later on
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        windows.forEach(epWindows::remove);
    }

    /**
     * Time window which is composed by filter INSTANCE_ID and QUOTA.
     */
    static class Window {

        private static final String VOLUME_NAME_PREFIX = "volume";
        private static final Pattern VOLUME_PATTERN = Pattern.compile("[- %+()]");

        private final String instanceId;
        private final Quota quota;
        private final String windowName;

        Window(String instanceId, Quota quota) {
            this.instanceId = instanceId;
            this.quota = quota;
            this.windowName = name(instanceId, quota);
        }

        public String getInstanceId() {
            return instanceId;
        }

        private static String name(String instanceId, Quota q) {
            String sanitisedFilterName = VOLUME_PATTERN.matcher(instanceId).replaceAll("_");
            return (VOLUME_NAME_PREFIX + sanitisedFilterName + "quota" + q.getPerTimeValue() + q.getPerTimeUnit() + q.getScore()).toLowerCase();
        }

        @Override
        public String toString() {
            return windowName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Window window = (Window) o;
            return Objects.equals(instanceId, window.instanceId) &&
                    Objects.equals(quota, window.quota);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instanceId, quota);
        }
    }
}
