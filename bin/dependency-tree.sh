#!/bin/bash

if [ "$#" -ne 1 ] ; then
    echo "$0: <tenant>"

    exit 1
fi

mvn \
  -Djavax.net.ssl.trustStore=comaas.jks -Djavax.net.ssl.trustStorePassword=comaas -U \
  -s etc/settings.xml -Drevision=123 -Denv-name=local \
  -P "$1,"'core,core-tests,integration-tests-part1,integration-tests-part2,!distribution' \
  compile dependency:tree
