DOCKER_COMPOSE  = docker-compose
BROWSE          = open
PROPS2CONSUL	= properties-to-consul

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
