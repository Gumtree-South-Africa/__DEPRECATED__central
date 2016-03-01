# ebayk-listener-jsonlogging

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-message-json-logging
(original git hash: 133d1b17189e4c24ee2dd4d1328e9b51387a5fae)

# Description

Logs message content's (with information about conversation) into a logfile. Data is logged in json style (one flat json object per line).

The intention of this plugin's output is to get the created logfiles into hadoop and then use hive with the JSON SerDe to analyze the data.

Every json object will look like this (of course compacted into one line)

* messageId
* conversationId
* adId
* buyerMail
* sellerMail
* messageDirection
* from
* to
* all custom values
* outcome state
* number of message in this conversation (starting with 0 for the contact poster mail, 1 will be the first reply,...)
* conversation creation date
* message received date

Please note that this plugin will log every message only once. later updates due to state changes (from HELD to SENT)  will be ignored.


# Configuring Logback for Hadoop Logging
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
