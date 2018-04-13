package com.ecg.comaas.core.filter.volume;

import java.util.Objects;
import java.util.regex.Pattern;

public class Window {
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

    String getInstanceId() {
        return instanceId;
    }

    public Quota getQuota() {
        return quota;
    }

    public String getWindowName() {
        return windowName;
    }

    private static String name(String instanceId, Quota q) {
        String sanitisedFilterName = VOLUME_PATTERN.matcher(instanceId).replaceAll("_");
        String volumeName = String.format("%s_%s_%d%s", VOLUME_NAME_PREFIX, sanitisedFilterName, q.getPerTimeValue(), q.getPerTimeUnit());
        return volumeName.toLowerCase();
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
        return Objects.equals(windowName, window.windowName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowName);
    }
}