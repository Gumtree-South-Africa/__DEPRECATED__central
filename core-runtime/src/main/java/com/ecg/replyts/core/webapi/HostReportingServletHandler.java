package com.ecg.replyts.core.webapi;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
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
    private static final String HEADER_X_REQUEST_TIME = "X-REQUEST-TIME";
    private static final DateTimeFormatter ISO_8604_WITH_MILLIS = new DateTimeFormatterBuilder().appendInstant(3)
            .toFormatter();

    private final MetricRegistry metricRegistry;

    private String name;

    // the requests handled by this handler, excluding active
    private Timer requests;

    // the request delays handled by this handler, based on the X-Request-Time
    // header, excluding active
    private Timer requestDelays;

    // the number of dispatches seen by this handler, excluding active
    private Timer dispatches;

    // the number of active requests
    private Counter activeRequests;

    // the number of active dispatches
    private Counter activeDispatches;

    // the number of requests currently suspended.
    private Counter activeSuspended;

    // the number of requests that have been asynchronously dispatched
    private Meter asyncDispatches;

    // the number of requests that expired while suspended
    private Meter asyncTimeouts;

    private Meter[] responses;

    private Map<HttpMethod, Timer> httpMethodRequests;
    private Timer otherRequests;

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

        final String prefix = name(TimingReports.getHostName(), getHandler().getClass().getName(), name);

        this.requests = metricRegistry.timer(name(prefix, "requests"));
        this.requestDelays = metricRegistry.timer(name(prefix, "request-delays"));
        this.dispatches = metricRegistry.timer(name(prefix, "dispatches"));

        this.activeRequests = metricRegistry.counter(name(prefix, "active-requests"));
        this.activeDispatches = metricRegistry.counter(name(prefix, "active-dispatches"));
        this.activeSuspended = metricRegistry.counter(name(prefix, "active-suspended"));

        this.asyncDispatches = metricRegistry.meter(name(prefix, "async-dispatches"));
        this.asyncTimeouts = metricRegistry.meter(name(prefix, "async-timeouts"));

        this.responses = new Meter[] { metricRegistry.meter(name(prefix, "1xx-responses")), // 1xx
                metricRegistry.meter(name(prefix, "2xx-responses")), // 2xx
                metricRegistry.meter(name(prefix, "3xx-responses")), // 3xx
                metricRegistry.meter(name(prefix, "4xx-responses")), // 4xx
                metricRegistry.meter(name(prefix, "5xx-responses")) // 5xx
        };

        httpMethodRequests = Maps.toMap(
            Arrays.asList(HttpMethod.values()),
            httpMethod -> metricRegistry.timer(name(prefix, httpMethod.toString().toLowerCase() + "-requests")));

        this.otherRequests = metricRegistry.timer(name(prefix, "other-requests"));

        metricRegistry.register(name(prefix, "percent-4xx-1m"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(responses[3].getOneMinuteRate(), requests.getOneMinuteRate());
            }
        });

        metricRegistry.register(name(prefix, "percent-4xx-5m"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(responses[3].getFiveMinuteRate(), requests.getFiveMinuteRate());
            }
        });

        metricRegistry.register(name(prefix, "percent-4xx-15m"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(responses[3].getFifteenMinuteRate(), requests.getFifteenMinuteRate());
            }
        });

        metricRegistry.register(name(prefix, "percent-5xx-1m"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(responses[4].getOneMinuteRate(), requests.getOneMinuteRate());
            }
        });

        metricRegistry.register(name(prefix, "percent-5xx-5m"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(responses[4].getFiveMinuteRate(), requests.getFiveMinuteRate());
            }
        });

        metricRegistry.register(name(prefix, "percent-5xx-15m"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(responses[4].getFifteenMinuteRate(), requests.getFifteenMinuteRate());
            }
        });

        this.listener = new AsyncListener() {
            private long startTime;

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                asyncTimeouts.mark();
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
                if (state.getHttpChannelState().getState() != HttpChannelState.State.DISPATCHED) {
                    activeSuspended.dec();
                }
            }
        };
    }

    @Override
    public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws IOException, ServletException {

        activeDispatches.inc();

        final long start;
        final HttpChannelState state = request.getHttpChannelState();
        if (state.isInitial()) {
            // new request
            activeRequests.inc();
            start = request.getTimeStamp();
        } else {
            // resumed request
            start = System.currentTimeMillis();
            activeSuspended.dec();
            if (state.getState() == HttpChannelState.State.DISPATCHED) {
                asyncDispatches.mark();
            }
        }

        try {
            super.handle(path, request, httpRequest, httpResponse);
        } finally {
            final long now = System.currentTimeMillis();
            final long dispatched = now - start;

            activeDispatches.dec();
            dispatches.update(dispatched, TimeUnit.MILLISECONDS);

            if (state.isSuspended()) {
                if (state.isInitial()) {
                    state.addListener(listener);
                }
                activeSuspended.inc();
            } else if (state.isInitial()) {
                updateResponses(httpRequest, httpResponse, start);
            }
            // else onCompletion will handle it.
        }
    }

    private void updateResponses(HttpServletRequest request, HttpServletResponse response, long start) {
        final int responseStatus = response.getStatus() / 100;
        if (responseStatus >= 1 && responseStatus <= 5) {
            responses[responseStatus - 1].mark();
        }
        activeRequests.dec();
        final long elapsedTime = System.currentTimeMillis() - start;
        HttpMethod httpMethod = HttpMethod.fromString(request.getMethod());
        updateRequestsMetric(elapsedTime);
        updateHttpMethodRequestsMetric(httpMethod, elapsedTime);
        updateRequestDelayMetric(request);
    }

    private void updateRequestsMetric(long elapsedTime) {
        requests.update(elapsedTime, TimeUnit.MILLISECONDS);
    }

    private void updateHttpMethodRequestsMetric(HttpMethod httpMethod, long elapsedTime) {
        Timer timer = httpMethodRequests.getOrDefault(httpMethod, otherRequests);
        timer.update(elapsedTime, TimeUnit.MILLISECONDS);
    }

    private void updateRequestDelayMetric(HttpServletRequest request) {
        String requestTimeHeader = request.getHeader(HEADER_X_REQUEST_TIME);
        if (requestTimeHeader == null) {
            return;
        }
        try {
            Instant requestTime = Instant.from(ISO_8604_WITH_MILLIS.parse(requestTimeHeader));
            Duration requestTimeDelay = Duration.between(requestTime, Instant.now());
            requestDelays.update(requestTimeDelay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (DateTimeParseException dateTimeParseException) {
            LOG.warn(
                "Request header {} malformed: {}",
                HEADER_X_REQUEST_TIME,
                requestTimeHeader,
                dateTimeParseException);
        }
    }
}
