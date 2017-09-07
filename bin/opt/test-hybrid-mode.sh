#!/usr/bin/env bash

# Tests the hybrid migrator by clearing out both data stores and starting up COMaaS with two different configurations
# 
# Currently (22/07/2016) works with the KJCA replyts.properties

export ECG_COMAAS_CENTRAL="`dirname $0`/.." && ECG_COMAAS_CENTRAL=$(cd $ECG_COMAAS_CENTRAL && pwd)
export ECG_COMAAS_VAGRANT="$ECG_COMAAS_CENTRAL/../ecg-comaas-vagrant" && ECG_COMAAS_VAGRANT=$(cd $ECG_COMAAS_VAGRANT && pwd)

echo -n "This will clear out your Vagrant's Riak and Cassandra data stores. Are you sure [y/N]? "

read ANSWER

if [ ! "$ANSWER" = "y" ] ; then
  exit 0
fi

# Reset both data stores

cd $ECG_COMAAS_VAGRANT

vagrant ssh -- -t 'sudo service cassandra stop && sleep 2' 2>/dev/null
vagrant ssh -- -t 'sudo rm -rf /var/lib/cassandra/*' 2>/dev/null
vagrant ssh -- -t 'sudo service cassandra start && sleep 2' 2>/dev/null

vagrant ssh -- -t 'sudo service riak stop 1>/dev/null && sleep 2' 2>/dev/null
vagrant ssh -- -t 'sudo rm -rf /var/lib/riak/*' 2>/dev/null
vagrant ssh -- -t 'sudo service riak start 1>/dev/null && sleep 2' 2>/dev/null

cd $ECG_COMAAS_CENTRAL

echo -e "\nWaiting for Cassandra and Riak to come back up (10s)" && sleep 10

echo -e "\nRe-initializing Cassandra data model"

bin/setup-cassandra.sh 1>/dev/null

# Start COMaaS with persistence.strategy = riak

sed -i '' 's/persistence\.strategy=hybrid/persistence\.strategy=riak/' distribution/conf/kjca/local/replyts.properties

bin/build.sh -T kjca -E 1>strategy.riak.log 2>&1 &

FORKED_PID=$!

echo -e "\nStarted COMaaS #1 with PID $FORKED_PID and strategy.riak.log (60s)" && sleep 60

echo -e "\nPlacing message #1 in the mailreceiver dropfolder"

# Add e-mail to the mailreceiver folder

cat << EOF > /tmp/mailreceiver/mail1
From: jvanveghel@ebay.com
Delivered-To: ptroshin@ebay.com
X-ADID: 12345
Subject: First message!

This is the first message.
EOF

mv /tmp/mailreceiver/mail1 /tmp/mailreceiver/pre_mail1

# Sleep a while and then kill the COMaaS instance

echo -e "\nWaiting for message to be processed (10s)" && sleep 10

JAVA_PID=$(ps aux | grep -v grep | grep java | grep 'clean verify$' | xargs | cut -d' ' -f2)

kill -9 $FORKED_PID $JAVA_PID

echo -e "\nWaiting until instance is shut down cleanly (20s)" && sleep 20

# Start COMaaS with persistence.strategy = hybrid

sed -i '' 's/persistence\.strategy=riak/persistence\.strategy=hybrid/' distribution/conf/kjca/local/replyts.properties

bin/build.sh -T kjca -E 1>strategy.hybrid.log 2>&1 &

FORKED_PID=$!

echo -e "\nStarted COMaaS #2 with PID $FORKED_PID and strategy.hybrid.log (60s)" && sleep 60

echo -e "\nPlacing message #2 in the mailreceiver dropfolder"

# Add e-mail to the mailreceiver folder

cat << EOF > /tmp/mailreceiver/mail2
From: jvanveghel@ebay.com
Delivered-To: ptroshin@ebay.com
X-ADID: 12345
Subject: Second message!

This is the second message.
EOF

mv /tmp/mailreceiver/mail2 /tmp/mailreceiver/pre_mail2

# Sleep a while and then kill the COMaaS instance

echo -e "\nWaiting for message to be processed (10s)" && sleep 10

JAVA_PID=$(ps aux | grep -v grep | grep java | grep 'clean verify$' | xargs | cut -d' ' -f2)

kill -9 $FORKED_PID $JAVA_PID

echo -e "\nWaiting until instance is shut down cleanly (20s)" && sleep 20

# Restore replyts.properties to the original strategy (for now)

sed -i '' 's/persistence\.strategy=hybrid/persistence\.strategy=riak/' distribution/conf/kjca/local/replyts.properties

# Results can now be received

KEY=$(curl -s "http://replyts.dev.kjdev.ca:8098/buckets/conversation/keys?keys=true" | jq -r .keys[0])

echo -e 'You can review Riak data here:\n\ncurl -s "http://replyts.dev.kjdev.ca:8098/buckets/conversation/keys/'"${KEY}"'" | gunzip | jq'
echo -e '\nYou can review Cassandra data here:\n\ncqlsh -e "SELECT * FROM replyts2.core_conversation_events" replyts.dev.kjdev.ca 9042'
