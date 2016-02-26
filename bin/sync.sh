#!/bin/bash
# Use this to sync source directory 1 into central directory 2
# Example usage: ./sync.sh ~/dev/ecg-comaas/replyts2-core/integration-test/ integration-tests/core-integration-test/

set -o nounset
set -o errexit

rsync -avz $1/* $2/

