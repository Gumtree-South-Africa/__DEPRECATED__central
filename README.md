# ReplyTS2 for Comaas

## Links

* [Reply T&S at Marktplaats - integration](https://ecgwiki.corp.ebay.com/confluence/pages/viewpage.action?pageId=69271634) Describes headers expected from Aurora.

## Dev Setup

Before you start, setup vagrant as described here: https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki/ReplyTS-Environment-Setup

Clone this repository, and finally clone all the submodules:
```
git submodule update --init
pushd replyts2-core && git checkout cassandra && git pull origin cassandra && popd
pushd replyts2-event-publisher && git checkout master && git pull origin master && popd
pushd replyts2-message-center && git checkout master && git pull origin master && popd
```

TODO: change last three lines for the following two when rts core is on master branch again.
```
#git submodule foreach git checkout master
#git submodule foreach git pull origin master
```

To run RTS2 create the following run configuration:

* Type: application
* Main class: `nl.marktplaats.replyts2.MarktplaatsReplyTS2Main`
* VM arguments:
  ```
  -DconfDir=replyts2-core/core-runtime/src/main/resources/vagrant-conf-cassandra
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
