#!/usr/bin/env bash

set -o nounset

# tenant env
get_region () {
    local ip=$(dig +short ${1}.${2}.comaas.cloud)
    echo ${ip} | egrep -q '^10\.32\.24\.' && echo -n ams1
    echo ${ip} | egrep -q '^10\.32\.56\.' && echo -n dus1
}

# tenant region dc
get_info () {
    echo $(curl -m 1 -s ${3}.${1}.${2}.comaas.cloud/health)
}

# tenant active_dc ams1_version ams1_mode dus1_version dus1_mode
print_row () {
#echo $@
    printf '%-8s | %6s | %-12s | %-4s | %-12s | %-4s\n' $1 $2 $3 $4 $5 $6
}

# key json
from_json () {
    what=${1}
    shift
    len=${1}
    shift
    v=$(echo ${@} | jq -r .${what})
    if [ -z ${v} ]; then
        v="x"
    fi
    if [ $len -gt 0 ]; then
        echo ${v:0:$len}
    else
        echo ${v}
    fi
}

if [ $# == 0 ]; then
    tenants="ar ek au uk it ca mo mp mx sg za"
else
    tenants="${@}"
fi

for env in prod lp; do
    print_row ${env} active ams1 ams1 dus1 dus1
    print_row tenant dc version mode version mode
    echo "---------------------------------------------------------------"

    for tenant in ${tenants}; do
        active_dc=$(get_region ${tenant} ${env})
        if [ -z ${active_dc} ]; then
            active_dc="-"
        fi

        info_ams1=$(get_info ${tenant} ${env} ams1)
        v_ams1=$(from_json version 0 ${info_ams1})
        p_ams1=$(from_json conversationRepositorySource 4 ${info_ams1})

        if [ ${env} == "lp" ]; then
            v_dus1="n/a"
            p_dus1="n/a"
        else
            info_dus1=$(get_info ${tenant} ${env} dus1)
            v_dus1=$(from_json version 0 ${info_dus1})
            p_dus1=$(from_json conversationRepositorySource 4 ${info_dus1})
        fi

        print_row ${tenant} ${active_dc} ${v_ams1} ${p_ams1} ${v_dus1} ${p_dus1}
    done
    echo
done

