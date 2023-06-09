package com.ecg.replyts.core.runtime;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public final class TimingReports {

    private static final Logger LOG = LoggerFactory.getLogger(TimingReports.class);

    private static String hostName;

    static {
        try {
            if (System.getenv().containsKey("NOMAD_JOB_NAME")) {
                hostName = String.format("%s_%s_%s",
                        System.getenv("NOMAD_JOB_NAME"),
                        System.getenv("NOMAD_GROUP_NAME"),
                        System.getenv("NOMAD_ALLOC_INDEX"));
            } else {
                hostName = InetAddress.getLocalHost().getCanonicalHostName().replaceAll("\\.", "_");
            }
        } catch (UnknownHostException e) {
            LOG.error("can not get local host name ", e);
        } finally {
            if (hostName == null) {
                hostName = "localhost";
            }
        }
    }

    private TimingReports() {
    }

    public static String getHostName() {
        return hostName;
    }

    private static final Map<ConfigurationId, Timer> PLUGIN_TIMERS = Maps.newConcurrentMap();


    /**
     * Returns a new timer factory that can be analyzed using JMX. Time. Will measure the number of invocations per second and the duration per invocation in milliseonds.
     */
    public static Timer newTimer(String timer) {
        return MetricsService.getInstance().timer(metricName(timer));
    }

    public static Counter newCounter(String name) {
        return MetricsService.getInstance().counter(metricName(name));
    }

    public static Histogram newHistogram(String name) {
        return MetricsService.getInstance().histogram(metricName(name));
    }

    public static void newGauge(String gaugeName, Gauge gauge) {
        try {
            String metric = metricName(gaugeName);

            if (MetricsService.getInstance().getRegistry().getGauges((n, g) -> n.equals(metric)).size() == 0) {
                MetricsService.getInstance().getRegistry().register(metric, gauge);
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("can not register gauge " + gaugeName, e);
        }
    }

    public static Timer newOrExistingTimerFor(ConfigurationId plugin) {
        if (!PLUGIN_TIMERS.containsKey(plugin)) {
            synchronized (PLUGIN_TIMERS) {
                if (!PLUGIN_TIMERS.containsKey(plugin)) { // NOSONAR
                    String titleKey = "plugin." + plugin.getPluginFactory() + "." + plugin.getInstanceId();
                    titleKey = titleKey.replaceAll("[^a-zA-Z0-9-]", "-");
                    PLUGIN_TIMERS.put(plugin, newTimer(titleKey));
                }
            }
        }
        return PLUGIN_TIMERS.get(plugin);
    }

    private static String metricName(String name) {
        List<String> strings = Lists.newArrayList(Splitter.on('.').split(name));
        strings.add(hostName);
        return MetricRegistry.name("reports", strings.toArray(new String[]{}));
    }

}
