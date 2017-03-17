#!/usr/bin/env bash
#Start RabbitMq Server with management: http://containter-ip:15672
docker run -d -p 15672:15672 -p 5672:5672 --hostname local-rabbit --name rabbit-server -e RABBITMQ_ERLANG_COOKIE='gumtree' rabbitmq:3-management

#Connect via terminal
docker run -it --rm --link rabbit-server:local-rabbit -e RABBITMQ_ERLANG_COOKIE='gumtree' -e RABBITMQ_NODENAME=rabbit@local-rabbit rabbitmq:3 bash
