Getting comaas unto the cloud, using mp-replyts2 as a base. Let's see where we get...

Unsupported major.minor version 52.0 --> set JAVA_HOME correctly

# ReplyTS2 for Comaas

## Contents

* Links
* Dev Setup
* System overview

## Links

* [Reply T&S at Marktplaats - integration](https://ecgwiki.corp.ebay.com/confluence/pages/viewpage.action?pageId=69271634) Describes headers expected from Aurora.
* [Cassandra debuggin with DevCenter](docs/cassandra-debugging.md)
* [Orphaned email](docs/orphaned-mail.md)

RTS2 and plugins repositories

* [RTS2 core](https://github.corp.ebay.com/ReplyTS/replyts2-core/)
* [RTS2 message-box](https://github.corp.ebay.com/ReplyTS/replyts2-message-box)
* [replyts2-threshold-resultinspector-plugin](https://github.corp.ebay.com/ReplyTS/replyts2-threshold-resultinspector-plugin)
* [replyts2-event-publisher](https://github.corp.ebay.com/ReplyTS/replyts2-event-publisher)
* [replyts2-graphite-plugin](https://github.corp.ebay.com/ReplyTS/replyts2-graphite-plugin)

CsBizapp services repositories

* [Mail-guard](https://github.corp.ebay.com/ecg-marktplaats/sunrise-mail-guard)
* [sunrise-replyts-events](https://github.corp.ebay.com/ecg-marktplaats/sunrise-replyts-events)

Aurora services repositories

* [aurora-messaging-frontend](https://github.corp.ebay.com/ecg-marktplaats/aurora-messaging-frontend)
* [aurora-messaging-api](https://github.corp.ebay.com/ecg-marktplaats/aurora-messaging-api)

Other repositories

* [Flume conversation event converter](https://github.corp.ebay.com/ReplyTS/conversation-event-converter)
* [Marktplaats' RTS 1 plugins](https://github.corp.ebay.com/ecg-marktplaats/csba-replyts-plugins)

## Dev Setup

Before you start, setup vagrant as described here: https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki/ReplyTS-Environment-Setup

Clone this repository, and finally clone all the submodules:
```
git submodule update --init
pushd replyts2-threshold-resultinspector-plugin && git checkout marktplaats && git pull origin marktplaats && popd
```

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

## System overview

![Messaging system overview at Marktplaats](/docs/20151221-messaging-system-overview.jpg)
