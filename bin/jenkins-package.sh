#!/bin/bash

set -o nounset
set -o errexit

cd distribution/target
tar xvfz "distribution-$TENANT-$ENVNAME.tar.gz"

for prop in ../conf/$TENANT/*;
do
	if [[ -f "$prop" || "$prop" == *comaasqa || "$prop" == *local ]]; then
        continue
    fi

    echo "ecg.comaas-$TENANT-$ENVNAME.tar.gz"
	rm -f distribution/conf/*
    cp "$prop"/* distribution/conf/
    tar cvfz distribution-$TENANT-$(basename "$prop").tar.gz distribution
done

rm -rf distribution
