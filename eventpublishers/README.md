# ReplyTS2 Event Publisher 
copied from https://github.corp.ebay.com/ReplyTS/replyts2-event-publisher
(original hash: 9453269e9913c250ce5af7aef0e02daf864a824b)

Note: Event publisher interface has been migrated to core-runtime/app/eventpublisher

This repository hosts two plugin that, if enabled, pushes "message received" events to RabbitMQ or Kafka.

Both plugins use the RTS2 plugin mechanism to be loaded by Spring (with files `resources/plugin-inf/*.xml`) and then be wired in the rest of the application.

# Table of contents

* [Kafka](#kafka)
* [RabbitMQ](#rabbitmq)
* [Example events](#example)
* [Getting and building releases](#releases)

# Kafka

Include the plugin in your project by depending on:

```
    <dependency>
        <groupId>com.ecg.replyts</groupId>
        <artifactId>mp-kafka-publisher</artifactId>
        <version>1.0</version>
    </dependency>
```

Then to enable the Kafka producer include the following in your `replyts.properties`:

```
replyts.event.publisher.kafka.enabled=true
replyts.kafka.broker.list=host1:9092,host2:9092
replyts.kafka.topic=conversations
... more properties ...
```

For more information see [KafkaEventPublisherConfig](/replyts2-kafka-publisher/src/main/java/com/ecg/replyts2/eventpublisher/kafka/KafkaEventPublisherConfig.java).

# RabbitMQ

Include the plugin by depending on:

```
    <dependency>
        <groupId>com.ecg.replyts</groupId>
        <artifactId>mp-rabbitmq-publisher</artifactId>
        <version>1.0</version>
    </dependency>
```

Then to enable the Rabbit MQ producer include the following in your `replyts.properties`:

```
replyts.event.publisher.rabbitmq.enabled=true
... more properties ...
```

For more information see [RabbitmqEventPublisherConfig](/replyts2-rabbitmq-publisher/src/main/java/com/ecg/replyts2/eventpublisher/rabbitmq/RabbitmqEventPublisherConfig.java).


# Example

Here are 4 events generated for an email that starts a new conversation:

```
{"event":{"type":"ConversationCreatedEvent","conversationId":"2:-nsggz5:ih21a4ge","adId":"m963345252","buyerId":"buyer@example.nl","sellerId":"seller@example.com","buyerSecret":"1z5uiul070jd0","sellerSecret":"17bk58off44i2","createdAt":"2015-11-16T14:15:39.182Z","state":"ACTIVE","customValues":{"aurora-abtests":"AUR234.A|MIG8525.A|mario2802.A|MIG8458.A|MIG10048.A|qualarooOnL1.A|npsfeedback.A|AUR286.A|AUR455.A|MIG7929.A|qualarooOnFavouriteSellers.A|6658.A|MTX1387.A|AUR32.A|MIG9961.A|MIG9550.C|qualarooOnVIP.A|qualarooOnLRP.A|AUR21.A|qualarooOnSYI.A|qualarooOnRYI.A|MIG9881.A|MIG9223.A|6121.A|qualarooOnMyMpFavourites.A|qualarooOnHUB.A|MIG10007.A|AUR28.A|MIG9403.A|6994.A|MIG8450.A|sitespeed.A|AUR413.A|9412.A|AUR125.A|MIG8492.A|MIG9399.A|optimizelyOnLRP.B|tealeafSample.A|AUR4.A","aurora-featureswitches":"MIG9551:true|MIG6787:true|MIG9257:true|MIG9485-c2c-show-onboard-dialog:false|carparts.new.query:true|MIG8478:true|MIG7393:true|MIG10032:true|MIG5609:true|MIG8801:true|MIG8849:true|campers.new.query:true|MIG9485-c2c-show-profile-box:false|MIG6783:true|MIG9491:true|MIG8915:true|MIG7577_CLEANUP:true|MIG9220:true|AUR4VIP:true|MIG7596:true|FEEDME_EVENTS:false|MIG9421:true|MIG7161:true|MAD4187:false|MAD3630:true|MIG6786:true|MIG6556:true|MIG6531:true|imagesNoFlash:true|MIG8290:true|MAD3633:true|MIG9455:true|MIG9796:true|MIG9135:true|crossOrigin:true|MIG9018:true|MIG8803:true|AUR52:true|MIG6135:true|MIG9485-force:false|MIG7841:true|MIG6785:true|MIG7577:true|MIG9264:true|MIG8913:true|MIG7367:true|MIG9625:true|MIG6269:true|MIG9091:true|MIG9596:true|KIUW:true|AUR241:true|MIG10093:true|MAD3629:true|MIG9442:true|MIG7842:true|MIG9987:true|MIG9392:true|MIG7397:true|MIG9485:true|MIG8919:true|MIG9399:true|MIG9607:true|accessLog:true|MIG7087:true|MIG7839:true|MIG9506:tr ue","device":"androidPhone","device-info":"mp::android::76x127","flowtype":"ASQ","from":"buyername","from-ip":"163.158.118.14","from-userid":"9958817","l1-categoryid":"621","l2-categoryid":"622","origin":"CAPI","to":"K","to-userid":"3521734","useragent":"okhttp/2.3.0"},"eventId":"created-2:-nsggz5:ih21a4ge-1447683339182","conversationModifiedAt":"2015-11-16T14:15:39.182Z","formatVer":1},"conversation":{"sellerAnonymousEmail":"s.17bk58off44i2@replyts.dev.kjdev.ca","buyerAnonymousEmail":"b.1z5uiul070jd0@replyts.dev.kjdev.ca","state":"ACTIVE","conversationId":"2:-nsggz5:ih21a4ge","adId":"m963345252","createdAt":"2015-11-16T14:15:39.182Z","customValues":{"aurora-abtests":"AUR234.A|MIG8525.A|mario2802.A|MIG8458.A|MIG10048.A|qualarooOnL1.A|npsfeedback.A|AUR286.A|AUR455.A|MIG7929.A|qualarooOnFavouriteSellers.A|6658.A|MTX1387.A|AUR32.A|MIG9961.A|MIG9550.C|qualarooOnVIP.A|qualarooOnLRP.A|AUR21.A|qualarooOnSYI.A|qualarooOnRYI.A|MIG9881.A|MIG9223.A|6121.A|qualarooOnMyMpFavourites.A|qualarooOnHUB.A|MIG10007.A|AUR28.A|MIG9403.A|6994.A|MIG8450.A|sitespeed.A|AUR413.A|9412.A|AUR125.A|MIG8492.A|MIG9399.A|optimizelyOnLRP.B|tealeafSample.A|AUR4.A","l1-categoryid":"621","device-info":"mp::android::76x127","origin":"CAPI","useragent":"okhttp/2.3.0","from-userid":"9958817","flowtype":"ASQ","to-userid":"3521734","l2-categoryid":"622","aurora-featureswitches":"MIG9551:true|MIG6787:true|MIG9257:true|MIG9485-c2c-show-onboard-dialog:false|carparts.new.query:true|MIG8478:true|MIG7393:true|MIG10032:true|MIG5609:true|MIG8801:true|MIG8849:true|campers.new.query:true|MIG9485-c2c-show-profile-box:false|MIG6783:true|MIG9491:true|MIG8915:true|MIG7577_CLEANUP:true|MIG9220:true|AUR4VIP:true|MIG7596:true|FEEDME_EVENTS:false|MIG9421:true|MIG7161:true|MAD4187:false|MAD3630:true|MIG6786:true|MIG6556:true|MIG6531:true|imagesNoFlash:true|MIG8290:true|MAD3633:true|MIG9455:true|MIG9796:true|MIG9135:true|crossOrigin:true|MIG9018:true|MIG8803:true|AUR52:true|MIG6135:true|MIG9485-force:false|MIG7841:true|MIG6785:true|MIG7577:true|MIG9264:true|MIG8913:true|MIG7367:true|MIG9625:true|MIG6269:true|MIG9091:true|MIG9596:true|KIUW:true|AUR241:true|MIG10093:true|MAD3629:true|MIG9442:true|MIG7842:true|MIG9987:true|MIG9392:true|MIG7397:true|MIG9485:true|MIG8919:true|MIG9399:true|MIG9607:true|accessLog:true|MIG7087:true|MIG7839:true|MIG9506:tr ue","from-ip":"163.158.118.14","from":"buyername","to":"K","device":"androidPhone"},"sellerId":"seller@example.com","buyerId":"buyer@example.nl","closed":false,"conversationModifiedAt":"2015-11-16T14:15:39.550Z","closedByBuyer":false,"closedBySeller":false,"messageCount":1}}
{"event":{"type":"MessageAddedEvent","messageId":"1:-nsggz5:ih21a4ge","messageDirection":"BUYER_TO_SELLER","receivedAt":"2015-11-16T14:15:39.206Z","state":"UNDECIDED","senderMessageIdHeader":"<1053831338.4678687.1447282801264.JavaMail.www-data@mp-be001>","inResponseToMessageId":null,"filterResultState":"OK","humanResultState":"GOOD","headers":{"Return-Path":"<automatisch@marktplaats.nl>","X-Original-To":"seller@example.com","Delivered-To":"seller@example.com","Received":"from mp-be001.aurora.fra01.marktplaats.nl (unknown [10.34.230.94])\tby mp-rtscore001.replyts.fra01.marktplaats.nl (Postfix) with ESMTP id 427EE20075F86\tfor <seller@gmail.com>; Thu, 12 Nov 2015 00:00:01 +0100 (CET)","Date":"Thu, 12 Nov 2015 00:00:01 +0100 (CET)","From":"buyername via Marktplaats <automatisch@marktplaats.nl>","Reply-To":"buyername <buyer@example.nl>","To":"seller@example.com","Message-ID":"<1053831338.4678687.1447282801264.JavaMail.www-data@mp-be001>","Subject":"Ik heb interesse in 'Zwarte tankini Livera 42' - buyername","X-Mailer":"Aurora Mailer","X-ADID":"m963345252","X-Cust-Aurora-Abtests":"AUR234.A|MIG8525.A|mario2802.A|MIG8458.A|MIG10048.A|qualarooOnL1.A|npsfeedback.A|AUR286.A|AUR455.A|MIG7929.A|qualarooOnFavouriteSellers.A|6658.A|MTX1387.A|AUR32.A|MIG9961.A|MIG9550.C|qualarooOnVIP.A|qualarooOnLRP.A|AUR21.A|qualarooOnSYI.A|qualarooOnRYI.A|MIG9881.A|MIG9223.A|6121.A|qualarooOnMyMpFavourites.A|qualarooOnHUB.A|MIG10007.A|AUR28.A|MIG9403.A|6994.A|MIG8450.A|sitespeed.A|AUR413.A|9412.A|AUR125.A|MIG8492.A|MIG9399.A|optimizelyOnLRP.B|tealeafSample.A|AUR4.A","X-Cust-Aurora-Featureswitches":"MIG9551:true|MIG6787:true|MIG9257:true|MIG9485-c2c-show-onboard-dialog:false|carparts.new.query:true|MIG8478:true|MIG7393:true|MIG10032:true|MIG5609:true|MIG8801:true|MIG8849:true|campers.new.query:true|MIG9485-c2c-show-profile-box:false|MIG6783:true|MIG9491:true|MIG8915:true|MIG7577_CLEANUP:true|MIG9220:true|AUR4VIP:true|MIG7596:true|FEEDME_EVENTS:false|MIG9421:true|MIG7161:true|MAD4187:false|MAD3630:true|MIG6786:true|MIG6556:true|MIG6531:true|imagesNoFlash:true|MIG8290:true|MAD3633:true|MIG9455:true|MIG9796:true|MIG9135:true|crossOrigin:true|MIG9018:true|MIG8803:true|AUR52:true|MIG6135:true|MIG9485-force:false|MIG7841:true|MIG6785:true|MIG7577:true|MIG9264:true|MIG8913:true|MIG7367:true|MIG9625:true|MIG6269:true|MIG9091:true|MIG9596:true|KIUW:true|AUR241:true|MIG10093:true|MAD3629:true|MIG9442:true|MIG7842:true|MIG9987:true|MIG9392:true|MIG7397:true|MIG9485:true|MIG8919:true|MIG9399:true|MIG9607:true|accessLog:true|MIG7087:true|MIG7839:true|MIG9506:tr ue","X-Cust-Device":"androidPhone","X-Cust-Device-Info":"mp::android::76x127","X-Cust-Flowtype":"ASQ","X-Cust-From":"buyername","X-Cust-From-Ip":"163.158.118.14","X-Cust-From-Userid":"9958817","X-Cust-L1-Categoryid":"621","X-Cust-L2-Categoryid":"622","X-Cust-Origin":"CAPI","X-Cust-To":"K","X-Cust-To-Userid":"3521734","X-Cust-Useragent":"okhttp/2.3.0"},"plainTextBody":"Message body.\n\n","attachments":[],"eventId":"MessageAddedEvent-1:-nsggz5:ih21a4ge-1447683339206","conversationModifiedAt":"2015-11-16T14:15:39.206Z","formatVer":1},"conversation":{"sellerAnonymousEmail":"s.17bk58off44i2@replyts.dev.kjdev.ca","buyerAnonymousEmail":"b.1z5uiul070jd0@replyts.dev.kjdev.ca","state":"ACTIVE","conversationId":"2:-nsggz5:ih21a4ge","adId":"m963345252","createdAt":"2015-11-16T14:15:39.182Z","customValues":{"aurora-abtests":"AUR234.A|MIG8525.A|mario2802.A|MIG8458.A|MIG10048.A|qualarooOnL1.A|npsfeedback.A|AUR286.A|AUR455.A|MIG7929.A|qualarooOnFavouriteSellers.A|6658.A|MTX1387.A|AUR32.A|MIG9961.A|MIG9550.C|qualarooOnVIP.A|qualarooOnLRP.A|AUR21.A|qualarooOnSYI.A|qualarooOnRYI.A|MIG9881.A|MIG9223.A|6121.A|qualarooOnMyMpFavourites.A|qualarooOnHUB.A|MIG10007.A|AUR28.A|MIG9403.A|6994.A|MIG8450.A|sitespeed.A|AUR413.A|9412.A|AUR125.A|MIG8492.A|MIG9399.A|optimizelyOnLRP.B|tealeafSample.A|AUR4.A","l1-categoryid":"621","device-info":"mp::android::76x127","origin":"CAPI","useragent":"okhttp/2.3.0","from-userid":"9958817","flowtype":"ASQ","to-userid":"3521734","l2-categoryid":"622","aurora-featureswitches":"MIG9551:true|MIG6787:true|MIG9257:true|MIG9485-c2c-show-onboard-dialog:false|carparts.new.query:true|MIG8478:true|MIG7393:true|MIG10032:true|MIG5609:true|MIG8801:true|MIG8849:true|campers.new.query:true|MIG9485-c2c-show-profile-box:false|MIG6783:true|MIG9491:true|MIG8915:true|MIG7577_CLEANUP:true|MIG9220:true|AUR4VIP:true|MIG7596:true|FEEDME_EVENTS:false|MIG9421:true|MIG7161:true|MAD4187:false|MAD3630:true|MIG6786:true|MIG6556:true|MIG6531:true|imagesNoFlash:true|MIG8290:true|MAD3633:true|MIG9455:true|MIG9796:true|MIG9135:true|crossOrigin:true|MIG9018:true|MIG8803:true|AUR52:true|MIG6135:true|MIG9485-force:false|MIG7841:true|MIG6785:true|MIG7577:true|MIG9264:true|MIG8913:true|MIG7367:true|MIG9625:true|MIG6269:true|MIG9091:true|MIG9596:true|KIUW:true|AUR241:true|MIG10093:true|MAD3629:true|MIG9442:true|MIG7842:true|MIG9987:true|MIG9392:true|MIG7397:true|MIG9485:true|MIG8919:true|MIG9399:true|MIG9607:true|accessLog:true|MIG7087:true|MIG7839:true|MIG9506:tr ue","from-ip":"163.158.118.14","from":"buyername","to":"K","device":"androidPhone"},"sellerId":"seller@example.com","buyerId":"buyer@example.nl","closed":false,"conversationModifiedAt":"2015-11-16T14:15:39.550Z","closedByBuyer":false,"closedBySeller":false,"messageCount":1}}
{"event":{"type":"MessageFilteredEvent","messageId":"1:-nsggz5:ih21a4ge","decidedAt":"2015-11-16T14:15:39.212Z","filterResultState":"OK","processingFeedback":[],"eventId":"MessageFilteredEvent-1:-nsggz5:ih21a4ge-1447683339212","conversationModifiedAt":"2015-11-16T14:15:39.212Z","formatVer":1},"conversation":{"sellerAnonymousEmail":"s.17bk58off44i2@replyts.dev.kjdev.ca","buyerAnonymousEmail":"b.1z5uiul070jd0@replyts.dev.kjdev.ca","state":"ACTIVE","conversationId":"2:-nsggz5:ih21a4ge","adId":"m963345252","createdAt":"2015-11-16T14:15:39.182Z","customValues":{"aurora-abtests":"AUR234.A|MIG8525.A|mario2802.A|MIG8458.A|MIG10048.A|qualarooOnL1.A|npsfeedback.A|AUR286.A|AUR455.A|MIG7929.A|qualarooOnFavouriteSellers.A|6658.A|MTX1387.A|AUR32.A|MIG9961.A|MIG9550.C|qualarooOnVIP.A|qualarooOnLRP.A|AUR21.A|qualarooOnSYI.A|qualarooOnRYI.A|MIG9881.A|MIG9223.A|6121.A|qualarooOnMyMpFavourites.A|qualarooOnHUB.A|MIG10007.A|AUR28.A|MIG9403.A|6994.A|MIG8450.A|sitespeed.A|AUR413.A|9412.A|AUR125.A|MIG8492.A|MIG9399.A|optimizelyOnLRP.B|tealeafSample.A|AUR4.A","l1-categoryid":"621","device-info":"mp::android::76x127","origin":"CAPI","useragent":"okhttp/2.3.0","from-userid":"9958817","flowtype":"ASQ","to-userid":"3521734","l2-categoryid":"622","aurora-featureswitches":"MIG9551:true|MIG6787:true|MIG9257:true|MIG9485-c2c-show-onboard-dialog:false|carparts.new.query:true|MIG8478:true|MIG7393:true|MIG10032:true|MIG5609:true|MIG8801:true|MIG8849:true|campers.new.query:true|MIG9485-c2c-show-profile-box:false|MIG6783:true|MIG9491:true|MIG8915:true|MIG7577_CLEANUP:true|MIG9220:true|AUR4VIP:true|MIG7596:true|FEEDME_EVENTS:false|MIG9421:true|MIG7161:true|MAD4187:false|MAD3630:true|MIG6786:true|MIG6556:true|MIG6531:true|imagesNoFlash:true|MIG8290:true|MAD3633:true|MIG9455:true|MIG9796:true|MIG9135:true|crossOrigin:true|MIG9018:true|MIG8803:true|AUR52:true|MIG6135:true|MIG9485-force:false|MIG7841:true|MIG6785:true|MIG7577:true|MIG9264:true|MIG8913:true|MIG7367:true|MIG9625:true|MIG6269:true|MIG9091:true|MIG9596:true|KIUW:true|AUR241:true|MIG10093:true|MAD3629:true|MIG9442:true|MIG7842:true|MIG9987:true|MIG9392:true|MIG7397:true|MIG9485:true|MIG8919:true|MIG9399:true|MIG9607:true|accessLog:true|MIG7087:true|MIG7839:true|MIG9506:tr ue","from-ip":"163.158.118.14","from":"buyername","to":"K","device":"androidPhone"},"sellerId":"seller@example.com","buyerId":"buyer@example.nl","closed":false,"conversationModifiedAt":"2015-11-16T14:15:39.550Z","closedByBuyer":false,"closedBySeller":false,"messageCount":1}}
{"event":{"type":"MessageTerminatedEvent","messageId":"1:-nsggz5:ih21a4ge","conversationModifiedAt":"2015-11-16T14:15:39.550Z","reason":"sent","issuer":"com.ecg.replyts.core.api.model.conversation.Message","terminationState":"SENT","eventId":"MessageTerminatedEvent-1:-nsggz5:ih21a4ge-1447683339550","formatVer":1},"conversation":{"sellerAnonymousEmail":"s.17bk58off44i2@replyts.dev.kjdev.ca","buyerAnonymousEmail":"b.1z5uiul070jd0@replyts.dev.kjdev.ca","state":"ACTIVE","conversationId":"2:-nsggz5:ih21a4ge","adId":"m963345252","createdAt":"2015-11-16T14:15:39.182Z","customValues":{"aurora-abtests":"AUR234.A|MIG8525.A|mario2802.A|MIG8458.A|MIG10048.A|qualarooOnL1.A|npsfeedback.A|AUR286.A|AUR455.A|MIG7929.A|qualarooOnFavouriteSellers.A|6658.A|MTX1387.A|AUR32.A|MIG9961.A|MIG9550.C|qualarooOnVIP.A|qualarooOnLRP.A|AUR21.A|qualarooOnSYI.A|qualarooOnRYI.A|MIG9881.A|MIG9223.A|6121.A|qualarooOnMyMpFavourites.A|qualarooOnHUB.A|MIG10007.A|AUR28.A|MIG9403.A|6994.A|MIG8450.A|sitespeed.A|AUR413.A|9412.A|AUR125.A|MIG8492.A|MIG9399.A|optimizelyOnLRP.B|tealeafSample.A|AUR4.A","l1-categoryid":"621","device-info":"mp::android::76x127","origin":"CAPI","useragent":"okhttp/2.3.0","from-userid":"9958817","flowtype":"ASQ","to-userid":"3521734","l2-categoryid":"622","aurora-featureswitches":"MIG9551:true|MIG6787:true|MIG9257:true|MIG9485-c2c-show-onboard-dialog:false|carparts.new.query:true|MIG8478:true|MIG7393:true|MIG10032:true|MIG5609:true|MIG8801:true|MIG8849:true|campers.new.query:true|MIG9485-c2c-show-profile-box:false|MIG6783:true|MIG9491:true|MIG8915:true|MIG7577_CLEANUP:true|MIG9220:true|AUR4VIP:true|MIG7596:true|FEEDME_EVENTS:false|MIG9421:true|MIG7161:true|MAD4187:false|MAD3630:true|MIG6786:true|MIG6556:true|MIG6531:true|imagesNoFlash:true|MIG8290:true|MAD3633:true|MIG9455:true|MIG9796:true|MIG9135:true|crossOrigin:true|MIG9018:true|MIG8803:true|AUR52:true|MIG6135:true|MIG9485-force:false|MIG7841:true|MIG6785:true|MIG7577:true|MIG9264:true|MIG8913:true|MIG7367:true|MIG9625:true|MIG6269:true|MIG9091:true|MIG9596:true|KIUW:true|AUR241:true|MIG10093:true|MAD3629:true|MIG9442:true|MIG7842:true|MIG9987:true|MIG9392:true|MIG7397:true|MIG9485:true|MIG8919:true|MIG9399:true|MIG9607:true|accessLog:true|MIG7087:true|MIG7839:true|MIG9506:tr ue","from-ip":"163.158.118.14","from":"buyername","to":"K","device":"androidPhone"},"sellerId":"seller@example.com","buyerId":"buyer@example.nl","closed":false,"conversationModifiedAt":"2015-11-16T14:15:39.550Z","closedByBuyer":false,"closedBySeller":false,"messageCount":1}}
```

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
