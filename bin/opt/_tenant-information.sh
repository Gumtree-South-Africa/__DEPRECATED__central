#!/usr/bin/env bash

set -o nounset

# env
get_region () {
    if [ ${env} == "lp" ]; then
        echo "AMS1"
    else
        echo $(dig +short -t TXT ${1}.comaas.cloud | awk -F"=" '{print $2}' | awk -F'"' '{print $1}')
    fi
}


# env dc
get_dc_ip() {
    echo $(dig +short dmz-vip.comaas-${1}.${2}.cloud)
}

# tenant env dc
get_info () {
    uri="https://${1}.${2}.comaas.cloud/health"
    dc_id=$(get_dc_ip ${2} ${3})
    echo $(curl --resolve ${1}.${2}.comaas.cloud:443:${dc_id} --max-time 1 --silent ${uri})
}

# tenant active_dc ams1_version ams1_mode dus1_version dus1_mode
print_row () {
#echo $@
    printf '%-8s | %-12s | %-12s | %-4s\n' $1 $2 $3 $4
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
    tenants="ar au be ca ek it mo mp mvca mx sg uk za"
else
    tenants="${@}"
fi

for env in prod lp; do
    active_dc=$(get_region ${env})
    echo ""
    echo "${env} active dc = $active_dc"
    echo ""
    print_row ${env} ams1 dus1 same
    print_row tenant version version vers
    echo "-------------------------------------------------------"

    for tenant in ${tenants}; do
        info_ams1=$(get_info ${tenant} ${env} ams1)
        v_ams1=$(from_json version 0 ${info_ams1})

        if [ ${env} == "lp" ]; then
            v_dus1="n/a"
            p_dus1="n/a"
        else
            info_dus1=$(get_info ${tenant} ${env} dus1)
            v_dus1=$(from_json version 0 ${info_dus1})
        fi

        equal="yes"
        if [ "${v_dus1}" != "n/a" ] && [ "${v_ams1}" != "${v_dus1}" ]; then
            equal="no"
        fi

        print_row ${tenant} ${v_ams1} ${v_dus1} ${equal}
    done
    echo
done

