package com.ecg.comaas.mp.postprocessor.urlgateway.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Rewrites URLs to the gateway for plain text mail parts.
 * <p>
 * Note: does <i>not</i> rewrite:
 * <ol>
 * <li>numeric addresses like http://2186908949, http://0x82599515, http://x.x.x.x, etc.</li>
 * <li>addresses with a user name in them, e.g. http://user:pw@marktplaats.nl</li>
 * </ol>
 * It is assumed that these are blocked by a filter.
 *
 * @author Erik van Oosten
 */
public class PlainTextMailPartUrlGatewayRewriter implements UrlGatewayRewriter {
    // Taken from http://en.wikipedia.org/wiki/List_of_Internet_top-level_domains
    private static final String TOP_LEVEL_DOMAIN_PATTERN =
            "(?:" +
                    // Encoded UTF-8 top level domains
                    "xn--[a-z0-9]+|" +
                    // All top level domains of 3 or more ASCII characters
                    "aero|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|" +
                    "museum|name|net|org|pro|tel|travel|xxx|" +
                    // All top level domains of 2 ASCII characters
                    "[a-z]{2}|" +
                    // Approved UTF-8 top level domains (escaped to not confuse the editor):
                    "\u0644\u062c\u0632\u0627\u0626\u0631|\u4e2d\u56fd|\u4e2d\u570b|\u0645\u0635\u0631|" +
                    "\u9999\u6e2f|\u092d\u093e\u0930\u0924|\u0628\u06be\u0627\u0631\u062a|" +
                    "\u0c2d\u0c3e\u0c30\u0c24\u0c4d|\u0aad\u0abe\u0ab0\u0aa4|\u0a2d\u0a3e\u0a30\u0a24|" +
                    "\u0b87\u0ba8\u0bcd\u0ba4\u0bbf\u0baf\u0bbe|\u09ad\u09be\u09b0\u09a4|" +
                    "\u0627\u06cc\u0631\u0627\u0646|\u0627\u0644\u0627\u0631\u062f\u0646|" +
                    "\u0627\u0644\u0645\u063a\u0631\u0628|\u0641\u0644\u0633\u0637\u064a\u0646|" +
                    "\u0642\u0637\u0631|\u0440\u0444|\u0627\u0644\u0633\u0639\u0648\u062f\u064a\u0629|" +
                    "\u0441\u0440\u0431|\u65b0\u52a0\u5761|" +
                    "\u0b9a\u0bbf\u0b99\u0bcd\u0b95\u0baa\u0bcd\u0baa\u0bc2\u0bb0|" +
                    "\ud55c\uad6d|\u0dbd\u0d82\u0d9a\u0dcf|\n\u0b87\u0bb2\u0b99\u0bcd\u0b95\u0bc8|" +
                    "\u0633\u0648\u0631\u064a\u0627|\u53f0\u6e7e|\u53f0\u7063|\u0e44\u0e17\u0e22|" +
                    "\u062a\u0648\u0646\u0633|\u0627\u0645\u0627\u0631\u0627\u062a|" +
                    // Not (yet) approved UTF-8 top level domains:
                    "\u09ac\u09be\u0982\u09b2\u09be|\u0431\u0433|\u10d2\u10d4|\u05d9\u05e9\u05e8\u05d0\u05dc|" +
                    "\u049b\u0430\u0437|\u0645\u0644\u064a\u0633\u064a\u0627|\u0639\u0645\u0627\u0646|" +
                    "\u067e\u0627\u06a9\u0633\u062a\u0627\u0646|\u0443\u043a\u0440|\u0627\u0644\u064a\u0645\u0646" +
                    ")";

    /**
     * Matches URL paths like:
     * -       <em>(the empty string)</em>
     * - /
     * - /path/to/file
     * - /?q=abc
     * - ?q=abc
     * - #abc
     * - /a=(between+parenthesis)
     * - /a=(with+2+(parenthesis))
     * <p>
     * Will NOT match an ending '?!:,.', will NOT match an closing ')' unless the matching opening '(' is also
     * part of the match.
     */
    private static final String URL_PATH_AND_QUERY_STRING =
            "(?:" +
                    "(/|\\?|#)" +
                    "(?:\\([-a-z0-9+&@#/%=~_|$?!:,.]*\\)|[-a-z0-9+&@#/%=~_|$?!:,.])*" +
                    "(?:\\([-a-z0-9+&@#/%=~_|$?!:,.]*\\)|[a-z0-9+&@#/%=~_|$])" +
                    ")?";

    /**
     * See https://mpwiki.corp.ebay.com/display/replyts/URL+gateway+filter
     * <p>
     * Group 1 is the optional schema, or ("www.").
     * Group 2 is the domain part, not including "www." if that is present
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(?:(https?:/*|ftp:/*|file:/*|www\\.))?((?<!@)(?:[a-z0-9_][-a-z0-9+&#%=~_|$]{0,62}\\.)+" +
                    TOP_LEVEL_DOMAIN_PATTERN + ")" +
                    URL_PATH_AND_QUERY_STRING +
                    "(?![\\w])",
            Pattern.CASE_INSENSITIVE);

    /** {@inheritDoc} */
    @Override
    public String rewriteUrls(String content, GatewaySwitcher gatewaySwitcher) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return replaceUrls(content, URL_PATTERN, gatewaySwitcher, false);
    }

    /**
     * Behaves like {@link #rewriteUrls(String, GatewaySwitcher)}, however all links are embedded in a HTML
     * a element.
     */
    public String rewriteUrlsForHtml(String content, GatewaySwitcher gatewaySwitcher) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return replaceUrls(content, URL_PATTERN, gatewaySwitcher, true);
    }

    private String replaceUrls(String content, Pattern pattern, GatewaySwitcher gatewaySwitcher, boolean wrapInHtmlA) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer out = new StringBuffer(content.length() + 256);
        while (matcher.find()) {
            String url = matcher.group();
            String schemaOrWww = matcher.group(1);
            String domain = matcher.group(2);
            if ("www.".equalsIgnoreCase(schemaOrWww)) {
                domain = "www." + domain;
            }
            domain = domain.toLowerCase();

            String gatewayUrl = gatewaySwitcher.rewrite(url, domain);
            String replacement = wrapInHtmlA ? format("<a href=\"%s\">%s</a>", gatewayUrl, url) : gatewayUrl;
            // Escape '$' with '\$', and '\' with '\\' (MIG-3394)
            replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");
            matcher.appendReplacement(out, replacement);
        }
        matcher.appendTail(out);
        return out.toString();
    }

}
