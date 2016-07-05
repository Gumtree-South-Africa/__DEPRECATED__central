# kjca-filter-newreplier

Originally taken from https://github.corp.ebay.com/ecg-kijiji-ca/replyts2-ca-filters-plugin
(original git hash: eea6b74f016e3bace2299e09272b6043797fbc7c)

# Description

## New User Filter

Calls the TNS back-end to verify if the replier is a new user and score accordingly
(i.e. delay replies from new user).

The JSON config should be like the following:

```
curl -X PUT http://localhost:8081/configv2/ca.kijiji.replyts.newreplierfilter.NewReplierFilterFactory/Default -H "Content-Type: application/json" -d '
    {  "priority": 50, 
       "state": "ENABLED", 
       "configuration": {
         runForCategories: [10, 174],
         skipForCategories: [14, 274],
         isNewScore: 100,
         gridApiEndpoint: "http://tns-server/path/to/api",
         gridApiUser: "basicAuthUser",
         gridApiPassword: "basicAuthPassword"
       }
    }'    
```

Note about security: putting the credentials in this config is an accepted risk since the TNS API is only available from
the ebay network and is currently limited to read-only operations for the replyTS user, which is unlikely to change.
The alternatives would be to "pollute" replyts.properties with Canada specific properties in core-runtime or build a
configuration system specific per plugin and environment.
