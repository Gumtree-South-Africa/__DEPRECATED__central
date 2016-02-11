#!/bin/bash

set -o nounset
set -o errexit

PKG=$1
FOLDER=${PKG%.tar.gz}

tar -xvf ${PKG}
./${FOLDER}/bin/comaas 2>&1 | tee logs/replyts.log

