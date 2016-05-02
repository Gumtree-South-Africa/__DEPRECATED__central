# kjca-filter-volume

Originally taken from https://github.corp.ebay.com/ecg-kijiji-ca/replyts2-volumefilter-plugin@kijiji.ca
(original git hash: 068ef1d48e39e7e6d302c82e78e61353ea076451)

# Description

Migration of the ReplyTS 1 volumefilter plugin to ReplyTS 2. Made by eBay Kleinanzeigen. Heavily modified by Kijiji Canada.

Volume filter manages multiple mail sending quotas per mail address (maximum number of mails per time unit). If a quota is violated,
a configurable score is assigned to the incoming message.

Multiple Quotas can be defined (e.g. max. 10 mails in one hour AND max. 30 mails per day). If more than one quota is exceeded,
the highest score is assigned to the message.
(e.g. max 10/hour -> score 50, max 30/day -> score 100: if 31 mails are received by a user within 5 minutes, both quotas
are violated and a score of 100 is assigned, because 100 is bigger than 50).

The filter can be configured to remember quota violations for some time. Once an email address is "remembered" all emails sent
from it will be assigned the score of the quota it exceeded until the memory timeout expires.

Follow-up emails (those that don't contain a X-ADID header) can be ignored by the filter with the appropriate config option.

The filter can be configured to run only in specific categories. See ActivableFilter.

The filter will use Hazelcast to communicate with other nodes in the ReplyTS cluster about received mails and will use
the Esper Event Processing Framework (http://esper.codehaus.org) to analyze the incoming stream.

When a ReplyTS node is restarted, all information is lost and can not be recovered.

# Filter configuration

Create a filter config via API:

```
curl -X PUT http://kreplyts44-1.220:42001/configv2/com.ecg.de.kleinanzeigen.replyts.volumefilter.VolumeFilterFactory/VolumeFilter -H "Content-Type: application/json" -d '
{
    state: "ENABLED",
    priority: 100,
    configuration: {
        "rules": [
            {"allowance": 20, "perTimeValue": 2, "perTimeUnit": "MINUTES", "score": 100, "scoreMemoryDurationValue": 8, "scoreMemoryDurationUnit": "HOURS"},
            {"allowance": 50, "perTimeValue": 2, "perTimeUnit": "MINUTES", "score": 300},
            {"allowance": 100, "perTimeValue": 1, "perTimeUnit": "HOURS", "score": 500}         
        ],
        "runFor": {
          "exceptCategories": [],
          "categories": [
            10
          ] 
        },
        "ignoreFollowUps": true
    }
}'
```

Allowed values for `perTimeUnit` are all provided by `java.util.concurrent.TimeUnit` except for anything smaller than `SECONDS`:

* `DAYS`
* `HOURS`
* `MINUTES`
* `SECONDS`

`scoreMemoryDurationUnit` and `scoreMemoryDurationValue` are optional. If not provided, quota violations aren't remembered.

`ignoreFollowUps` is optional and defaults to `false`.

# Contributor

* Andre Charton from eBay Kleinanzeigen Germany: acharton@ebay-kleinanzeigen.de
* Matthias Huttar from eBay Kleinanzeigen Germany: mhuttar@ebay-kleinanzeigen.de
* Dmitri Vassilenko from Kijiji Canada: dvassilenko@ebay.com
