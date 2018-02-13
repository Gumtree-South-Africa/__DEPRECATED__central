# bt-filter-volume

Originally taken from https://github.corp.ebay.com/ecg-global/replyts2-volumefilter-plugin
(original git hash: f3e2dbabf534f8306d903b922a7a068f2b0293b4)

# replyts2-volumefilter-plugin

Migration of the ReplyTS 1 volumefilter plugin to ReplyTS 2. Made by eBay Kleinanzeigen.

A Volumefilters manages multiple mail sending quotas per mail address (maximum number of mails per time unit). If a quota is violated,
a configurable score is assigned to the incoming message.

Multiple Quotas can be defined (e.g. max. 10 mails in one hour AND max. 30 mails per day). If more than one quota is exceeded,
the highest score is assigned to the message.
(e.g. max 10/hour -> score 50, max 30/day -> score 100: if 31 mails are received by a user within 5 minutes, both quotas
are violated and a score of 100 is assigned, because 100 is bigger than 50)

The filter will use Hazelcast to communicate with other nodes in the ReplyTS cluster about received mails and will use
the Esper Event Processing Framework (http://esper.codehaus.org) to analyze the incoming stream.

When a ReplyTS node is restarted, all information is lost and can not be recovered.

# Filter configuration


 ```
 {
    rules: [
        {"allowance": 10, "perTimeValue": 1, "perTimeUnit": "HOURS", "score": 100},
        {"allowance": 10, "perTimeValue": 1, "perTimeUnit": "DAYS", "score": 200}
    ]
 }
 ```

Allowed values for `perTimeUnit` are all provided by `java.util.concurrent.TimeUnit` except for anything smaller than `MINUTES`:
* `DAYS`
* `HOURS`
* `MINUTES`

(there are some more, which do not make much sense in that context tough)


Create a filter config via API:


```
curl -X PUT http://kreplyts44-1.220:42001/configv2/com.ecg.de.kleinanzeigen.replyts.volumefilter.VolumeFilterFactory/VolumeFilter -H "Content-Type: application/json" -d '
{
    state: "ENABLED",
    priority: 100,
    configuration: {
     rules: [
            {"allowance": 20, "perTimeValue": 2, "perTimeUnit": "MINUTES", "score": 75},
            {"allowance": 50, "perTimeValue": 2, "perTimeUnit": "MINUTES", "score": 300},
            {"allowance": 100, "perTimeValue": 1, "perTimeUnit": "HOURS", "score": 500}
        ]
} }'
```

# Contributor
Andre Charton from eBay Kleinanzeigen Germany: acharton@ebay-kleinanzeigen.de
Matthias Huttar from eBay Kleinanzeigen Germany: mhuttar@ebay-kleinanzeigen.de
