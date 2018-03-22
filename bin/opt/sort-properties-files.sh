#!/usr/bin/env bash
# This sorts properties in all *.properties files
# Later improvement: make this into a git precommit hook automatically (or via the README), as follows:
# `cp bin/opt/sort-properties-files.sh .git/hooks/pre-commit`

set -o nounset
set -o errexit

for i in $(find . -name \*properties); do cat $i | sort | uniq > $i.bak; mv $i.bak $i; done
