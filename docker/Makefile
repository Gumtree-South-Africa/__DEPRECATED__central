DOCKER_COMPOSE  = docker-compose
BROWSE          = open
PROPS2CONSUL    = properties-to-consul
CASS_IMG_VER    = 0.0.5

.PHONY: start up down ui import logs

up: start import

start:
	@$(DOCKER_COMPOSE) up -d

down:
	@$(DOCKER_COMPOSE) down --remove-orphans --volumes

ui:
	@$(BROWSE) http://localhost:8500

import:
	@find ../distribution/conf -name docker.properties | xargs -n1 $(PROPS2CONSUL) -file

logs:
	@$(DOCKER_COMPOSE) logs -f

cassandra-image:
	@find .. -name \*cql -not -path "../docker/*" -exec cp {} cassandra/ \;
	@docker build -t registry.ecg.so/comaas_cassandra_data:$(CASS_IMG_VER) cassandra
	@rm -f cassandra/*cql

push-cassandra-image:
	@docker push registry.ecg.so/comaas_cassandra_data:$(CASS_IMG_VER)
