package nl.marktplaats.postprocessor.urlgateway;

import java.util.ArrayList;
import java.util.List;

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
    private String gatewayUrl;

    /**
     * List of domains to skip, an entry starting with *. will ignore sub-domains, the domain
     * of the gateway is always skipped regardless of whether it is in this list.
     * <p>
     * Example: ["markplaats.nl", "*.marktplaats.nl"].
     * <p>
     * Optional, defaults to empty list.
     */
    private List<String> skipDomains = new ArrayList<String>();

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public List<String> getSkipDomains() {
        return skipDomains;
    }

    public void setSkipDomains(List<String> skipDomains) {
        this.skipDomains = skipDomains;
    }

}
