#! /bin/sh

# Remove your tenant from the list if it is live in the cloud in any persistence strategy.
# This is used in other scripts to make sure there are no legacy packages built or uploaded.

function isTenantLegacy () {
    readonly LEGACY_TENANTS=(bt dk kjca)
    local match="$1"
    for tenant in "${LEGACY_TENANTS[@]}"; do
    echo $tenant
        if [[ "$tenant" == "$match" ]]; then
            return 0
        fi
    done
    return 1
}
