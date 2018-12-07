#!/bin/sh

TENANT=$1

die(){
	echo FATAL: $@
	exit 1

}

[ -z "$TENANT" ] && die "Need a tenant name"

CONFDIR=${PWD}/distribution/conf
TENANT_CONFIG=$CONFDIR/$TENANT/docker.properties

[ -d "$CONFDIR" ] || die "Confdir not found: $CONFDIR . Are you in project-Central root?"
[ -f "$TENANT_CONFIG" ] || die "No config file for tenant $TENANT: $TENANT_CONFIG "

echo "Loading to consul: tenant config file $TENANT_CONFIG"

docker run --network comaasdocker_default --rm \
	--volume ${PWD}/distribution/conf/$TENANT/docker.properties:/props.properties \
        dock.es.ecg.tools/comaas/properties-to-consul:0.0.7 -consul http://comaasdocker_consul_1:8500 \
	-tenant $TENANT

