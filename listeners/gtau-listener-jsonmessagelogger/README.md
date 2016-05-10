# gtau-listener-jsonmessagelogger

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-message-json-logging
(original git hash: f19d032c78de2a3ad92da656fb740c1955ab6192)

# Description

# replyts2-message-json-logging
Logs message content's (with information about conversation) into a logfile. Data is logged in json style (one flat json object per line).

The intention of this plugin's output is to get the created logfiles into hadoop and then use hive with the JSON SerDe to analyze the data.

Every json object will look like this (of course compacted into one line)

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

Please note that this plugin only appends to the log. Existing log entries are never updated. Any changes to a message - ie, state changes (from HELD to SENT) - will have its own log entry.

# Configuring Logback for JSON Logging
Sample configuration that creates log files for hadoop in the log directory. they always get the current date suffixed. only the message is logged (no timestamp or anything else).
logs are kept for 3 and are then automatically deleted

```xml
<logger name="message-json-log" level="info">
    <appender name="HADOOP_DAILY" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder>
            <pattern>%msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${logDir}/hadoop-MESSAGE_EVENTS_V1.%d{yyyy-MM-dd}</fileNamePattern>
            <maxHistory>3</maxHistory>
        </rollingPolicy>
    </appender>
</logger>
```

# Configuring Logback for DB Logging
Sample configuration that creates log for hadoop in a database.

```xml
<logger name="message-json-log" level="INFO">
    <appender name="DWH_DAILY" class="com.ecg.au.gumtree.messagelogging.RTSDBAppender">
        <connectionSource class="ch.qos.logback.core.db.DriverManagerConnectionSource">
            <driverClass>com.mysql.jdbc.Driver</driverClass>
            <url>jdbc:mysql://dev-common.local/replyts</url>
            <user>box</user>
            <password>box</password>
        </connectionSource>
    </appender>
</logger>
```

The table structure:

```SQL
CREATE TABLE replyts.rts2_event_log
(
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
    custip VARCHAR(20) NOT NULL
);
```
