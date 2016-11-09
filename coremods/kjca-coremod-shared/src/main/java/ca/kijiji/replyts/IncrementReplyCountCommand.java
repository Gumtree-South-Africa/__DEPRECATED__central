package ca.kijiji.replyts;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;


class IncrementReplyCountCommand extends HystrixCommand<Void>{
    private static final Logger LOG = LoggerFactory.getLogger(IncrementReplyCountCommand.class);
    private static final int THREAD_POOL_SIZE = 5;
    private static final int EXECUTION_TIMEOUT = 1025;
    private static final HystrixCommandProperties.Setter COMMAND_DEFAULTS = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(EXECUTION_TIMEOUT);
    private static final HystrixThreadPoolProperties.Setter POOL_DEFAULTS = HystrixThreadPoolProperties.Setter()
            .withCoreSize(THREAD_POOL_SIZE);

    private static final HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("IncrementReplyCountGroup");
    private static final HystrixThreadPoolKey POOL_KEY = HystrixThreadPoolKey.Factory.asKey("IncrementReplyCountPool");

    private final CloseableHttpClient httpClient;
    private URI path;
    private String authHeader;


    IncrementReplyCountCommand(
            CloseableHttpClient httpClient,
            URI path,
            String authHeader
    ) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andThreadPoolKey(POOL_KEY)
                .andCommandPropertiesDefaults(COMMAND_DEFAULTS)
                .andThreadPoolPropertiesDefaults(POOL_DEFAULTS)
        );
        this.httpClient = httpClient;
        this.path = path;
        this.authHeader = authHeader;
    }

    @Override
    protected Void run() throws Exception {
        HttpPost request = new HttpPost(path);
        request.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
        try(CloseableHttpResponse response = httpClient.execute(request)){
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 300) {
                LOG.warn("Got unexpected [{}] from [{}].", response.getStatusLine(), path);
            }
        } catch (Exception e) {
            LOG.warn("Got exception from [{}]", path, e);
        }
        return null;
    }
}
