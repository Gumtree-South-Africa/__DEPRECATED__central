# gtau-coremod-autogatemaildelivery

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-autogate-maildelivery
(original git hash: 01710a83ba6d9d160130bccc608ec859e1133bab)

# Description

# Autogate Mail Delivery Service
This service is an extension to Reply TS mail delivery system for sending Autogate leads. When this service receives
the Autogate headers and lead details (formatted like an email), it performs a post http call to Autogate rather
than sending the email.

The following headers are targeted in emails:
####"X-Cust-Http-Url"
####"X-Cust-Http-Account-Name"
####"X-Cust-Http-Account-Password"


## Configuration
The following entries can be configured in the replyts properties file:

* replyts.autogate.header.url (Default: X-Cust-Http-Url)
* replyts.autogate.header.account (Default: X-Cust-Http-Account-Name)
* replyts.autogate.header.password (Default: X-Cust-Http-Account-Password)
* replyts.autogate.httpclient.proxyHost (Default: null)
* replyts.autogate.httpclient.proxyPort (Default: 80)
* replyts.autogate.httpclient.maxConnectionsPerRoute (Default: 100)
* replyts.autogate.httpclient.maxConnections (Default: 100)
* replyts.autogate.httpclient.connectionTimeout (Default: 1000)
* replyts.autogate.httpclient.socketTimeout (Default: 1000)

