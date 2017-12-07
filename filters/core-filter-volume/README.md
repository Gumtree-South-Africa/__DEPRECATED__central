# Description

The volume filter maintains multiple mail sending quotas per mail address (maximum number of mails per time unit). 
If a quota is violated, a configurable score is assigned to the incoming message.

Multiple Quotas can be defined (e.g. max. 10 mails in one hour AND max. 30 mails per day). If more than one quota is exceeded,
the highest score is assigned to the message.
(e.g. max 10/hour -> score 50, max 30/day -> score 100: if 31 mails are received by a user within 5 minutes, both quotas
are violated and a score of 100 is assigned, because 100 is bigger than 50)

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
