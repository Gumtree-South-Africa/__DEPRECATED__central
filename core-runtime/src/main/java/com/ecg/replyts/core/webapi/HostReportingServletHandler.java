package com.ecg.replyts.core.webapi;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Maps;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.AsyncContextState;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This is slightly modified copy of a metrics InstrumentedHandler Jetty
 * https://raw.githubusercontent.com/dropwizard/metrics/3.2-development/metrics-jetty9/src/main/java/com/codahale/metrics/jetty9/InstrumentedHandler.java
 */
public class HostReportingServletHandler extends HandlerWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(HostReportingServletHandler.class);
    private static final String HEADER_X_REQUEST_TIMESTAMP = "X-REQUEST-TIMESTAMP";
    private static final DateTimeFormatter ISO_8604_WITH_MILLIS = new DateTimeFormatterBuilder().appendInstant(3)
            .toFormatter();

    private final MetricRegistry metricRegistry;

    private String name;

    // the requests handled by this handler, excluding active
    private Timer requests;

    // the request delays handled by this handler, based on the X-Request-Timestamp
    // header, excluding active
    private Timer requestDelays;

    private Counter[] responses;

    private Map<HttpMethod, Timer> httpMethodRequests;

    private AsyncListener listener;

    /**
     * Create a new instrumented handler using a given metrics registry.
     *
     * @param registry
     *            the registry for the metrics
     */
    public HostReportingServletHandler(MetricRegistry registry) {
        this.metricRegistry = registry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final String prefix = name(TimingReports.getHostName(), getHandler().getClass().getSimpleName(), name);

        this.requests = metricRegistry.timer(name(prefix, "requests"));
        this.requestDelays = metricRegistry.timer(name(prefix, "request-delays"));

        this.responses = new Counter[] {
            null,
            metricRegistry.counter(name(prefix, "2xx-responses")), // 2xx
            null,
            metricRegistry.counter(name(prefix, "4xx-responses")), // 4xx
            metricRegistry.counter(name(prefix, "5xx-responses")) // 5xx
        };

        this.httpMethodRequests = Maps.toMap(
            Arrays.asList(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE),
            httpMethod -> metricRegistry.timer(name(prefix, httpMethod.toString().toLowerCase() + "-requests")));

        this.listener = new AsyncListener() {
            private long startTime;

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                startTime = System.currentTimeMillis();
                event.getAsyncContext().addListener(this);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                final AsyncContextState state = (AsyncContextState) event.getAsyncContext();
                final HttpServletRequest request = (HttpServletRequest) state.getRequest();
                final HttpServletResponse response = (HttpServletResponse) state.getResponse();
                updateResponses(request, response, startTime);
            }
        };
    }

    @Override
    public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws IOException, ServletException {

        final long start;
        final HttpChannelState state = request.getHttpChannelState();
        if (state.isInitial()) {
            // new request
            start = request.getTimeStamp();
        } else {
            // resumed request
            start = System.currentTimeMillis();
        }

        try {
            super.handle(path, request, httpRequest, httpResponse);
        } finally {
            if (state.isSuspended()) {
                if (state.isInitial()) {
                    state.addListener(listener);
                }
            } else if (state.isInitial()) {
                updateResponses(httpRequest, httpResponse, start);
            }
            // else onCompletion will handle it.
        }
    }

    private void updateResponses(HttpServletRequest request, HttpServletResponse response, long start) {
        final int responseStatus = response.getStatus() / 100;
        if (responseStatus >= 1 && responseStatus <= 5) {
            Counter counter = responses[responseStatus - 1];
            if (counter != null) {
                counter.inc();
            }
        }
        final long elapsedTime = System.currentTimeMillis() - start;
        updateRequestsMetric(elapsedTime);
        updateHttpMethodRequestsMetric(HttpMethod.fromString(request.getMethod()), elapsedTime);
        updateRequestDelayMetric(request);
    }

    private void updateRequestsMetric(long elapsedTime) {
        requests.update(elapsedTime, TimeUnit.MILLISECONDS);
    }

    private void updateHttpMethodRequestsMetric(HttpMethod httpMethod, long elapsedTime) {
        Timer timer = httpMethodRequests.get(httpMethod);
        if (timer != null) {
            timer.update(elapsedTime, TimeUnit.MILLISECONDS);
        }
    }

    private void updateRequestDelayMetric(HttpServletRequest request) {
        String requestTimestampHeader = request.getHeader(HEADER_X_REQUEST_TIMESTAMP);
        if (requestTimestampHeader == null) {
            return;
        }
        try {
            Instant requestTime = Instant.from(ISO_8604_WITH_MILLIS.parse(requestTimestampHeader));
            Duration requestTimeDelay = Duration.between(requestTime, Instant.now());
            requestDelays.update(requestTimeDelay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (DateTimeParseException dateTimeParseException) {
            LOG.warn(
                "Request header {} malformed: {}",
                HEADER_X_REQUEST_TIMESTAMP,
                requestTimestampHeader,
                dateTimeParseException);
        }
    }
}
