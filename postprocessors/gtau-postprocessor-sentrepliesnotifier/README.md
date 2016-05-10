# gtau-postprocessor-sentrepliesnotifier

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-sentreplies-notifier
(original git hash: b8af49c3fc86a9d9e2a0604584f3ef505e54e05d)

# Description

# Sent Replies Notifier Plugin
This Postprocessor plugin will send notifications with Advert ID to a configurable endpoint URL.

In detail, this plugin will:

Send an http request to the endpoint upon starting a new conversation on an advert. e.g.:
`http://accounting.com/?adId=123`

XML messages are ignored by the plugin.

## Configuration
just put the plugin into your classpath and add this entry to `replyts.properties`:

# Accounting Endpoint URL
replyts.sendnotifier.endpoint.url=http://foo.com/
