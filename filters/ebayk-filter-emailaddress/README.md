# ebayk-filter-emailaddress

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-emailaddressfilter-plugin
(original git hash: efe7ecb6ceaafbc67250d9f6822bab5b67f5cb12)

# Description

This filter will try to detect blocked email addresses in a mail.

# Features
* It will also try to skip number separation characters (e.g. whitespace, dashes,...)
* Detect email addresses in HTML links: <a href="mailto:foo@bar.com.."> 
* Detect patterns like [at] (at)

# Filter configuration

The filter factory creating filter instances is `com.ecg.de.kleinanzeigen.replyts.emailaddressfilter.EmailAddressFilterFactory`.

Filter configurations should look like this:
 ```
 {
    score: 100,
    values: [
        'foo@bar.com',
        'bar@foo.com'
    ]
 }
 ```
Only valid email addresses are allowed. No wildcards supported.

 ```
 curl -H "Content-Type: application/json"  -X PUT "http://localhost:8081/configv2/com.ecg.de.kleinanzeigen.replyts.emailaddressfilter.EmailAddressFilterFactory/Default" -d "{
 'state': 'ENABLED',
 'priority':100,
 'configuration':{
    'score': 100,
    'values': ['foo@bar.com']
 }
 }"

 ```

# Limitations
No wildcard supported in blocked email addresses.

# Contributor
Swen Moczarski from eBay Kleinanzeigen Germany: smoczarski@ebay-kleinanzeigen.de
