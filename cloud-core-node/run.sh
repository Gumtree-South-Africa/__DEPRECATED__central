#!/bin/bash

set -o nounset
set -o errexit

PKG=$1
FOLDER=${PKG%.tar.gz}

rm -rf ${FOLDER}
tar -xvf ${PKG}
rm -rf ${FOLDER}/conf/*
cp conf/* ${FOLDER}/conf/
./${FOLDER}/bin/comaas 2>&1 | tee logs/replyts.log

