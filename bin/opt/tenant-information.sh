#!/usr/bin/env bash

get_region () {
    ip=$1
    echo $ip | egrep -q '^10\.32\.24\.' && echo -n ams1
    echo $ip | egrep -q '^10\.32\.56\.' && echo -n dus1
}

get_version () {
    echo $(curl -m 1 -s $1.$2.comaas.ecg.so/health | jq -r .version)
}

for env in prod sandbox; do
    echo Environment: ${env}
    printf "tenant\tversion\tregion\tip\n"
    for tenant in uk mo mp ca au ek; do
        ip=$(dig +short ${tenant}.${env}.comaas.ecg.so)
        region=$(get_region ${ip})
        if [ -z ${region} ]; then
            region="n/a"
        fi

        version=$(get_version ${tenant} ${env})
        if [ -z ${version} ]; then
            version="n/a"
        fi
        printf "${tenant}\t${version}\t${region}\t${ip}\n"
    done
    echo
done

