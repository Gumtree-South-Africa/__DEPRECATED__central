#!/usr/bin/env bash

get_region () {
    ip=$1
    echo $ip | egrep -q '^10\.32\.24\.' && echo -n ams1
    echo $ip | egrep -q '^10\.32\.56\.' && echo -n dus1
}

for env in prod sandbox; do
    echo Environment: ${env}
    for tenant in uk mo mp ca au ek; do
        ip=$(dig +short ${tenant}.${env}.comaas.ecg.so)
        region=$(get_region ${ip})
        if [ -z ${region} ]; then
            region="n/a"
        fi
        printf "${tenant}:\t${ip}\t(region: ${region})\n"
    done
    echo
done

