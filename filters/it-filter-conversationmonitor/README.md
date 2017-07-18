Originally taken from  https://github.corp.ebay.com/annunci/replyts-conversationmonitor-plugin
(original git hash: bc976a76dd22f3945d23b5dac61b9e99718ce1db)

# replyts-conversationmonitor-plugin #

This plugin is used to monitor the size of a conversation and eventually log a warning or an error depending on the configured size thresholds. The plugin needs to be configured in the columbus-replyts-config project and these line must be added to the replyts.properties file:

```
replyts.conversation.monitor.trigger.chars=�
replyts.conversation.monitor.threshold.check.enabled=true
replyts.conversation.monitor.warn.size.threshold=5
replyts.conversation.monitor.error.size.threshold=10
replyts.conversation.monitor.replaced.chars=$|a,&|b

```

The thresholds values represent the chars of the whole conversation. The replaced chars represent the characters needed to be replaced, and related replacement.

Remember to activate the plugin using ReplyTs APIs in this way:

```
curl -X PUT "http://localhost:42001/configv2/ConversationMonitorFilterFactory/Default" -H "Content-Type: application/json" -d "{'priority': 100, 'state': 'ENABLED', 'configuration': {}}"
```
