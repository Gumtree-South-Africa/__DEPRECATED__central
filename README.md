# Comaas central

## Dev setup

* Follow the steps mentioned [here](https://github.corp.ebay.com/ecg-comaas/central/wiki#set-up-code-review) to clone this repository.
* Run `git submodule update --init`
* Setup Docker

### Docker

Install [Docker](https://docs.docker.com/engine/installation/mac/) to be able to run tests. Alternatively, `brew cask install docker` and start `Docker.app`.

One time setup:

`docker login dock.es.ecg.tools`

Start all Comaas supporting services by checking out https://github.corp.ebay.com/ecg-comaas/ecg-comaas-docker:
```
cd ecg-comaas-docker
make up
```
Remove all containers using `cd docker; make down`

Note that you will have to install Docker on your local machines, the automated tests rely on it.

### Run COMaaS for a specific tenant
 
#### Option 1. IDE

Before running from IDE you have to import properties into consul manually to do that execute:  

```
cd ecg-comaas-docker
export ECG_COMAAS_CENTRAL=/Users/<USER_NAME>/dev/comaas/ecg-comaas-central
make import
```
replace `/Users/<USER_NAME>/dev/comaas/ecg-comaas-central` with you path to `ecg-comaas-central` repository.

* Type: Maven
* Name: COMaaS for: [name of tenant, mp|mde|ebayk]

* Parameters / Working directory: [full path to the ecg-comaas-central folder]
* Parameters / Command line: verify -Dmaven.exec.skip=false
* Parameters / Profiles: [name of tenant, e.g. mp]

* General / Maven home directory: [make sure you select the latest version, usually not the IntelliJ built-in, e.g. 3.3.9, set `User setting file` to <PATH_TO>/ecg-comaas-central/etc/settings.xml]
* General / Make sure "Execute goals recursively" is checked

* Runner / VM arguments:
  ```
  -DconfDir=distribution/conf/<name of tenant, e.g. mp>/docker
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

#### Option 1. Build script

  ```
  ./bin/build.sh -T gtuk -P docker -E -D
  ```
  see bin/README.md for more details
  
### Testing that your setup works

1. Place a raw email in the `/tmp/mailreceiver` (file should start with `pre_`) on your local machine. Example email:
```
From:acharton@ebay-kleinanzeigen.de
Delivered-To: receiver@host.com
X-ADID:12345
X-CUST-FROM-USERID: 20000368
X-CUST-TO-USERID: 20000553
Subject:This is a subject
 
THIS is a test.
```
2. Go to mailhog `http://localhost:8090` and check that your email was sent.

### Native Cassandra

If you want to be able to run Cassandra natively, download Cassandra from http://archive.apache.org/dist/cassandra/2.1.15/ to your machine and put it in /opt/cassandra. Alternatively, use `brew install homebrew/versions/cassandra21`.
This is no longer strictly needed.

If you are using python >= 2.7.12 you need cassandra 2.1.16+ (http://archive.apache.org/dist/cassandra/2.1.16/apache-cassandra-2.1.16-bin.tar.gz) download it and put it in /opt/cassandra.

If you are using older python you can use brew `brew install homebrew/versions/cassandra21`.

1. Run `cassandra` to start the service. Start the VM.
2. Run `bin/setup-cassandra.sh; bin/setup-cassandra.sh localhost` to run initial db migrations on both instances. Integration tests use the local one, while runtime uses the VM.

If you get an error like `Connection error: ('Unable to connect to any servers', {'localhost': TypeError('ref() does not take keyword arguments',)})` try to upgrade cassandra to 2.1.16 (if released), or downgrade python to <2.7.12. For example, using homebrew: `brew switch python 2.7.9`.

Check the profile you want to build against in IntelliJ's Maven Projects view
![IntelliJ Profile Selection](/docs/intellij-profile-selection.png)

## Auto-discovery of (cloud) services and properties (Consul)
COMaaS has support for auto-discovery of some cloud services (currently Cassandra, when registered in Consul) as well as configuration properties (Consul KV).

### Registration with Consul
The application will register itself with the Consul server as `comaas:core:<tenant>:<port>` and appear under 'comaas-core' in Consul.

### Consul services
The application will automatically scan for other Consul-registered services. Currently this is only Cassandra, where the service name is hard-matched to 'cassandra'. Cassandra must be registered with Consul (e.g. through a `cassandra.json` file on each Cassandra/Consul node) for this to work.

### Consul configuration (KV)
Consul will also be added as a PropertySource for configuration resolution (similar to replyts.properties). Configuration in Consul overrides what is present in the replyts.properties file.

In order to add keys to consul, add them with the prefix `comaas/<tenant>` or `comaas/comaas:core:<tenant>:<port>`. E.g. `comaas/kjca/persistence.strategy` with value `cassandra`. This prefix approach allows one Consul instance to contain properties for multiple tenants (or the same tenant running on multiple ports).

## MP system overview
![Messaging system overview at Marktplaats](/docs/20151221-messaging-system-overview.jpg)

### Certificate issues

When encountering `sun.security.validator.ValidatorException: PKIX path building failed` while downloading artifacts from maven, one of the certificates might be expired.

To create an updated keystore file, download and unzip `https://ebayinc.sharepoint.com/teams/SelfService/Directory%20Services/SiteAssets/SitePages/Active%20Directory%20Certificates%20Services%20Help%20Site/root-certs-pem.zip`.

Generate a new `comaas.jks` file:
```
keytool -genkey -alias comaas -keyalg RSA -keystore comaas.jks -keysize 2048 \
  -dname "CN=com, OU=COMaaS, O=eBay Classifieds, L=Amsterdam, S=Noord-Holland, C=NL" \
  -storepass 'comaas' -keypass 'comaas'

for f in root-certs-pem/*.pem; do
    keytool -importcert -keystore comaas.jks -storepass 'comaas' -file ${f} -alias ${f} -noprompt
done

# Install AMS1 & DUS1 CA
openssl s_client -connect keystone.ams1.cloud.ecg.so:443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM |
  keytool -importcert -keystore comaas.jks -storepass 'comaas' -alias ams1 -noprompt

openssl s_client -connect keystone.dus1.cloud.ecg.so:443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM |
  keytool -importcert -keystore comaas.jks -storepass 'comaas' -alias dus1 -noprompt

# Install Gumtree AU nexus CA
openssl s_client -showcerts -connect nexus.au.ecg.so:443 </dev/null 2>/dev/null | openssl x509 -outform PEM | \
  keytool -importcert -keystore comaas.jks -storepass 'comaas' -alias nexusau -noprompt
```

Finally, upload the new `comaas.jks` to Swift. Update the URL in `bin/build.sh` if needed.
