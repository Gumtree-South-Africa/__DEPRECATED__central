package com.ecg.replyts.core.runtime.prometheus;

import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import org.eclipse.jetty.server.AsyncContextState;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ApiResponseExporter extends HandlerWrapper {

    private Gauge apiResponseStatusCodes;
    private Summary apiResponseTimes;

    @Override
    protected void doStart() throws Exception {
        apiResponseStatusCodes = Gauge.build("api_response_status_codes", "API response HTTP status code").labelNames("status_code", "context_path").register();
        apiResponseTimes = Summary.build("api_response_times", "API Response time").quantile(0.95, 0.01).labelNames("method", "context_path").register();

        super.doStart();
    }

    @Override
    public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws IOException, ServletException {

        String contextPath = getContextPath(path);

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
                    state.addListener(new AsyncListenerImplementation(contextPath));
                }
            } else if (state.isInitial()) {
                updateResponses(request, httpResponse, start, contextPath);
            }
            // else onCompletion will handle it.
        }
    }

    private String getContextPath(String path) {
        int indexOf = path.indexOf('/', 1);
        if (indexOf == -1) return "";
        return path.substring(1, indexOf);
    }

    private void updateResponses(HttpServletRequest request, HttpServletResponse httpResponse, long start, String contextPath) {
        if (!contextPath.isEmpty()) {
            apiResponseStatusCodes.labels(Integer.toString(httpResponse.getStatus()), contextPath).inc();
            apiResponseTimes.labels(request.getMethod().toLowerCase(), contextPath).observe(System.currentTimeMillis() - start);
        }
    }

    private final class AsyncListenerImplementation implements AsyncListener {
        private final String contextPath;
        private long startTime;

        public AsyncListenerImplementation(String contextPath) {
            this.contextPath = contextPath;
        }

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
            final HttpServletRequest httpRequest = (HttpServletRequest) state.getRequest();
            final HttpServletResponse httpResponse = (HttpServletResponse) state.getResponse();
            updateResponses(httpRequest, httpResponse, startTime, contextPath);
        }
    }
}
