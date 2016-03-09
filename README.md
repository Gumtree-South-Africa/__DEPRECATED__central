# Comaas central

## Dev setup
Follow the steps mentioned [here](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-central/wiki#set-up-code-review) to 
clone this repository.
Also clone the [vagrant machine](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-vagrant) and start it (see [here](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-vagrant#get-started)). 

Download Cassandra from http://archive.apache.org/dist/cassandra/2.1.11/ to your machine.

Run `setup-cassandra.sh` to run initial db migrations.

To run RTS2 create the following run configuration:

* Type: application
* Main class: `nl.marktplaats.replyts2.MarktplaatsReplyTS2Main`
* VM arguments:
  ```
  -DconfDir=replyts2-mp-dist/conf
  -DlogDir=/tmp
  -Dmail.mime.parameters.strict=false
  -Dmail.mime.address.strict=false
  -Dmail.mime.ignoreunknownencoding=true
  -Dmail.mime.uudecode.ignoreerrors=true
  -Dmail.mime.uudecode.ignoremissingbeginend=true
  -Dmail.mime.multipart.allowempty=true
  ```
* Module: `replyts2-mp-dist`
* Working directory: the project directory (which is the default)

## MP system overview
![Messaging system overview at Marktplaats](/docs/20151221-messaging-system-overview.jpg)

## Notes on (future) tenants:

eBay Annunci (Italy) is using a quite clean replyts2-core fork (with Riak storage) plus some custom plugin (mainly for monitoring purpose).   
They forked the message box plugin from the GTAU one and customized it by adding some endpoints (e.g. direct conversation/message creation) and cleanup strategies.  

core: https://github.corp.ebay.com/annunci/replyts2-core  
message box plugin: https://github.corp.ebay.com/annunci/replyts2-ebayk-message-center
