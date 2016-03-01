# ebayk-filter-phonenumber

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-phonenumberfilter-plugin
(original git hash: 0fd5f5864b9c3a8d1db1aea664e3c50a2bf850bd)

# Description

This filter will try to detect phone numbers in a mail.

# Features
* It will also try to skip number separation characters (e.g. whitespace, dashes,...)
* Detect phone numbers in HTML links: <a href="tel:+49151.."> See: https://tools.ietf.org/html/rfc3966

# Filter configuration

The filter factory creating filter instances is `com.ecg.de.kleinanzeigen.replyts.phonenumberfilter.PhoneNumberFilterFactory`.

Filter configurations should look like this (see allowed formats):
 ```
 {
    score: 100,
    numbers: [
        '015712345678',
        '+4915712345679',
        '004915712345670'
    ]
 }
 ```

 ```
 curl -H "Content-Type: application/json"  -X PUT "http://localhost:8081/configv2/com.ecg.de.kleinanzeigen.replyts.phonenumberfilter.PhoneNumberFilterFactory/Default" -d "{
 'state': 'ENABLED',
 'priority':100,
 'configuration':{
    'score': 100,
    'numbers': ['015712345678']
 }
 }"

 ```

# Limitations
The filter don't take country code into account. It's possible to configure numbers with country code, but it will only used the national part of the number.

# Contributor
Swen Moczarski from eBay Kleinanzeigen Germany: smoczarski@ebay-kleinanzeigen.de
