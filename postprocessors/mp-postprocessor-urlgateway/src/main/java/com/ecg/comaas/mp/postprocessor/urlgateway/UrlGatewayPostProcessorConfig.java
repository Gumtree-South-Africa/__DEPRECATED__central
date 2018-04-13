package com.ecg.comaas.mp.postprocessor.urlgateway;

import com.ecg.replyts.core.EnvironmentSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UrlGatewayPostProcessorConfig {

    /**
     * The gateway URL.
     * <p>
     * The string <code>[url]</code> will be replaced with the url-encoded URL.
     * If <code>[url]</code> is missing it will be appended at the end.
     * <p>
     * Example: {@literal <a href="http://gateway.marktplaats.nl/?url=[url]}
     * <p>
     * Required, must be a valid {@literal http://} or {@literal https://} url.
     */
    private final String gatewayUrl;

    /**
     * List of domains to skip, an entry starting with *. will ignore sub-domains, the domain
     * of the gateway is always skipped regardless of whether it is in this list.
     * <p>
     * Example: ["markplaats.nl", "*.marktplaats.nl"].
     * <p>
     * Optional, defaults to empty list.
     */
    private final List<String> skipDomains;

    public UrlGatewayPostProcessorConfig(String gatewayUrl,
                                         List<String> skipDomains) {
        this.gatewayUrl = gatewayUrl;
        this.skipDomains = skipDomains;

    }

    @Autowired
    public UrlGatewayPostProcessorConfig(@Value("${urlgateway.gatewayurl}") String gatewayUrl,
                                         AbstractEnvironment environment) {
        this(gatewayUrl,
                EnvironmentSupport.propertyNames(environment)
                .stream()
                .filter(key -> key.startsWith("urlgateway.skipdomains."))
                .map(environment::getProperty)
                .collect(Collectors.toList()));
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public List<String> getSkipDomains() {
        return skipDomains;
    }

}
