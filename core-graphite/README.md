# core-graphite

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-graphite-plugin
(original git hash: 53c95acccddb4c3942822e2b07edce91e1286c7b)

# Description

Makes all metrics collected in ReplyTS via the Codahale Metrics library available to Graphite.

Add this configuration in your `replyts.properties`:

```
graphite.enabled=true # true is default
graphite.endpoint.hostname=graphitehost.com
graphite.endpoint.port=2003
graphite.timeperiod.sec=60
graphite.prefix=some.prefix
```
