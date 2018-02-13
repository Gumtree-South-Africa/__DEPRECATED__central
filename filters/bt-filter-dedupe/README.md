# bt-filter-ip

Originally taken from https://github.corp.ebay.com/ecg-global/replyts2-dedupe-filter
(original git hash: c2e9ad1a707f3d28659c5b66aa250829fd9a79e5)

# replyts2-dedupe-filter

Plugin used for detecting the spam messages to the seller. This rules based filter uses the below to detect the duplicates
  
  * Adid
  * Ip Address of the seller
  * Receiver Email Address
  * Uses Elasticsearch proximity relevance on the message text
  
  Sample rules for this filter given below
  ``` 
  {
    "rules": {
      "minimumShouldMatch": 80%,
      "lookupInterval": 15,
      "lookupIntervalTimeUnit": "MINUTES",
      "score": 2000,
      "matchCount": 3
    },
    "runFor": {
      "exceptCategories": [],
      "categories": []
    }
  }
  ```
   
   
   Use the below `curl` command to register and activate the plugin with rules
   
   ```
   curl -X PUT http://{hostname:port}/configv2/com.ebay.ecg.bolt.replyts.dedupefilter.DeDupeFilterFactory/DeDupeFilter 
   -H "Content-Type: application/json" 
   -d '{state: "ENABLED",priority: 60,configuration: {"rules": {"minimumShouldMatch": "80%","lookupInterval": 15,"lookupIntervalTimeUnit": "MINUTES","score": 2000,"matchCount": 3},"runFor": {"exceptCategories": [],"categories": []}}}'
   ```
