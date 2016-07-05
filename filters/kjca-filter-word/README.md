# kjca-filter-word

Originally taken from https://github.corp.ebay.com/ecg-kijiji-ca/replyts2-wordfilter-plugin
(original git hash: 3e035280b531907d8b4817c83cc4ca22c1201bbf)

# Description

Migration of the ReplyTS 1 wordfilter plugin to ReplyTS 2. Made by eBay Kleinanzeigen.

A wordfilter manages a list of `(regexp X score)` pairs. Filter instances will apply their regular expressions against
each incoming mail, including subject and body . If the pattern matches, the message is assigned a score. Optional a rule can assigned by category ids. These ids are provided via custom headers ('categoryid').

# Filter configuration

The filter factory creating filter instances is com.ecg.de.kleinanzeigen.replyts.wordfilter.WordfilterFactory`.

Filter configurations should look like this:
 ```
 {
    "ignoreQuotedRegexps": true,
    "ignoreFollowUps": false,
    "rules": [
        {"regexp": "foo[a-z0-9]+", "score": 100},
        {"regexp": "bar[a-z0-9]+", "score": -100, "categoryIds": {'c218','c45556565'}}
    ],
    "runFor": {
        "userType": [
          "FSBO"
        ],
        "exceptCategories": [132],
        "categories": [ 238, 14 ]
    }
 }
 ```

If `ignoreQuotedRegexps` is true, then regular expressions that have also fired in previous messages in the same conversation will be ignored if those messages were sent out. If not specified, false.

If `ignoreFollowUps` is true, then the filter will only be run on conversation-initiating messages (those that are coming from the buyer until the seller responds). If not specified, false.

`runFor` is optional. If provided, restricts the categories and user types for which the filter will run. See https://github.corp.ebay.com/ReplyTS/replyts2-activable-filter for details.

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

# Contributors

* André Charton from eBay Kleinanzeigen Germany: acharton@ebay-kleinanzeigen.de
* Swen Moczarski from eBay Kleinanzeigen Germany: smoczarski@ebay-kleinanzeigen.de
* Dmitri Vassilenko from Kijiji Canada: dvassilenko@ebay.com
