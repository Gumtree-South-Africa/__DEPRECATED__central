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

Clone the Cassandra docker library:
`git clone https://github.com/docker-library/cassandra.git`
Switch to the `2.1` subdirectory.

Remove the `VOLUME` statement in the Dockerfile.

This is needed because `docker commit` does not commit the `VOLUME`. The Cassandra Dockerfile has a
handy `VOLUME` statement for

Build the new image:
`docker build -t cas_temp .`

Run the newly created image:
`docker run --rm -p 9042:9042 --name cassandra cas`
Wait for Cassandra to settle (INFO  18:54:45 Listening for thrift clients...)

Now run the `setup-cassandra.sh` script against this container:
`docker run --rm --volume ~/dev/ecg-comaas-central:/code -w /code --link cassandra:cassandra cassandra:2.1.15 bin/setup-cassandra.sh cassandra replyts2`
Note that you might need to change the volume to point to where you checked out the Comaas code.

The original container now has the `replyts2` keyspace in a directory inside the container. Commit this container as an image:
`docker commit cassandra registry.ecg.so/cassandra_data:0.0.1`

Push the image to our repository:
`docker push registry.ecg.so/cassandra_data:0.0.1`

We're done.
