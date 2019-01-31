package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.comaas.filterapi.dto.FilterResponse;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.cluster.XidFactory;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import io.vavr.control.Try;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RemoteFilter implements InterruptibleFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteFilter.class);

    private static final Duration maxProcessingDuration = Duration.ofMillis(30000);

    private static final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

    // pool *should* have same number of connections as in KafkaMessageProcessingPoolManager
    // this is obviously a leaky abstraction but we cannot fix this nicely now.
    public static final int NR_OF_COMAAS_THREADS = 4;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(NR_OF_COMAAS_THREADS, 5, TimeUnit.MINUTES))
            .readTimeout(maxProcessingDuration.toMillis(), TimeUnit.MILLISECONDS)
            .build();

    private final URL endpointURL;


    private RemoteFilter(URL endpointURL) {
        this.endpointURL = endpointURL;
    }

    public static RemoteFilter create(URL endpointURL) {
        return new RemoteFilter(endpointURL);
    }

    // We assume this was set externally; the filter interface doesn't allow us to do this differently.
    // But we are defensive here, creating it if it's not set (e.g. in tests)
    private static final String getOrCreateCorrelationId() {
        String cid = MDC.get(MDCConstants.CORRELATION_ID);
        if (cid == null) {
            cid = XidFactory.nextXid();
            MDC.put(MDCConstants.CORRELATION_ID, cid);
        }
        return cid;
    }

    public List<FilterFeedback> filter(MessageProcessingContext context) {
        Objects.requireNonNull(context);

        byte[] serializedRequestBody = Try.of(() -> FilterAPIMapper.FromModel.toFilterRequest(context, getOrCreateCorrelationId(), (int) maxProcessingDuration.toMillis()))
                .mapTry(FilterAPIMapper.getSerializer()::writeValueAsBytes)
                .getOrElseThrow(e -> new RuntimeException("FilterRequest unserializable: that's a bug", e));

        Call call = client.newCall(
                new Request.Builder()
                        .url(endpointURL)
                        .post(RequestBody.create(mediaType, serializedRequestBody))
                        .build()
        );

        Response response = Try.of(call::execute)
                .getOrElseThrow(ioExc -> {
                    if (ioExc instanceof java.io.InterruptedIOException) {
                        // satisfy InterruptibleFilter contract
                        return InterruptibleFilter.createInterruptedException();
                    }
                    return new RuntimeException("HTTP Call failed", ioExc);
                });

        LOG.info("Received {} from {}", response.code(), endpointURL);

        String responseBody = Try.of(response.body()::string)
                .andFinally(response::close)
                .getOrElseThrow(e -> {
                    throw new RuntimeException("Cannot read response", e);
                });

        switch (response.code()) {
            case 200:
                return Try
                        .ofCallable(() -> FilterAPIMapper.getSerializer().readValue(responseBody, FilterResponse.class))
                        .flatMap(FilterAPIMapper.FromAPI::toFilterFeedback)
                        .getOrElseThrow(e -> new RuntimeException("cannot parse response body", e));
            case 400: // fall though
            case 422:
                throw new RuntimeException("Request failed: code=400 (bad request), url=" + endpointURL + ", body=" + responseBody);
            case 404:
                throw new RuntimeException("Unknown filter: url=" + endpointURL);
            default:
                throw new RuntimeException("Unexpected response: code=" + response.code() + ", url=" + endpointURL);
        }
    }

}
