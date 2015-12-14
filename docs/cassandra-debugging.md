Cassandra debugging with DevCenter
==================================

DevCenter is a nice tool to use Cassandra with auto completion, clickable interface etc.

Download and install DevCenter ([Download link](http://www.datastax.com/what-we-offer/products-services/devcenter)).

Create a tunnel to access Cassandra:
```
-- DEMO tunnel:
-- ssh -NL 9043:cass001.dro.comaas.demo.mp.ecg.so:9042 seed001

-- L&P tunnel:
-- ssh -NL 9044:mp-rtscass001.replytslp.ams01:9042 mp-shellserver001.opslp.ams01

-- PROD tunnel:
-- ssh -NL 9045:cass001.esh.ds.prod.comaas.ecg.so:9042 10.34.122.14
```

Create new connection in the `Connections` view. Add a single hostname (`localhost`) and fill in
the 'native port' from above (`9043`, `9044` or `9045`). Set compression to 'none'.

Create a script (you will be prompted). Make sure to select the correct connection, and then the `replyts2` keyspace.