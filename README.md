# Comaas central

## Dev setup
Follow the steps mentioned [here](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-central/wiki#set-up-code-review) to 
clone this repository.
Also clone the [vagrant machine](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-vagrant) and start it (see [here](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-vagrant#get-started)). 

Download Cassandra from http://archive.apache.org/dist/cassandra/2.1.11/ to your machine.

Run `setup-cassandra.sh` to run initial db migrations.

To run COMaaS for a specific tenant from your IDE, use the following Run configuration:

* Type: Maven
* Name: COMaaS for: <name of tenant, e.g. mp>

* Working directory: <full path to the ecg-comaas-central folder>
* Command line: verify -T1C -Dexec.mainClass=com.ecg.replyts.core.runtime.ReplyTS
* Profiles: <name of tenant, e.g. mp>

* Maven home directory: <make sure you select the latest version, usually not the IntelliJ built-in, e.g. 3.3.9>
* User settings file: etc/settings.xml (select 'Override' to override)

* VM arguments:
  ```
  -DconfDir=distribution/conf/<name of tenant, e.g. mp>
  -DlogDir=/tmp
  -Dmail.mime.parameters.strict=false
  -Dmail.mime.address.strict=false
  -Dmail.mime.ignoreunknownencoding=true
  -Dmail.mime.uudecode.ignoreerrors=true
  -Dmail.mime.uudecode.ignoremissingbeginend=true
  -Dmail.mime.multipart.allowempty=true
  -Dmaven.wagon.http.ssl.insecure=true
  -Dmaven.wagon.http.ssl.allowall=true
  -Dmaven.wagon.http.ssl.ignore.validity.dates=true
  ```
* Skip tests: check this

## MP system overview
![Messaging system overview at Marktplaats](/docs/20151221-messaging-system-overview.jpg)

## Notes on (future) tenants:

eBay Annunci (Italy) is using a quite clean replyts2-core fork (with Riak storage) plus some custom plugin (mainly for monitoring purpose).   
They forked the message box plugin from the GTAU one and customized it by adding some endpoints (e.g. direct conversation/message creation) and cleanup strategies.  

core: https://github.corp.ebay.com/annunci/replyts2-core  
message box plugin: https://github.corp.ebay.com/annunci/replyts2-ebayk-message-center
