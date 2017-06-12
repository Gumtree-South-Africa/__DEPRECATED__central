## Run Docker instead of Vagrant
```
cd docker
make up
```
Now you are ready to start Comaas:

```
bin/build.sh -T mp -P docker -E
```

## UIs
Consul: [http://localhost:8500/](http://localhost:8500/)
mail-sink: [http://localhost:8090/](http://localhost:8090/)
Graphite: [http://localhost:8082/](http://localhost:8082/)

## More commands
Check out the Makefile for more helpful commands, like `make ui`, and `make logs`.

To get to the `cqlsh` shell, run `docker-compose exec cassandra cqlsh -k replyts2`.

## Create the Cassandra image with the replyts2 keyspace
Note: only do this when the `.cql` files have changed, or when upgrading C* versions.

Update the version number in the `Makefile`, in `docker-compose.yml`, and in `bin/_cassandra_docker.sh`.

`make cassandra-image`

We're done. If the upload fails, just run the same `make` command again.
