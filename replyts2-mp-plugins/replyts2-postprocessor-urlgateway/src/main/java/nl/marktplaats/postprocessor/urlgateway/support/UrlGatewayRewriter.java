package nl.marktplaats.postprocessor.urlgateway.support;

/**
 * Updates URLs in content to point to gateway.
 *
 * @author Erik van Oosten
 */
public interface UrlGatewayRewriter {

    /**
     * Updates URLs in content to point to gateway.
     *
     * @param content         the content to change (not null)
     * @param gatewaySwitcher the gateway information (not null)
     * @return the content with updated URLs
     */
    String rewriteUrls(String content, GatewaySwitcher gatewaySwitcher);

}
