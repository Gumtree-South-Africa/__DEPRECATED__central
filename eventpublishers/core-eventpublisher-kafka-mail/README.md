# core-eventpublisher-kafka-mail

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-kafka-mail-publisher
(original git hash: 825a73f88b0abcceb988abcfe2de905a4abcceab)

# Description

Allows to store incoming mail and outgoing mail diff in kafka.
Plugin uses spring to provide implementation for the `com.ecg.replyts.core.runtime.listener.MailPublisher` which will be called after mail processing is finished.

# Usage

Include the plugin in your project by depending on:

```
    <dependency>
        <groupId>com.ecg.replyts</groupId>
        <artifactId>replyts2-kafka-mail-publisher</artifactId>
        <version>1.0.0</version>
    </dependency>
```

Then to enable the Kafka producer include the following in your `replyts.properties`:

```
mailpublisher.kafka.enabled=true
mailpublisher.kafka.broker.list=localhost:9092
mailpublisher.kafka.topic=rtscoremail
```

# Storage format

Storage format: key is message id with incoming mail and difference between incoming and outgoing mail.
See `com.ecg.replyts2.persistence.kafka.MailCompressor` for details.


# Releases

Releases are just tags in git. It is your own responsibility to deploy binaries to your own maven repository.

The simplest way to do so would be to check out this repository and then:

```
REPOSITORY_ID=...see below...
REPOSITORY_URL=...see below...
git tag -l
git checkout $RELEASE_TAG
mvn deploy -P distribution -DaltDeploymentRepository=$REPOSITORY_ID::default::$REPOSITORY_URL
```

## Building a release

Build the release:

```
REPOSITORY_ID=...see below...
REPOSITORY_URL=...see below...
VERSION=...version to release, e.g. `1.0.0`...
NEXT_VERSION=...next version to release, e.g. `1.0.1`...

mvn versions:set versions:commit -DnewVersion=$VERSION
mvn clean package deploy -P distribution -DaltDeploymentRepository=$REPOSITORY_ID::default::$REPOSITORY_URL
git commit -am "upgrade to version $VERSION"
git tag REL-$VERSION
git push origin tag REL-$VERSION
git push

mvn versions:set versions:commit -DnewVersion=${NEXT_VERSION}-SNAPSHOT
git commit -am "prepare for next version"
git push
```

## Maven repositories

For Marktplaats:
```
REPOSITORY_ID=mpnexus
REPOSITORY_URL=http://mpnexus.corp.ebay.com/content/repositories/libs-releases-local
```
