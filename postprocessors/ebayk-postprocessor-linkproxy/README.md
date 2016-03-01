# ebayk-postprocessor-linkproxy

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-linkproxy-plugin
(original git hash: 85e11d8e11fa48b1baff7caea02009b74a6e1fee)

# Description

This Postprocessor plugin will search every text part of an outgoing mail and rewrite all external urls (linking to potentially
hazardous sites) so that they can be intercepted by an interstitial warning the user about potential risk.

In detail, this plugin will:

Replace Every URL in the mail's bodies by a configurable url that has the original url embedded. e.g.:
`http://www.facebook.com => http://yoursite.com/warning?to=http%3A%2F%2Fwww.facebook.com`

For HTML Mails, the plugin is Image-Tag aware: `<img src=""/>" urls will not be rewritten.

Also, a set of whitelisted domains can be configured. URLs from this domain will not be rewritten.

## Configuration
just put the plugin into your classpath and add these entries to `replyts.properties`:

```
# pattern to replace every link with. The '%s' is a placeholder for the original url (it will be url encoded correctly)
replyts.linkescaper.proxyurl=http://foo.com?url=%s;

#comma seperated list of whitelisted domains. Subdomains will also be whitelisted
replyts.linkescaper.whitelist=ebay.com,ebay.de;
```
