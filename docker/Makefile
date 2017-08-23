DOCKER_COMPOSE = docker-compose
BROWSE         = open
PROPS2CONSUL   = docker run --net docker_default --rm --volume $$PWD/..:/code -w /code/docker docker-registry.ecg.so/comaas/properties-to-consul:0.0.4 -consul http://docker_consul_1:8500
CASS_IMG_VER   = 0.0.6
CASS_IMG_NAME  = docker-registry.ecg.so/comaas/cassandra_data:$(CASS_IMG_VER)

.PHONY: up down ui import logs cassandra-image push-cassandra-image pull

up:
	@$(DOCKER_COMPOSE) up -d

down:
	@$(DOCKER_COMPOSE) down --remove-orphans --volumes

ui:
	@$(BROWSE) http://localhost:8091

import:
	find ../distribution/conf -name docker.properties -exec $(PROPS2CONSUL) -file {} \;

logs:
	@$(DOCKER_COMPOSE) logs -f

cassandra-image:
	@find .. -name \*cql -not -path "../docker/*" -exec cp {} cassandra/ \;
	@docker build -t $(CASS_IMG_NAME) cassandra
	@rm -f cassandra/*cql

push-cassandra-image:
	@docker push $(CASS_IMG_NAME)

pull:
	@$(DOCKER_COMPOSE) pull
	@docker pull basho/riak-kv:2.1.4
