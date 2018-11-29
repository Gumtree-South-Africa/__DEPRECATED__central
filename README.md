# Comaas central

## Dev setup

* Follow the steps mentioned [here](https://github.corp.ebay.com/ecg-comaas/central/wiki#set-up-code-review) to clone this repository.
* Run `git submodule update --init`
* Setup Docker

### Docker

Install [Docker](https://docs.docker.com/engine/installation/mac/) to be able to run tests. Alternatively, `brew cask install docker` and start `Docker.app`.

One time setup:

`docker login dock.es.ecg.tools`

Start all Comaas supporting services by checking out https://github.corp.ebay.com/ecg-comaas/ecg-comaas-docker:
```
cd ecg-comaas-docker
make up
```
Remove all containers using `cd docker; make down`

Note that you will have to install Docker on your local machines, the automated tests rely on it.

### Run/Debug COMaaS for a specific tenant

- Run Docker Containers - `docker` project `make up logs`
- Load properties from the root of `central` project

```
docker run --network comaasdocker_default --rm --volume ${PWD}/distribution/conf/mp/docker.properties:/props.properties \
        dock.es.ecg.tools/comaas/properties-to-consul:0.0.7 -consul http://comaasdocker_consul_1_d1e3b633c48a:8500 -tenant mp
```

- Run Configuration in IntelliJ

![local_development](etc/local_dev.png)

#### Option 1. Build script

```
./bin/build.sh -p
```

### Testing that your setup works

In the docker repo, run `make send`. This puts a message in the `mp_messages`  kafka topic (assuming tenant is mp). 

After being succesfully processed in the Trust & Safety pipeline, the approved message should show up in the `conversation_events`  topic , which you can check at [http://localhost:8073/#/cluster/default/topic/n/conversation_events/data](http://localhost:8073/#/cluster/default/topic/n/conversation_events/data) . 

## Auto-discovery of (cloud) services and properties (Consul)
COMaaS has support for auto-discovery of some cloud services (currently Cassandra, when registered in Consul) as well as configuration properties (Consul KV).

### Consul services
The application will automatically scan for other Consul-registered services. Currently this is Cassandra, Kafka, and ElasticSearch.

### Consul configuration (KV)
In order to add keys to consul, add them with the prefix `comaas/comaas:core:<tenant long name>`. E.g. `comaas/comaas:core:ebayk/persistence.strategy` with value `cassandra`. This prefix approach allows one Consul instance to contain properties for multiple tenants (or the same tenant running on multiple ports).
