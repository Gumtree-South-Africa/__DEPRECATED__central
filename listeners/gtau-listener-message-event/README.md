# gtau-listener-message-event

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-message-event-listener

# Description

# replyts2-message-event-listener
This is a post process listener when a message is initiated. The listener creates an event when a message is initiated and sent it to RMQ to be consumed by gumbot-watchlist-notification. 

Every event contains the following information about each message:

* messageId
* conversationId
* adId
* buyerMail
* sellerMail
* messageDirection
* conversationState
* messageState
* number of message in this conversation (starting with 0 for the contact poster mail, 1 will be the first reply,...)
* conversation creation date
* message received date
* conversation last modified date
* category id from custom headers
* ip from custom headers
* replyChannel from custom headers
* userAgent from custom headers

Please note that this plugin only consumed by gumbot-watchlist-notification microservice.

