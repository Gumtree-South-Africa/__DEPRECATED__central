package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import com.netflix.hystrix.*;

public final class HystrixCommandConfigurationProvider {

    private static final String GROUP_KEY = "UserBehaviourGroup";
    private static final String POOL_KEY = "UserBehaviourPool";
    private static final double FUDGE_FACTOR = 1.25;
    private static final int READ_TIMEOUT = 100;
    private static final int CONNECT_TIMEOUT = 25;
    private static final int RETRY_BUFFER_MULTIPLIER = 3; // want to allow for some read timeouts and retries
    private static final int EXECUTION_TIMEOUT = ((int) ((READ_TIMEOUT + CONNECT_TIMEOUT) * FUDGE_FACTOR)) * RETRY_BUFFER_MULTIPLIER;
    private static final int THREADS = 5;

    private HystrixCommandConfigurationProvider() {
    }

    public static HystrixCommand.Setter provideUserBehaviourConfig(boolean testMode) {
        return HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(POOL_KEY))
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter()
                                .withExecutionTimeoutInMilliseconds(EXECUTION_TIMEOUT)
                                .withFallbackEnabled(false)
                                .withCircuitBreakerEnabled(!testMode)
                                .withExecutionTimeoutEnabled(!testMode)
                )
                .andThreadPoolPropertiesDefaults(
                        HystrixThreadPoolProperties.Setter()
                                .withCoreSize(THREADS));
    }
}
