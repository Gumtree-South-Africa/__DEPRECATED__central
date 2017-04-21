Originally taken from https://github.corp.ebay.com/annunci/replyts2-ebayk-message-center
(original git hash: 97ed2009c964742d6c32e73cb3d8b2b9440cd52b)

# replyts2-ebayk-message-center

Addon for ReplyTS that allows simple message center functionality for apps and our website. It utilizes our push mobile server
https://github.scm.corp.ebay.com/eBay-Kleinanzeigen/push-mobile for push notifications to android and iphone apps.



# Deployment
mvn release:prepare release:perform

# Message Box API
===

## Endpoints

### GetPostBox
* `GET /postboxes/{email:.+}`
* `PUT /postboxes/{email:.+}` // mark as read (reset replies -> counter)

  - `newCounterMode` (boolean, defaultValue = `false`)
  - `size` (integer, defaultValue = `50`)
  - `page` (integer, defaultValue = `0`)
  - `robotEnabled` (boolean, defaultValue = `true`)

* `DELETE /postboxes/{email:.+}`

  - `ids` (string[], defaultValue = ``, required)
  - `newCounterMode` (boolea, defaultValue = `true`)
  - `page` (integer, defaultValue = `0`)
  - `size` (integer, defaultValue = `50`)


### GetPostBoxConversation
* `GET /postboxes/{email}/conversations/{conversationId}`
* `PUT /postboxes/{email}/conversations/{conversationId}` // mark as read

  - `newCounterMode` (boolean, defaultValue = `true`)
  - `robotEnabled` (boolean, defaultValue = `true`)

### AddMessageToPostBoxConversation (*)
* `POST /postboxes/{email}/conversations/{conversationId}`

### CreatePostBoxConversation (*)
* `POST /postboxes/{email}/conversations`
 
### GetAdConversationRecipients (?)
* `GET /postboxes/{urlEncodedSellerEmail}/ad/{adId}/buyeremails`

  - `state` (default = `ACTIVE`)

### DeletePostBoxConversation
* `DELETE /postboxes/{email}/conversations`

## Payloads

### GetPostBox

`GET http://host:8080/ebayk-msgcenter/postboxes/xuyin@ebay.com`

```
{
  "status": {
    "state": "OK",
    "details": null,
    "errorLog": null
  },
  "body": {
    "numUnread": 2,
    "lastModified": "2015-06-16T15:29:24.606+10:00",
    "_meta": {
      "numFound": 3,
      "pageSize": 50,
      "pageNum": 0
    },
    "conversations": [
      {
        "email": "xuyin@ebay.com",
        "id": "3:iayuborn",
        "buyerName": "Automation dealer via Gumtree",
        "sellerName": "Zoe via Gumtree",
        "adId": "1076938310",
        "role": "Seller",
        "unread": true,
        "attachments": [],
        "robot": null,
        "textShortTrimmed": "reply from VIP on desktop and tablet Automation dealer",
        "senderEmail": null,
        "boundness": "INBOUND",
        "receivedDate": "2015-06-16T15:29:24.571+10:00",
        "offerId": null
      },
      {
        "email": "xuyin@ebay.com",
        "id": "9n81:iaxjx6qh",
        "buyerName": "Carol via Gumtree",
        "sellerName": "Zoe via Gumtree",
        "adId": "1076938310",
        "role": "Seller",
        "unread": true,
        "attachments": [],
        "robot": null,
        "textShortTrimmed": "Hi Zoe, I'm interested in your \"1973 BMW 16 Sedan...\" on Gumtree. Is there anything I need to know about it? When can I inspect it? Please contact me. Thanks! Carol",
        "senderEmail": null,
        "boundness": "INBOUND",
        "receivedDate": "2015-06-16T11:38:52.979+10:00",
        "offerId": null
      },
      {
        "email": "xuyin@ebay.com",
        "id": "3:iahjmid8",
        "buyerName": "zoe via Gumtree",
        "sellerName": "Cindy via Gumtree",
        "adId": "1076938355",
        "role": "Buyer",
        "unread": false,
        "attachments": [],
        "robot": null,
        "textShortTrimmed": "sdasdf adf",
        "senderEmail": null,
        "boundness": "OUTBOUND",
        "receivedDate": "2015-06-04T13:39:43.620+10:00",
        "offerId": null
      }
    ]
  },
  "pagination": null
}
```

