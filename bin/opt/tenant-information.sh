#!/usr/bin/env bash

set -o nounset

# tenant env
get_region () {
    local ip=$(dig +short ${1}.${2}.comaas.ecg.so)
    echo ${ip} | egrep -q '^10\.32\.24\.' && echo -n ams1
    echo ${ip} | egrep -q '^10\.32\.56\.' && echo -n dus1
}

# tenant region dc
get_info () {
    echo $(curl -m 1 -s $3.$1.$2.comaas.ecg.so/health)
}

# tenant active_dc ams1_version ams1_mode dus1_version dus1_mode
print_row () {
#echo $@
    printf '%-8s | %6s | %-8s | %-9s | %-8s | %-9s\n' $1 $2 $3 $4 $5 $6
}

# key json
from_json () {
    what=${1}
    shift
    v=$(echo ${@} | jq -r .${what})
    if [ -z ${v} ]; then
        v="x"
    fi
    echo ${v}
}

if [ $# == 0 ]; then
    tenants="uk mo mp ca au ek it"
else
    tenants="${@}"
fi

for env in prod sandbox; do
    print_row ${env} active ams1 ams1 dus1 dus1
    print_row tenant dc version mode version mode
    echo "---------------------------------------------------------------"

    for tenant in ${tenants}; do
        active_dc=$(get_region ${tenant} ${env})
        if [ -z ${active_dc} ]; then
            active_dc="-"
        fi

        info_ams1=$(get_info ${tenant} ${env} ams1)
        v_ams1=$(from_json version ${info_ams1})
        p_ams1=$(from_json conversationRepositorySource ${info_ams1})

        if [ ${env} == "sandbox" ]; then
            v_dus1="n/a"
            p_dus1="n/a"
        else
            info_dus1=$(get_info ${tenant} ${env} dus1)
            v_dus1=$(from_json version ${info_dus1})
            p_dus1=$(from_json conversationRepositorySource ${info_dus1})
        fi

        print_row ${tenant} ${active_dc} ${v_ams1} ${p_ams1} ${v_dus1} ${p_dus1}
    done
    echo
done
