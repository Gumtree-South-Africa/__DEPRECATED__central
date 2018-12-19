#!/usr/bin/env bash

TENANT=$1

die(){
	echo FATAL: $@
	exit 1

}

[ -z "$TENANT" ] && die "Need a tenant name"

CONFDIR=${PWD}/distribution/conf
TENANT_PROPERTIES=$CONFDIR/$TENANT/docker.properties

[ -d "$CONFDIR" ] || die "Confdir not found: $CONFDIR . Are you in the root of the ecg-comaas/central repository?"
[ -f "$TENANT_PROPERTIES" ] || die "No properties config file for tenant $TENANT: $TENANT_PROPERTIES"

echo "Loading to consul: tenant properties from file $TENANT_PROPERTIES"

docker run --network comaasdocker_default --rm \
	--volume $TENANT_PROPERTIES:/props.properties \
        dock.es.ecg.tools/comaas/properties-to-consul:0.0.7 -consul http://comaasdocker_consul_1:8500 \
	-tenant $TENANT

