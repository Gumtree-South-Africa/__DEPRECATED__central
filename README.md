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
replace `/Users/${USER}/dev/comaas/ecg-comaas-central` with your path to Comaas's `central` repository and `tenant long name` with the tenant's long name.

Before IntelliJ understands the project, you will need to generate the protobuf sources by executing `bin/build.sh` without arguments.

Now add a Run Configuration in IntelliJ with the following properties:

* Type: Application
* Name: COMaaS for: [name of tenant, mp|mde|ebayk]

* Main class: com.ecg.replyts.core.Application
* VM Options: -Dtenant=<tenant long name> -Dservice.discovery.port=8599 -Dlogging.service.structured.logging=false
* Working directory: /Users/<user name>/dev/ecg-comaas-central
* Use classpath of module: distribution

#### Option 1. Build script

  ```
  ./bin/build.sh -T gtuk -P docker -E -D
  ```
  see bin/README.md for more details

### Testing that your setup works

In the docker repo, run `make send`. This puts a message in the `mp_messages`  kafka topic (assuming tenant is mp). 

After being succesfully processed in the Trust & Safety pipeline, the approved message should show up in the `conversation_events`  topic , which you can check at [http://localhost:8073/#/cluster/default/topic/n/conversation_events/data](http://localhost:8073/#/cluster/default/topic/n/conversation_events/data) . 

## Auto-discovery of (cloud) services and properties (Consul)
COMaaS has support for auto-discovery of some cloud services (currently Cassandra, when registered in Consul) as well as configuration properties (Consul KV).

### Consul services
The application will automatically scan for other Consul-registered services. Currently this is Cassandra, Kafka, and ElasticSearch.

### Consul configuration (KV)
In order to add keys to consul, add them with the prefix `comaas/comaas:core:<tenant long name>`. E.g. `comaas/comaas:core:ebayk/persistence.strategy` with value `cassandra`. This prefix approach allows one Consul instance to contain properties for multiple tenants (or the same tenant running on multiple ports).

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
