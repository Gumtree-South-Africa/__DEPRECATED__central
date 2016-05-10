# ebayk-filter-belenblockedad

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-belenblockedadfilter-plugin
(original git hash: e59c45c8c7597bb87b3338592c4499ea8daf86d2)

# Description

This plugin implements a basic blocked ad filter. It connects to a belen/box style database and queries if the ad is ACTIVE or not.
If the ad is not active, it checks the ad's TNS record for the blocking reason.
In case this one should be `NO_REASON_AT_ALL` (*) it assumes that this ad is blocked for spam/fraud reasons and also drops the mail.

#### About NO_REASON_AT_ALL
 this is nothing more than a legacy blocking reason that we use as blocking reason for spam/fraud. other sites will probably
 need to fork this plugin and update the string here.

# Configuration

There are two parts in this plugin's configuration:
* datasource configuration in `replyts.properties`
* filter instance (dummy) configuration

### Dummy Filter Instance Configuration
This plugin is rather complex, therefore the config is put into the `replyts.properties` file in your conf dir.
A plugin instance configuration needs to be present tough to activate this filter. This configuration is as simple as can be, an empty JSON Object.

The below curl command will create such a dummy filter configuration.

```
curl -H "Content-Type: application/json"  -X PUT "http://localhost:8081/configv2/com.ecg.de.kleinanzeigen.replyts.belen.blockeduser.BlockedAdFilterFactory/blocked-ads" -d "{'state': 'ENABLED', 'priority':100, 'configuration':{}}"
```

### Datasource configuration
Datasource configuration is a rather tricky beast, that will be kept in `replyts.properties` within the `confDir`.
Internally, this plugin uses a c3p0 datasource for connection management. The connection is read-only.
```
replyts2-belenblockedadfilter-plugin.dataSource.url # jdbc connection url to the correct datasource
replyts2-belenblockedadfilter-plugin.username #default: belen
replyts2-belenblockedadfilter-plugin.password #default: <empty>
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.initialPoolSize #default: 5
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.minPoolSize #default: 3
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.maxPoolSize #default: 100
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.acquireIncrement #default: 5
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.maxIdleTime #default: 90
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.maxConnectionAge #default: 900
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.idleConnectionTestPeriod #default: 30
replyts2-belenblockedadfilter-plugin.dataSource.name #default: belen_user
replyts2-belenblockedadfilter-plugin.dataSource.pool.checkoutTimeout #default: 2000
replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.numHelperThreads #default: 10
```

# Running integration tests
To run integration tests, one can pass the `replyts2-belenblockedadfilter-plugin.dataSource.url` param to the test
runner as JVM param: `-Dreplyts2-belenblockedadfilter-plugin.dataSource.url=...`

# Database Schema Assumptions
This plugin assumes to find a table named `ad_tns` looking like this:

```
+-----------------------+-------------+------+-----+---------+----------------+
| Field                 | Type        | Null | Key | Default | Extra          |
+-----------------------+-------------+------+-----+---------+----------------+
| id                    | bigint(20)  | NO   | PRI | NULL    | auto_increment |
| creation_date         | datetime    | NO   |     | NULL    |                |
| modification_date     | datetime    | NO   |     | NULL    |                |
| version               | bigint(20)  | YES  |     | NULL    |                |
| action_log            | text        | NO   |     | NULL    |                |
| admin_id              | bigint(20)  | YES  | MUL | NULL    |                |
| admin_username        | varchar(50) | YES  |     | NULL    |                |
| ad_id                 | bigint(20)  | NO   | UNI | NULL    |                |
| delay_expiration_date | datetime    | YES  | MUL | NULL    |                |
| delay_minutes         | int(11)     | NO   |     | NULL    |                |
| filter_score          | int(11)     | NO   |     | NULL    |                |
| score_group_index     | int(11)     | NO   |     | NULL    |                |
| send_message          | bit(1)      | NO   |     | NULL    |                |
| score_desc            | text        | YES  |     | NULL    |                |
| delete_reason         | varchar(50) | YES  |     | NULL    |                |
| admin_notes           | text        | YES  |     | NULL    |                |
+-----------------------+-------------+------+-----+---------+----------------+
```

the `ad_id` and `delete_reason` cols are required, the rest is ignored. If the ad is blocked, the contents of `delete_reason` are assumed to be `NO_REASON_AT_ALL`.
The query will look like this: `select delete_reason from ad_tns where ad_id=?`
