package nl.marktplaats.postprocessor.urlgateway.support;

import nl.marktplaats.postprocessor.urlgateway.UrlGatewayPostProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Decides whether to replace a URL to the gateway or not.
 *
 * @author Erik van Oosten
 */
public class GatewaySwitcher {

    private static final Logger LOG = LoggerFactory.getLogger(GatewaySwitcher.class);

    /**
     * Pattern specialized in matching the schema of a URL.
     * <p>
     * group 1: the schema (http, https, ftp or file)
     * group 2: the rest (the domain, path, query string, etc.) excluding any leading characters from "://"
     */
    private static final Pattern SCHEMA_PATTERN = Pattern.compile("(https?|ftp|file):/?/?(.*)", CASE_INSENSITIVE);

    /**
     * URL to the gateway.
     */
    private final String gatewayUrl;

    /**
     * List of domains that should be skipped (in lowercase).
     */
    private final List<String> skipDomains;

    /**
     * List of parent domains, of which sub-domains should be skipped (in lowercase).
     */
    private final List<String> skipParentDomains;

    /**
     * Builds optimized configuration from config pojo.
     *
     * @param config the processing filter config (not null)
     */
    public GatewaySwitcher(UrlGatewayPostProcessorConfig config) {
        // Add [url] to end of gatewayUrl if its not in it yet
        String configUrl = config.getGatewayUrl();
        gatewayUrl = configUrl.contains("[url]") ? configUrl : configUrl + "[url]";

        skipDomains = new ArrayList<>();
        skipParentDomains = new ArrayList<>();

        // Add the configured skip domains.
        List<String> configSkipDomains = config.getSkipDomains();
        for (String skipDomain : configSkipDomains) {
            if (skipDomain.startsWith("*.")) {
                skipParentDomains.add(skipDomain.substring(2).toLowerCase());
            } else {
                skipDomains.add(skipDomain.toLowerCase());
            }
        }

        // Also skip the domain of gatewayUrl (don't add if its already skipped)
        if (!redirectUrlToGateway(gatewayUrl)) {
            skipDomains.add(extractDomain(gatewayUrl));
        }

        LOG.info(
                "UrlGatewayPostProcessor started with:\n" +
                        "   * gateway URL: {}\n" +
                        "   * skipDomains: {}\n" +
                        "   * skipParentDomains: {}",
                gatewayUrl, skipDomains, skipParentDomains);
    }

    /**
     * Rewrite a URL to the gateway (when appropriate).
     *
     * @param url a valid URL (not null)
     * @return the rewritten URL, or <code>url</code> when the no rewrite was needed,
     * or null when the URL should not be included at all
     * @throws IllegalArgumentException when the URL is not valid
     */
    public String rewrite(String url) {
        String urlLower = url.toLowerCase();

        if (urlLower.startsWith("mailto:") || urlLower.startsWith("tel:") || urlLower.startsWith("wlmailhtml:") || urlLower.startsWith("skype:")) {
            return url;
        }
        if (urlLower.startsWith("javascript:") || urlLower.startsWith("about:")) {
            return null;
        }

        return rewrite(url, extractDomain(normalizeUrlSchema(url)));
    }

    /**
     * Rewrite a URL to the gateway (when appropriate).
     *
     * @param url    a valid URL (not null)
     * @param domain the domain of the given url in lowercase (not null)
     * @return the rewritten URL, or <code>url</code> when the no rewrite was needed for the given domain
     */
    public String rewrite(String url, String domain) {
        if (url.toLowerCase().startsWith("mailto:")) {
            return url;
        }

        if (redirectDomainToGateway(domain)) {
            try {
                url = normalizeUrlSchema(url);
                return gatewayUrl.replace("[url]", URLEncoder.encode(url, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 not supported on this JVM");
            }
        } else {
            return url;
        }
    }

    /**
     * Fixes the schema of a URL.
     *
     * @param url a url, with optionally a broken schema, e.g. http:/site.com, or http:site.com.
     * @return the same url with a proper schema, or url when it wasn't broken in the first place
     */
    private String normalizeUrlSchema(String url) {
        String lc = url.toLowerCase();
        if (lc.startsWith("http://") || lc.startsWith("https://") || lc.startsWith("ftp://")) {
            return url;
        }

        Matcher urlMatcher = SCHEMA_PATTERN.matcher(url);
        if (urlMatcher.matches()) {
            return urlMatcher.group(1) + "://" + urlMatcher.group(2);
        } else {
            return "http://" + url;
        }
    }

    /**
     * Should the URL be redirected?
     *
     * @param url a valid URL
     * @return true when the given URL contains a domain that should be redirected, false otherwise
     * @throws IllegalArgumentException when the URL is not valid
     */
    public boolean redirectUrlToGateway(String url) {
        // Skip exact matches
        String domain = extractDomain(url);

        return redirectDomainToGateway(domain);
    }

    /**
     * Should a URL for the given domain be redirected?
     *
     * @param domain a domain in lower case
     * @return true when the given domain that should be redirected, false otherwise
     */
    public boolean redirectDomainToGateway(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        if (skipDomains.contains(domain)) {
            return false;
        }

        // Skip parent domains
        int firstDotIndex = domain.indexOf('.');
        if (firstDotIndex > 0) {
            String parentDomain = domain.substring(firstDotIndex + 1);
            if (skipParentDomains.contains(parentDomain)) {
                return false;
            }
        }

        // The rest
        return true;
    }

    /**
     * @param url a URL (not null, must be valid as defined by {@link URL})
     * @return the domain, converted to lowercase
     * @throws IllegalArgumentException when the URL is not valid
     */
    private static String extractDomain(String url) {
        try {
            return new URL(url).getHost().toLowerCase();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Not a valid URL " + url);
        }
    }

}
