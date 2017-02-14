DOCKER_COMPOSE  = docker-compose
BROWSE          = open
PROPS2CONSUL	= docker run --net host --rm --volume $$PWD/..:/code registry.ecg.so/properties-to-consul:0.0.2

.PHONY: start up down ui import logs

up: start import

start:
	@$(DOCKER_COMPOSE) up -d

down:
	@$(DOCKER_COMPOSE) down --remove-orphans --volumes

ui:
	@$(BROWSE) http://localhost:8082 http://localhost:8500 http://localhost:8090

import:
	@$(PROPS2CONSUL) -file /code/distribution/conf/mp/import_into_consul/docker.properties
	@$(PROPS2CONSUL) -file /code/distribution/conf/ebayk/import_into_consul/docker.properties
	@$(PROPS2CONSUL) -file /code/distribution/conf/gtuk/import_into_consul/docker.properties
	@$(PROPS2CONSUL) -file /code/distribution/conf/mde/import_into_consul/docker.properties

logs:
	@$(DOCKER_COMPOSE) logs -f

