# kjca-filter-word

Originally taken from https://github.corp.ebay.com/ecg-kijiji-ca/replyts2-wordfilter-plugin
(original git hash: b51a8809e256bdea545a9adb6652d2b104e0047e)

# Description

Migration of the ReplyTS 1 wordfilter plugin to ReplyTS 2. Made by eBay Kleinanzeigen.

A wordfilter manages a list of `(regexp X score)` pairs. Filter instances will apply their regular expressions against
each incoming mail, including subject and body . If the pattern matches, the message is assigned a score. Optional a rule can assigned by category ids. These ids are provided via custom headers ('categoryid').

# Filter configuration

The filter factory creating filter instances is com.ecg.de.kleinanzeigen.replyts.wordfilter.WordfilterFactory`.

Filter configurations should look like this:
 ```
 {
    ignoreQuotedRegexps: true,
    rules: [
        {regexp: "foo[a-z0-9]+", score: 100},
        {regexp: "bar[a-z0-9]+", score: -100, 'categoryIds': {'c218','c45556565'}}
    ]
 }
 ```

if `ignoreQuotedRegexps` is true, then regular expressions that have also fired in previous messages in the same conversation will be ignored if those messages were sent out.

# Properties

Configuration via replyts.properties:

Timeout for RegEx processing:
 ```
replyts2-wordfilter-plugin.regex.processingTimeoutMs
 ```

Default: 60,000
After this time, the processing of a single RegEx will be skipped.

# DoS Protection
To protect against long running RegEx expressions, the total processing time will be continuously checked. If time exceeded, message wil be dropped.
This feature are implemented since version 2.7.0 and requires RTS version 2.17.x. See RTS documentation for more.  

# Contributor
André Charton from eBay Kleinanzeigen Germany: acharton@ebay-kleinanzeigen.de
Swen Moczarski from eBay Kleinanzeigen Germany: smoczarski@ebay-kleinanzeigen.de
