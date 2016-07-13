# mp-messagecenter

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-message-box
(original git hash: ca4ebc3688d6797545cb3833c5976ffd73dfb47a)

# Description

Addon for ReplyTS that allows simple messaging functionality for apps and website.

Supports both Riak and Cassandra for persistence.

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
mvn clean package deploy -DaltDeploymentRepository=$REPOSITORY_ID::default::$REPOSITORY_URL
git commit -am "upgrade to version $VERSION"
git tag REL-$VERSION
git push origin tag REL-$VERSION
git push

mvn versions:set versions:commit -DnewVersion=${NEXT_VERSION}-SNAPSHOT
git commit -am "prepare for next version"
git push
```

## Maven repositories

For Germany:
```
REPOSITORY_ID=kijiji-belen-releases
REPOSITORY_URL=http://ci.corp.ebay-kleinanzeigen.de/nexus/content/repositories/hosted-kijiji-belen-releases
```

For Marktplaats:
```
REPOSITORY_ID=mpnexus
REPOSITORY_URL=http://mpnexus.corp.ebay.com/content/repositories/libs-releases-local
```
