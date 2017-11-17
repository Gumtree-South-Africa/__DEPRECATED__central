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
to=seller@example.com
from=buyer@example.com
adid=12345

function create_mail {
  cat > $(mktemp ${TARGET_DIR}/pre_mail_XXXXXXXXXX) <<End-of-message
HELO: <example.com>
FROM:<$from>
Delivered-To: <$to>
Return-Path: $from
Subject:This is a subject
X-ADID: $adid
Date: $now
Reply-To: $from
X-Mailer: xMailer
X-REPLY-CHANNEL: desktop
X-Cust-from-userid: $from
X-Cust-to-userid: $to
X-USER-MESSAGE: May I buy this $adid
X-Original-To: $to
To: $to

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
    adid=$(( ( RANDOM % 999999999999999999 )  + 1000000000000000000 ))
    from="buyer$adid@example.com"
    to="seller$adid@example.com"
    now=$(date)
    create_mail $(printf '(%s)T\n' -1)

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
