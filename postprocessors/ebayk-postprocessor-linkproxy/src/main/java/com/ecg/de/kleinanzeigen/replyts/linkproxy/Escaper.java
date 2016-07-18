package com.ecg.de.kleinanzeigen.replyts.linkproxy;

import com.google.common.collect.Sets;
import org.elasticsearch.common.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

class Escaper {

    public static final Pattern PATTERN_PLAINTEXT_LINK = Pattern.compile("(https?://[^\\s\"]+)", Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_IMAGE_LINK = Pattern.compile("(?<!src\\s{0,10}=\\s{0,10}\")(https?:(/|&#?[a-z0-9]+;){2}[^\\s\"<>]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_ESCAPED_SLASHES = Pattern.compile("&#x2F;", Pattern.CASE_INSENSITIVE);

    private final String proxyDomain;

    private final Set<Pattern> excludedDomainsAsPattern;

    public Escaper(String proxyDomain, Set<String> excludedDomains) {
        this.proxyDomain = proxyDomain;


        this.excludedDomainsAsPattern = Sets.newHashSet();
        for (String excludedDomain : excludedDomains) {
            this.excludedDomainsAsPattern.add(Pattern.compile(format("^https?://([^/]*\\.)?%s([/#?]|$)", Pattern.quote(excludedDomain)), Pattern.CASE_INSENSITIVE));
        }
    }



    public String escapePlaintext(String plaintext) {
        return escapeWithMatcher(plaintext, PATTERN_PLAINTEXT_LINK);
    }

    public String escapeHtml(String html) {
        html = HTML_ESCAPED_SLASHES.matcher(html).replaceAll("/");
        return escapeWithMatcher(html, PATTERN_IMAGE_LINK);
    }

    private String escapeWithMatcher(String content, Pattern p) {
        String escaped = content;
        Matcher matcher = p.matcher(content);
        while (matcher.find()) {
            String link = matcher.group(1);
            link = fixLinkEnd(link);

            if (isWhitelisted(link)) {
                continue;
            }

            escaped = Strings.replace(escaped, link, format(proxyDomain, urlEncode(htmlDecode(link))));

        }

        return escaped;
    }

    private String fixLinkEnd(String link) {
        // some mail clients will put tees around urls and make a hyperlink out of them. it's very hard to build a regex to fix this.  removing trailing > (or &gt;) is simpler.
        if(link.toLowerCase().endsWith("&gt;")) {
            return Strings.replace(link, "&gt;", "");
        }
        if(link.endsWith(">")) {
            return link.substring(0, link.length()-1);
        }
        return link;
    }

    private String htmlDecode(String link) {
        return Entities.unescapeXml(link);
    }

    private boolean isWhitelisted(String match) {


        for (Pattern excludePattern : excludedDomainsAsPattern) {
            if (excludePattern.matcher(match).find()) {
                return true;
            }
        }

        return false;
    }


    private String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
