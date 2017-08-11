package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Time window which is composed by filter INSTANCE_ID and QUOTA.
 */
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

    public String getInstanceId() {
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
        String volumeName = String.format("%s_%s_quota%d%s%d", VOLUME_NAME_PREFIX, sanitisedFilterName, q.getPerTimeValue(), q.getPerTimeUnit(), q.getScore());
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
        return Objects.equals(instanceId, window.instanceId) &&
                Objects.equals(quota, window.quota);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, quota);
    }
}