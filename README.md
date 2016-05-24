# Comaas central

## Dev setup
Follow the steps mentioned [here](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-central/wiki#set-up-code-review) to 
clone this repository.
Also clone the [vagrant machine](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-vagrant) and start it (see [here](https://github.corp.ebay.com/ecg-comaas/ecg-comaas-vagrant#get-started)). 

Download Cassandra from http://archive.apache.org/dist/cassandra/2.1.11/ to your machine and put it in /opt/cassandra.

Run `setup-cassandra.sh` to run initial db migrations.

To build comaas from IDE add -Drevision=(ANY_NUMBER) to maven configuration 
![IntelleJ Maven Config](/docs/comaas_maven_config.jpg)

To run COMaaS for a specific tenant from your IDE, use the following Run configuration:

* Type: Maven
* Name: COMaaS for: [name of tenant, mp|mde|ebayk]

* Parameters / Working directory: [full path to the ecg-comaas-central folder]
* Parameters / Command line: verify -Dmaven.exec.skip=false
* Parameters / Profiles: [name of tenant, e.g. mp]

* General / Maven home directory: [make sure you select the latest version, usually not the IntelliJ built-in, e.g. 3.3.9]
* General / User settings file: etc/settings.xml (select 'Override' to override)
* General / Make sure "Execute goals recursively" is checked

* Runner / VM arguments:
  ```
  -Drevision=running-from-ide
  -DconfDir=distribution/conf/<name of tenant, e.g. mp>/local
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
  -Djavax.net.ssl.trustStore=comaas.jks
  -Djavax.net.ssl.trustStorePassword=comaas
  ```
* Skip tests: check this

This will call exec:java through the 'verify' phase in the distribution module. 'maven.exec.skip' is normally true, which prevents exec:java from being called prior to the normal 'deploy' phase.

### Running replyTS on comaas-vagrant
* If you see elasticsearch/cassadra/kafka error messages first try ```vagrant provision```, that should make sure that all services are up and running.
 If that does not help check if these services are running in Vagrant machine
  ```
  vagrant ssh
  sudo /etc/init.d/elasticsearch status 
  sudo /etc/init.d/kafka status
  sudo /etc/init.d/cassandra status
  ```
 and manually start them if necessary
 
### ebayk tenant on comaas-vagrant
*  Set search.es.clustername=replytscluster in distribution/conf/ebayk/replyts.properties
*  After starting ReplyTS launch jconsole and invoke ReplayTS.ClusterModeControl.Operations.switchToFailoverMode so email processing works. 
For more information on this see https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki/Two%20Datacenter%20Operations

### mde tenant on comaas-vagrant
*  Set search.es.clustername=replytscluster in distribution/conf/mde/replyts.properties
*  After starting ReplyTS launch jconsole and invoke ReplayTS.ClusterModeControl.Operations.switchToFailoverMode so email processing works. 
For more information on this see https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki/Two%20Datacenter%20Operations

## MP system overview
![Messaging system overview at Marktplaats](/docs/20151221-messaging-system-overview.jpg)

## Notes on (future) tenants:

eBay Annunci (Italy) is using a quite clean replyts2-core fork (with Riak storage) plus some custom plugin (mainly for monitoring purpose).   
They forked the message box plugin from the GTAU one and customized it by adding some endpoints (e.g. direct conversation/message creation) and cleanup strategies.  

core: https://github.corp.ebay.com/annunci/replyts2-core  
message box plugin: https://github.corp.ebay.com/annunci/replyts2-ebayk-message-center
