package ca.kijiji.replyts;

import com.codahale.metrics.Counter;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Collections;

/**
 * An HTTP retrier that will retry requests even if they're not idempotent (i.e. POST/PUT)
 * and even if the request has already been sent. By default it will retry the request
 * on any IOException.
 *
 * Basically, this is the honeybadger of http retriers. It just don't care.
 */
public class ZealousHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {
    private Counter retryCounter;

    @SuppressWarnings("unchecked")
    public ZealousHttpRequestRetryHandler(final int retryCount) {
        this(retryCount, null);
    }

    @SuppressWarnings("unchecked")
    public ZealousHttpRequestRetryHandler(final int retryCount, Counter retryCounter) {
        super(retryCount, true, Collections.EMPTY_LIST);

        this.retryCounter = retryCounter;
    }

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        boolean retryRequest = super.retryRequest(exception, executionCount, context);
        if (retryCounter != null && retryRequest) {
            retryCounter.inc();
        }
        return retryRequest;
    }
}
