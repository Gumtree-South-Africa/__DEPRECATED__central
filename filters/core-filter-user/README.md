# ebayk-filter-user

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-userfilter-plugin
(original git hash: fc3fc9b16e57616a843c7806d311214ac4ab0434)

# Description

Migration of the ReplyTS 1 userfilter plugin to ReplyTS 2. Made by eBay Kleinanzeigen.

A wordfilter manages a list of `(regexp X score)` pairs. Filter instances will apply their regular expressions against
each incoming mail (from/to). If the pattern matches, the message is assigned a score.

# Filter configuration

The filter factory creating filter instances is `com.ecg.de.kleinanzeigen.replyts.userfilter.UserfilterFactory`.

Filter configurations should look like this:
 ```
 {
    rules: [
        {regexp: "yahoo.com", score: 500},
        {regexp: "gmx.de", score: 100}
    ]
 }
 ```
# Contributor
Matthias Huttar/Andre Charton from eBay Kleinanzeigen Germany: mhuttar@ebay-kleinanzeigen.de
