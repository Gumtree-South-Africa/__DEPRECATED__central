# bt-filter-word

Originally taken from https://github.corp.ebay.com/ecg-global/replyts2-wordfilter-plugin
(original git hash: 836f9dc5868faac88c9a099db1f578f8fd832a7a)

# Word filter

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



# Contributor

André Charton from eBay Kleinanzeigen Germany: mhuttar@ebay-kleinanzeigen.de
