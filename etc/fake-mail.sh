#! /usr/bin/env bash

# Usage:
#   fake-mail.sh
#       Send messages until ctrl-c
#   fake-mail.sh <nr of messages>
#       Send a specific number of messages
#
# In order to run this script you will need a version of bash that is newer than the one that comes with standard OSX.
# Run `brew install bash` to get a proper version.
#
# Output is a dot per message or the number of messages created so far for every tenth message.

TARGET_DIR=/tmp/mailreceiver
mkdir -p ${TARGET_DIR}

function create_mail {
  cat > $(mktemp ${TARGET_DIR}/pre_mail_XXXXXXXXXX) <<End-of-message
From:buyer@example.com
Delivered-To: seller@example.com
X-ADID:12345
Subject:This is a subject

HELLO

End-of-message
}

NR_OF_MESSAGES=$1
if [ ! -z ${NR_OF_MESSAGES} ]; then
    re='^[0-9]+$'
    if ! [[ ${NR_OF_MESSAGES} =~ $re ]] ; then
        echo "error: ${NR_OF_MESSAGES} is not a number" >&2; exit 1
    fi
fi

echo -n "Starting mail stream to $TARGET_DIR with "
if [ ! -z ${NR_OF_MESSAGES} ]; then
    echo "$NR_OF_MESSAGES message(s)"
else
    echo "unlimited messages"
fi

COUNTER=1
while [ -z ${NR_OF_MESSAGES} ] || [ ${COUNTER} -lt ${NR_OF_MESSAGES} ]; do
    create_mail $(printf '%(%s)T\n' -1)

    if [ $(expr ${COUNTER} % 10) -eq 0 ]; then
        echo -n ${COUNTER}
    else
        echo -n .
    fi

    let COUNTER=COUNTER+1
    sleep 1
done

echo
echo Done!
