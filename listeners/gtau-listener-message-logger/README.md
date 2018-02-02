# gtau-listener-message-logger

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-message-json-logging
(original git hash: f19d032c78de2a3ad92da656fb740c1955ab6192)

# Description

Logs message contents (with information about conversation) to a database. Each insertion contains:

* messageId
* conversationId
* adId
* buyerMail
* sellerMail
* messageDirection
* conversationState
* messageState
* Number of message in this conversation (starting with 0 for the contact poster mail; 1 will be the first reply)
* Conversation creation date
* Message received date
* Conversation last modified date
* Category ID from custom headers
* IP from custom headers

Please note that this plugin only appends to the log. Existing log entries are never updated. Any changes to a message - ie, state changes (from HELD to SENT) - will have its own log entry.

The table structure:

```SQL
CREATE TABLE replyts.rts2_event_log (
  id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
  messageId varchar (20) NOT NULL,
  conversationId VARCHAR(20) NOT NULL,
  messageDirection varchar (20) NOT NULL,
  conversationState varchar (20) NOT NULL,
  messageState VARCHAR(20) NOT NULL,
  adId VARCHAR(20) NOT NULL,
  sellerMail VARCHAR(100) NOT NULL,
  buyerMail VARCHAR(100) NOT NULL,
  numOfMessageInConversation VARCHAR(20) NOT NULL,
  logTimestamp VARCHAR(25) NOT NULL,
  conversationCreatedAt VARCHAR(25) NOT NULL,
  messageReceivedAt VARCHAR(25) NOT NULL,
  conversationLastModifiedDate VARCHAR(25) NOT NULL,
  custcategoryid VARCHAR(20) NOT NULL,
  custip VARCHAR(20) NOT NULL);
```
