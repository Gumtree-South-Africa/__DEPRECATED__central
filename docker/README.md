## Run Docker instead of Vagrant
```
bin/build.sh -T mp -P docker
cd docker
make up ui logs
```
Now you are ready to start Comaas.

## UIs
Consul: [http://localhost:8500/](http://localhost:8500/)
mail-sink: [http://localhost:8090/](http://localhost:8090/)
Graphite: [http://localhost:8082/](http://localhost:8082/)

## Create the Cassandra image with the replyts2 keyspace
Note: only do this when the `.cql` files have changed, or when upgrading C* versions.

Update the version number in the `Makefile`, in `docker-compose.yml`, and in `bin/_cassandra_docker.sh`.

`make cassandra-image`

We're done. If the upload fails, just run the same `make` command again.
