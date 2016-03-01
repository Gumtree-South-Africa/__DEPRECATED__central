# ebayk-listener-dailyreport

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-dailyreport-plugin
(original git hash: 70eb418c709165b5df1530bf2235e4c99ffd93c0)

# Description

Hadoop integration plugin to report statistics to hadoop that can be used to generate reports.
Contains several message listeners with different purpose.

Please be aware that:
* Only generates log output.
* All log output is on `info` level.
* You will need to configure your logging.xml to enable info logging for the hadoop logger categories you're interested in
* You will also need to take care of delivering these logs to hadoop and having them processed there.

## MessageStateListener
Message Processed Listener that appends it's contents to a logger category named `hadoop_daily_report`.

It implements a `MessageProcessedListener` that will generate a log entry (on level `info`) for every mail that is processed
it will log one of the following events:


* If the message has not been moderated (yet), the end state of the message (e.g. `HELD`, `SENT`, `ORPHANED`, `UNPARSEABLE`,...)
* If the message was moderated, it will log: `FROM_[HELD|BLOCKED]_TO_[SENT|BLOCKED]`

This information can be used to

## FilterRulePerformanceListener
Generates statistics about Filter Rule performance. Appends to a logger category called `hadoop_filter_performance`. All Colums are `Tab` seperated

for every filter rule that fires, it will log out:
```
FIRE  [FilterFactory] [FilterInstance]    [score]   [filterstate]   [uihint]
```
This can be multiple times per message.
When a message is moderated, all filter hits will be reported again. Either as `CONFIRMED` (if the agent confirmed that this message was bad)
or `MISFIRE` (if the agent actually decided to deliver this message to the receiver)
```
[CONFIRMED|MISFIRE]  [FilterFactory] [FilterInstance]    [score]    [filterstate]   [uihint]
```

### Note on uihint field
For simplicity in processing the data, the uihint that is logged will be slightly filtered: The characters `\r`, `\n`
and `\t` (carriage return, line break and tab) will be stripped off.

### Examples:
```
FIRE com.ecg.de.kleinanzeigen.wordfilter.WordfilterFactory  swearwords  50  OK  \bassholes?\b
CONFIRMED   com.ecg.de.kleinanzeigen.wordfilter.WordfilterFactory  swearwords  50   OK  \bassholes?\b
MISFIRE com.ecg.de.kleinanzeigen.wordfilter.WordfilterFactory  swearwords  50   OK  \bassholes?\b
```


# Configuring Logback for Hadoop Logging
Sample configuration that creates log files for hadoop in the log directory. they always get the current date suffixed. only the message is logged (no timestamp or anything else).
logs are kept for 3 and are then automatically deleted

```xml
 <logger name="hadoop_daily_report" level="info">
        <appender name="HADOOP_DAILY" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <encoder>
                <pattern>%msg%n</pattern>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${logDir}/hadoop-REPLYTS_EVENTS.%d{yyyy-MM-dd}</fileNamePattern>
                <maxHistory>3</maxHistory>
            </rollingPolicy>
        </appender>
    </logger>
    <logger name="hadoop_filter_performance" level="info">
        <appender name="HADOOP_FILTER_PERFORMANCE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <encoder><pattern>%msg%n</pattern></encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${logDir}/hadoop-REPLYTS_FILTERPERFORMANCE.%d{yyyy-MM-dd}</fileNamePattern>
                <maxHistory>3</maxHistory>
            </rollingPolicy>
        </appender>

    </logger>
```
