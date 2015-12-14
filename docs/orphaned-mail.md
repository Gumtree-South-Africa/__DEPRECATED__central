ORPHANED emails
---------------

To investigate cause of ORPHANED emails, do the following:

Build tunnel to ES machine and then open Sense.

* Demo: `ssh -L 9204:localhost:9200 esmaster001.dro.comaas.demo.mp.ecg.so`, [Sense](localhost:9204)
* L&P: `ssh -L 9205:localhost:9200 mp-rtsesmaster001.replytslp.ams01`, [Sense](localhost:9205)
* PROD: `ssh -L 9206:localhost:9200 esmaster001.esh.ops.prod.comaas`, [Sense](localhost:9206)

If you use the Sense Chrome plugin, connect to `localhost:9204`, `localhost:9205` or `localhost:9206`.

In Sense execute following query:

```
GET replyts_*/_search
{
  "sort" : [
        { "conversationStartDate" : {"order" : "desc"}}
  ],
  "query" : {
      "term" : { "messageState" : "ORPHANED" }
  }
}
```

(Also useful is the query `GET replyts_mail.gemaaktvoorelkaar.nl/_mapping`.)

Look at result, for example in the fragment
```"hits": [
         {
            "_index": "replyts_mail.gemaaktvoorelkaar.nl",
            "_type": "message",
            "_id": "2uvu:-oqbvtw:ihxogvlk/2uvt:-oqbvtw:ihxogvlk",
            "_score": null,
            "sort": [
               1449753332510
            ]
         },```
the following is a conversation id: `2uvu:-oqbvtw:ihxogvlk`.

With DevCenter (see [Debugging Cassandra with DeveCenter](cassandra-debugging.md)) execute:

```select * from core_conversation_events where conversation_id = '2uvu:-oqbvtw:ihxogvlk';```

to find conversation with orphaned message.

Select second event (MessageAddedEvent) and find the `To` header. In this case the value
is `"a-2xkcvzmxilw9m@mail.gemaaktvoorelkaar.nl"`.

Take out the ‘secret’ which is `2xkcvzmxilw9m`. Use to query cassandra again:
`select * from core_conversation_secret where secret = '2xkcvzmxilw9m';`

And now to the conclusion!:
* If you find a record, the mail arrived too fast. The `core_conversation_secret` was not written yet when the reply came in.
* If you didn’t find a record, a bad person tried to guess an email address. Nothing to be done about this.