### GetPostBoxConversation

*Seller*

`GET http://host/ebayk-msgcenter/postboxes/xuyin@ebay.com/conversations/9n81:iaxjx6qh`

```
{
  "status": {
    "state": "OK",
    "details": null,
    "errorLog": null
  },
  "body": {
    "id": "9n81:iaxjx6qh",
    "role": "Seller",
    "buyerEmail": "avennajl@hotmail.com",
    "sellerEmail": "xuyin@ebay.com",
    "buyerName": "Carol via Gumtree",
    "sellerName": "Zoe via Gumtree",
    "adId": "1076938310",
    "messages": [
      {
        "receivedDate": "2015-06-16T11:38:20.030+10:00",
        "boundness": "INBOUND",
        "textShort": "Hi Zoe,\nI'm interested in your \"1973 BMW 16 Sedan...\" on Gumtree.\nIs there anything I need to know about it?\nWhen can I inspect it?\nPlease contact me.\nThanks!\nCarol",
        "textShortTrimmed": "Hi Zoe, I'm interested in your \"1973 BMW 16 Sedan...\" on Gumtree. Is there anything I need to know about it? When can I inspect it? Please contact me. Thanks! Carol",
        "attachments": [],
        "offerId": null,
        "robot": null,
        "senderEmail": "avennajl@hotmail.com"
      }
    ],
    "numUnread": 0,
    "negotiationId": null
  },
  "pagination": null
}
```

*Buyer*

`GET http://host:8080/ebayk-msgcenter/postboxes/xuyin@ebay.com/conversations/3:iahjmid8`

```
{
  "status": {
    "state": "OK",
    "details": null,
    "errorLog": null
  },
  "body": {
    "id": "3:iahjmid8",
    "role": "Buyer",
    "buyerEmail": "xuyin@ebay.com",
    "sellerEmail": "Cxin@ebay.com",
    "buyerName": "zoe via Gumtree",
    "sellerName": "Cindy via Gumtree",
    "adId": "1076938355",
    "messages": [
      {
        "receivedDate": "2015-06-04T13:16:43.737+10:00",
        "boundness": "OUTBOUND",
        "textShort": "sdasdf\nzoe",
        "textShortTrimmed": "sdasdf zoe",
        "attachments": [],
        "offerId": null,
        "robot": null,
        "senderEmail": "xuyin@ebay.com"
      },
      {
        "receivedDate": "2015-06-04T13:24:46.244+10:00",
        "boundness": "OUTBOUND",
        "textShort": "dfsd as\nadf",
        "textShortTrimmed": "dfsd as adf",
        "attachments": [],
        "offerId": null,
        "robot": null,
        "senderEmail": "xuyin@ebay.com"
      },
      {
        "receivedDate": "2015-06-04T13:28:12.468+10:00",
        "boundness": "OUTBOUND",
        "textShort": "sd sdas\nadf",
        "textShortTrimmed": "sd sdas adf",
        "attachments": [],
        "offerId": null,
        "robot": null,
        "senderEmail": "xuyin@ebay.com"
      },
      {
        "receivedDate": "2015-06-04T13:39:18.902+10:00",
        "boundness": "OUTBOUND",
        "textShort": "sdasdf\nadf",
        "textShortTrimmed": "sdasdf adf",
        "attachments": [],
        "offerId": null,
        "robot": null,
        "senderEmail": "xuyin@ebay.com"
      }
    ],
    "numUnread": 0,
    "negotiationId": null
  },
  "pagination": null
}
```

