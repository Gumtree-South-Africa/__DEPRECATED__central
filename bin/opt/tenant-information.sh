#!/usr/bin/env bash
# Executes tenant-information.sh on a remote server that has access (since Comaas APIs won't be directly accessible from local laptops anymore).

set -o nounset
set -o errexit

ssh shellserver001.comaas-lp.ams1 "bash -s" -- < $(dirname $0)/_tenant-information.sh $@
