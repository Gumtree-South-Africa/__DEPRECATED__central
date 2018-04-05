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

    private final AsyncListener listener = new AsyncListenerImplementation();

    private Gauge apiResponseStatusCodes;
    private Summary apiResponseTimes;

    @Override
    protected void doStart() throws Exception {
        apiResponseStatusCodes = Gauge.build("api_response_status_codes", "API response HTTP status code").labelNames("status_code").create().register();
        apiResponseTimes = Summary.build("api_response_times", "API Response time").quantile(0.95, 0.01).labelNames("method").register();

        super.doStart();
    }

    @Override
    public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws IOException, ServletException {
        if (!isApiRequest(path)) {
            super.handle(path, request, httpRequest, httpResponse);
            return;
        }

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
                updateResponses(request, httpResponse, start);
            }
            // else onCompletion will handle it.
        }
    }

    private boolean isApiRequest(String path) {
        return path.startsWith("/msgcenter") || path.startsWith("/msgbox") || path.startsWith("/message-center")
                || path.startsWith("/ebayk-msgcenter");
    }

    private void updateResponses(HttpServletRequest request, HttpServletResponse httpResponse, long start) {
        apiResponseStatusCodes.labels(Integer.toString(httpResponse.getStatus())).inc();
        apiResponseTimes.labels(request.getMethod().toLowerCase()).observe(System.currentTimeMillis() - start);
    }

    private final class AsyncListenerImplementation implements AsyncListener {
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
            final HttpServletRequest httpRequest = (HttpServletRequest) state.getRequest();
            final HttpServletResponse httpResponse = (HttpServletResponse) state.getResponse();
            updateResponses(httpRequest, httpResponse, startTime);
        }
    }
}
