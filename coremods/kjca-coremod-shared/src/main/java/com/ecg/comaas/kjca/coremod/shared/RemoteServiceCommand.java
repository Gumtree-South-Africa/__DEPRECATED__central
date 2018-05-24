package com.ecg.comaas.kjca.coremod.shared;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.google.common.collect.ImmutableList;
import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic Hystrix command wrapper/decorator, for all your external service needs!
 * <p>
 * Adds the X-Kijiji-TraceNumber header automatically, from the TraceThreadLocal object, and will set assorted other headers on the HttpRequest as needed.
 * <p>
 * Retry logic can be configured easily. Likewise, Hystrix failures are discovered, tracked, and contained in RemoteServiceExceptions.
 * <p>
 * Configuring the RemoteServiceCommand uses a fluent interface, so you can get everything set up quite easily:
 * <code>
 * public class SuccessAsAService {
 * public boolean succeed() {
 * AlwaysSucceedsCommand command = new AlwaysSucceedsCommand().withHttpClient(httpClient)
 * .withHttpRequest(new HttpGet(""))
 * .withMetrics(metricRegistry)
 * .withCircuitBreakerName("success")
 * .withThreadPoolName("succeed")
 * .withMaxThreadsUsed(2)
 * .withMaximumCommandTime(50)
 * .withFallbackEnabled(false)
 * .withMicroserviceDiscovery(serviceDiscovery, "success", 1, "success.discovery.failed");
 * return command.execute();
 * }
 * <p>
 * private class AlwaysSucceedsCommand extends RemoteServiceCommand<Boolean, AlwaysSucceedsCommand> {
 * protected Boolean success(InputStream inputStream) {
 * return true;
 * }
 * <p>
 * protected String exceptionMessageTemplate() {
 * return "Had a {} problem while succeeding.";
 * }
 * }
 * }
 * </code>
 * This will reach out to the "success" microservice, try once, and return a success every time (since the result isn't parsed). Fallback has been disabled,
 * the command has a maximum of 50ms to complete, and no more than two AlwaysSucceedsCommands may be executing simultaneously.
 * <p>
 * You can use {@link #withRetryRequestOnException(Predicate)} and {@link #withRetries(int)} to configure the retry conditions.
 */
public abstract class RemoteServiceCommand<R, S extends RemoteServiceCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteServiceCommand.class);
    private static final int DEFAULT_RESPONSE_KEY = -1;

    private final Callable<HttpResponse> httpResponseCallable = new HttpResponseCallable();
    private final String trace = TraceThreadLocal.get();

    protected RemoteServiceException failure;
    protected HttpRequestBase request;
    HystrixCommand<R> command;

    private int threadPoolSize;
    private Map<Integer, Function<String, R>> responseCallbacks = new HashMap<>();

    private HttpClient httpClient;
    private HystrixCommandKey commandKey;
    private HystrixCommandGroupKey groupKey;
    private HystrixThreadPoolKey threadPoolKey;
    private HystrixCommandProperties.Setter commandPropertiesDefaults = HystrixCommandProperties.Setter();
    private Iterable<URI> endpoints;
    private RetryerBuilder<HttpResponse> retryerBuilder = RetryerBuilder.newBuilder();

    public RemoteServiceCommand() {
        withCommandName(this.getClass().getSimpleName());
        responseCallbacks.put(HttpStatus.SC_OK, this::parseResult);
        responseCallbacks.put(HttpStatus.SC_CREATED, this::parseResult);
        responseCallbacks.put(HttpStatus.SC_ACCEPTED, this::parseResult);
        responseCallbacks.put(HttpStatus.SC_NO_CONTENT, this::parseResult);
    }

    /**
     * Specify the HttpClient to use for requests to the downstream service.
     *
     * @param httpClient The use of a subtype of CloseableHttpClient is preferred, for obvious reasons, but not enforced.
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return (S) this;
    }

    /**
     * Specify the HttpRequest type you're using.
     *
     * @param httpRequest Any subclass of HttpRequestBase. RemoteServiceCommand will apply certain headers when the command is executed, include the Trace #.
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withHttpRequest(HttpRequestBase httpRequest) {
        this.request = httpRequest;
        return (S) this;
    }

    /**
     * Specify the maximum execution time for the command before directly timing out.
     *
     * @param fullTimeout Defined in milliseconds.
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withMaximumCommandTime(int fullTimeout) {
        this.commandPropertiesDefaults = this.commandPropertiesDefaults.withExecutionTimeoutInMilliseconds(fullTimeout);
        return (S) this;
    }

    /**
     * Specify the circuit breaker group name this command belongs to.
     *
     * @param circuitBreakerName Corresponds to Hystrix's GroupKeyName.
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withCircuitBreakerName(String circuitBreakerName) {
        this.groupKey = HystrixCommandGroupKey.Factory.asKey(circuitBreakerName);
        return (S) this;
    }

    /**
     * Specify the command name this command belongs to.
     * <p>
     * Optional; will use the class name of the RemoteServiceCommand implementation by default.
     *
     * @param commandName Corresponds to Hystrix's CommandKeyName.
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withCommandName(String commandName) {
        this.commandKey = HystrixCommandKey.Factory.asKey(commandName);
        return (S) this;
    }

    /**
     * Specify the thread pool name this command belongs to.
     *
     * @param threadPoolName
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withThreadPoolName(String threadPoolName) {
        this.threadPoolKey = HystrixThreadPoolKey.Factory.asKey(threadPoolName);
        return (S) this;
    }

    /**
     * Specify the core thread pool size to use for this command.
     *
     * @param threadPoolSize
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withMaxThreadsUsed(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        return (S) this;
    }

    /**
     * Provide a fixed list of endpoints to iterate through for requests to the remote service.
     * <p>
     * This iterable, and its cursor, are persistent across requests to the same command.
     * However, this does not assure load-balancing amongst members of the list.
     */
    public S withSpecificEndpoints(final Iterable<URI> serviceEndpoints) {
        this.endpoints = ImmutableList.copyOf(serviceEndpoints);

        return (S) this;
    }

    /**
     * Specify a callback Function, acting on the InputStream from the HTTP response, returning an object of the Command's return type
     * <p>
     * If you don't specify any of these, the default behaviour is to call success(InputStream) for HTTP 200-204. This method is not abstract, and will throw
     * an UnsupportedOperationException if not overridden, in these cases. It's up to the developer to know what responses they expect from their downstream
     * service and handle them appropriately.
     *
     * @param statusCode An HTTP status code
     * @param function   A Java Function, lambda, or :: static reference to a method that accepts an InputStream and returns an <R>.
     * @return An instance of the RemoteServiceCommand, of its own type, for fluent configuration.
     */
    public S withResponse(int statusCode, Function<String, R> function) {
        responseCallbacks.put(statusCode, function);
        return (S) this;
    }

    /**
     * Execute the command within Hystrix
     *
     * @return An object of type R.
     * @throws IllegalStateException  if the developer tries calling .execute() before the circuit breaker has been fully configured
     * @throws RemoteServiceException if any failure occurred, whether by the remote service or by Hystrix
     */
    public R execute() {
        if (this.groupKey == null || this.threadPoolKey == null) {
            throw new IllegalStateException("Circuit breaker and thread pool have not yet been configured! Remember to call .withCircuitBreakerName() and .withThreadPoolName() before .execute!");
        }

        if (this.request == null) {
            throw new IllegalStateException("The HTTP request has not yet been configured!");
        }

        this.command = new DecoratedHystrixCommand<R>();
        try {
            final R result = command.execute();
            if (isAcceptableResult(result)) {
                return result;
            }
        } catch (HystrixRuntimeException e) {
            LOG.debug("Hystrix tried to escape!", e);
        }

        throw handleFailure();
    }

    /**
     * Depending on the type of R, the result may be unacceptable if it's null, or if it's e.g. Boolean.FALSE. Override
     * this method if a non-null result is also considered "unacceptable". Failure of this test will assume that Hystrix
     * failed out, and will cause a RemoteServiceException to be thrown.
     *
     * @param result The output of a command execution, whether successful or the fallback response.
     * @return true iff the remote service's result is usable, according to the caller.
     */
    protected boolean isAcceptableResult(R result) {
        return result != null;
    }

    private RemoteServiceException handleFailure() {
        if (failure == null) {
            if (command.isCircuitBreakerOpen()) {
                // Hystrix's circuit breaker tripped; we have no recorded failure, but something's still wrong.
                failure = new RemoteServiceException(RemoteServiceException.Cause.UNKNOWN, "Hystrix circuit open", null);
            } else if (command.isResponseTimedOut()) {
                failure = new RemoteServiceException(RemoteServiceException.Cause.TIMEOUT, "Hystrix command timed out", null);
            } else if (command.isFailedExecution()) {
                failure = new RemoteServiceException(RemoteServiceException.Cause.UNKNOWN, "Hystrix command failed", command.getFailedExecutionException());
            } else {
                failure = new RemoteServiceException(RemoteServiceException.Cause.UNKNOWN, "Hystrix command did not succeed, unsure why", null);
            }
            onRSCException(failure);
        } else {
            onRSCException(failure.getCause());
        }

        return failure;
    }

    /**
     * Called from Hystrix's getCallback() method. To be implemented iff you've enabled fallback on the command.
     *
     * @return A replacement instance of type R.
     */
    protected R fallback() {
        throw new UnsupportedOperationException();
    }

    /**
     * The call came back better than HTTP 4xx or 5xx. The response entity is passed in, and an instance of R is
     * returned.
     */
    @Deprecated
    protected R success(String response) throws IOException {
        throw new UnsupportedOperationException("A default successful response handler has not yet been assigned.");
    }

    private R parseResult(String response) {
        try {
            return success(response);
        } catch (IOException e) {
            throw new RemoteServiceException(e);
        }
    }

    private RemoteServiceException wrapException(Exception e) {
        if (e.getClass() != RemoteServiceException.class) {
            return new RemoteServiceException(e);
        }

        return (RemoteServiceException) e;
    }


    /**
     * A method that's called on command execution exception. How the exception is handled can be overridden by individual commands.
     * Common actions can be Logging the exception in a desired way.
     *
     * @param e the actual exception that caused hystrix command to fail or a hystrix failure wrapped in  RemoteServiceException
     */
    protected abstract void onRSCException(final Throwable e);

    private class HttpResponseCallable implements Callable<HttpResponse> {
        @Override
        public HttpResponse call() throws Exception {
            IOException exception = null;
            for (URI endpoint : endpoints) {
                try {
                    final HttpHost host = new HttpHost(endpoint.getHost(), endpoint.getPort(), endpoint.getScheme());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("About to send http request {} to {}://{}{}", request.getRequestLine().getMethod(), host.getSchemeName(), host.toHostString(), request.getRequestLine().getUri());
                    }
                    final HttpResponse response = httpClient.execute(host, request);
                    if (responseCanBeProcessed(response)) {
                        return response;
                    }
                    HttpClientUtils.closeQuietly(response);
                } catch (IOException e) {
                    exception = e;
                    LOG.warn("Unable to process {} due to {}. This is retryable. ({})", request.getMethod(), e.getClass().getSimpleName(), e.getMessage());
                }
            }
            throw new RemoteServiceException(RemoteServiceException.Cause.HTTP, "No endpoints were able to successfully handle this " + request.getMethod(), exception);
        }
    }

    /**
     * Indicates whether the HTTP response can be processed, or if the next available endpoint should be used.
     *
     * @param response The HttpResponse received, to be tested.
     * @return Boolean yes-or-no; can this processed?
     */
    protected boolean responseCanBeProcessed(final HttpResponse response) {
        return response.getStatusLine().getStatusCode() < 500;
    }

    private class DecoratedHystrixCommand<T> extends HystrixCommand<R> {
        private final Map<String, String> parentContextMap;

        DecoratedHystrixCommand() {
            super(Setter.withGroupKey(RemoteServiceCommand.this.groupKey)
                    .andCommandKey(RemoteServiceCommand.this.commandKey)
                    .andThreadPoolKey(RemoteServiceCommand.this.threadPoolKey)
                    .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(RemoteServiceCommand.this.threadPoolSize))
                    .andCommandPropertiesDefaults(RemoteServiceCommand.this.commandPropertiesDefaults));

            parentContextMap = MDC.getCopyOfContextMap();
        }

        @Override
        protected R run() throws Exception {
            if (parentContextMap != null) {
                MDC.setContextMap(parentContextMap);
            }

            request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            request.setHeader(TraceLogFilter.TRACE_HEADER, trace);

            try {
                final HttpResponse response = tryUntilGoodResponseOrException();
                final StatusLine statusLine = response.getStatusLine();
                final int statusCode = statusLine.getStatusCode();
                final String executedResponse = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Http Response: status code {} {} \n Response body: [{}]", statusCode, statusLine.getReasonPhrase(), executedResponse);
                }

                return handleResponse(statusCode, executedResponse);
            } catch (Exception e) {
                failure = wrapException(e);
                throw failure;
            }
        }

        private HttpResponse tryUntilGoodResponseOrException() {
            try {
                final Retryer<HttpResponse> retryer = retryerBuilder.build();
                return retryer.call(httpResponseCallable);
            } catch (ExecutionException e) {
                throw new RemoteServiceException(RemoteServiceException.Cause.UNKNOWN, "Encountered exception with retrying request, aborting.", e);
            } catch (RetryException e) {
                LOG.warn("Ran out of retries for {}, aborting.", request.getMethod());
                throw new RemoteServiceException(RemoteServiceException.Cause.HTTP, "Ran out of retries to handle this " + request.getMethod(), e);
            }
        }

        private Function<String, R> findResponseCallback(int statusCode) {
            final Function<String, R> callback = responseCallbacks.get(statusCode);
            return callback != null ? callback : responseCallbacks.get(DEFAULT_RESPONSE_KEY);
        }

        private R handleResponse(int statusCode, String executedResponse) {
            final Function<String, R> callback = findResponseCallback(statusCode);
            if (callback != null) {
                return callback.apply(executedResponse);
            }

            throw new RemoteServiceException(statusCode, executedResponse, null);
        }

        @Override
        protected R getFallback() {
            return fallback();
        }
    }
}
