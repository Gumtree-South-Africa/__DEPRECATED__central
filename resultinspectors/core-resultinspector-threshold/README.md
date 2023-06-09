# core-resultinspector-threshold

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-threshold-resultinspector-plugin
(original git hash: d8c67610d53ad6b0e17fe81cc03d2949aff8b80c)

Migration of the ReplyTS 1 result inspector  plugin to ReplyTS 2. Made by eBay Kleinanzeigen.

The result inspector has thresholds for putting incoming messages to HELD and BLOCKED. It will sum up the score output of all filters applied to the message and then compare with the thresholds. 

# Filter configuration


configurations should look like this:
```
{
    held: 200, 
    blocked: 600
}
```

Putting configuration into ReplyTS example: 
```
curl -X PUT http://localhost:8081/configv2/com.ecg.de.kleinanzeigen.replyts.thresholdresultinspector.ThresholdResultInspectorFactory/Default -H "Content-Type: application/json" -d '
    {  "priority": 100, 
       "state": "ENABLED", 
       "configuration": {"held": 75, "blocked": 300 } }'
```
# Contributor
Matthias Huttar from eBay Kleinanzeigen Germany: mhuttar@ebay-kleinanzeigen.de
