#! /bin/sh

# This script checks if ports are open, for example from the cloud to a tenant's infra
# Create a file with IP and port pairs, e.g.
#
# 8.8.8.8 80
# 8.8.4.4 5432
# ... etc ...
#
# Now feed this file into this script: open_port_checker.sh <filename>
#
# The timeout for each connection is set to 3 seconds, this should be long enough but feel free to adjust.

readonly timeout=3 # seconds

time while read line; do nc -z -v -w${timeout} $line; done < ${1}

