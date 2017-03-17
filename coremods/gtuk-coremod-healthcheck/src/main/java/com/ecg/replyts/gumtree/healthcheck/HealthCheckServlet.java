package com.ecg.replyts.gumtree.healthcheck;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.ecg.replyts.core.runtime.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletConfigAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.codahale.metrics.MetricRegistry.name;

@Component
public class HealthCheckServlet implements ServletConfigAware {
    private AdminServlet servletInstance;

    @Autowired
    private HealthCheckProvider healthCheckProvider;

    @Override
    public void setServletConfig(ServletConfig servletConfig) {
        MetricRegistry metricRegistry = MetricsService.getInstance().getRegistry();

        metricRegistry.register(name("jvm", "gc"), new GarbageCollectorMetricSet());
        metricRegistry.register(name("jvm", "memory"), new MemoryUsageGaugeSet());
        metricRegistry.register(name("jvm", "thread-states"), new ThreadStatesGaugeSet());
        metricRegistry.register(name("jvm", "fd", "usage"), new FileDescriptorRatioGauge());

        final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

        for (HealthCheckProvider.NamedHealthCheck namedHealthCheck : healthCheckProvider.getHealthChecks()) {
            healthCheckRegistry.register(namedHealthCheck.getName(), namedHealthCheck.getHealthCheck());
        }

        final ServletContext servletContext = servletConfig.getServletContext();

        servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        servletContext.setAttribute(com.codahale.metrics.servlets.HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);

        try {
            servletInstance = AdminServlet.class.newInstance();
            servletInstance.init(servletConfig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Handler for all requests, delegating them to the AdminServlet class provided by the metrics library.
     * 
     * @param request the request
     * @param response the response
     * @throws Exception if something goes wrong
     */
    @RequestMapping("/*")
    public final void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        servletInstance.service(request, response);
    }
}
