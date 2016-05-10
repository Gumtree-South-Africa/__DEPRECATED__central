# ebayk-filter-bankaccount

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-bankaccountfilter-plugin
(original git hash: 30e4114d671c69ef16eb43ea0fcd7a7b2701f528)

# Description

This plugin implements a blocked/unblocked user functionality by connecting to the belen database and checking if the
mail's sender is a blocked user or not.

If the sender is blocked, the mail is dropped automatically.

# Configuration

There are two parts in this plugin's configuration:
* datasource configuration in `replyts.properties`
* filter instance (dummy) configuration

### Dummy Filter Instance Configuration
This plugin is rather complex, therefore the config is put into the `replyts.properties` file in your conf dir.
A plugin instance configuration needs to be present tough to activate this filter. This configuration is as simple as can be, an empty JSON Object.

The below curl command will create such a dummy filter configuration.

```
curl -H "Content-Type: application/json"  -X PUT "http://localhost:8081/configv2/com.ecg.de.kleinanzeigen.replyts.belen.blockeduser.BlockedUserFilterFactory/blocked-users" -d "{'state': 'ENABLED', 'priority':100, 'configuration':{}}"
```

### Datasource configuration
Datasource configuration is a rather tricky beast, that will be kept in `replyts.properties` within the `confDir`.
Internally, this plugin uses a c3p0 datasource for connection management. The connection is read-only.
```
replyts2-belenblockeduserfilter-plugin.dataSource.url # jdbc connection url to the correct datasource
replyts2-belenblockeduserfilter-plugin.username #default: belen
replyts2-belenblockeduserfilter-plugin.password #default: <empty>
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.initialPoolSize #default: 5
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.minPoolSize #default: 3
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.maxPoolSize #default: 100
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.acquireIncrement #default: 5
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.maxIdleTime #default: 90
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.maxConnectionAge #default: 900
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.idleConnectionTestPeriod #default: 30
replyts2-belenblockeduserfilter-plugin.dataSource.name #default: belen_user
replyts2-belenblockeduserfilter-plugin.dataSource.pool.checkoutTimeout #default: 2000
replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.numHelperThreads #default: 10
```

# Running integration tests
To run integration tests, one can pass the `replyts2-belenblockeduserfilter-plugin.dataSource.url` param to the test
runner as JVM param: `-Dreplyts2-belenblockeduserfilter-plugin.dataSource.url=...`

# Database Schema Assumptions
This plugin assumes to find a table named `userdata` looking like this:

```
+----------------------------------+--------------+------+-----+---------+----------------+
| Field                            | Type         | Null | Key | Default | Extra          |
+----------------------------------+--------------+------+-----+---------+----------------+
| id                               | bigint(20)   | NO   | PRI | NULL    | auto_increment |
| creation_date                    | datetime     | NO   |     | NULL    |                |
| modification_date                | datetime     | NO   |     | NULL    |                |
| email                            | varchar(255) | NO   | UNI | NULL    |                |
| legacy_id                        | bigint(20)   | YES  |     | NULL    |                |
| password                         | varchar(40)  | YES  |     | NULL    |                |
| status                           | varchar(20)  | NO   |     | NULL    |                |
| uuid                             | varchar(50)  | NO   | MUL | NULL    |                |
| version                          | bigint(20)   | YES  |     | NULL    |                |
| status_lifecycle                 | varchar(40)  | YES  |     | NULL    |                |
| imprint                          | text         | YES  |     | NULL    |                |
| marketing_opted_in               | bit(1)       | NO   |     | NULL    |                |
| accepted_conditions_and_policies | bit(1)       | NO   |     | NULL    |                |
| blocked_from_replying            | bit(1)       | NO   |     | b'0'    |                |
| registered                       | bit(1)       | NO   |     | b'0'    |                |
| fb_token                         | varchar(255) | YES  |     | NULL    |                |
| fb_id                            | bigint(20)   | YES  | MUL | NULL    |                |
| fb_pinboard_enabled              | bit(1)       | YES  |     | NULL    |                |
+----------------------------------+--------------+------+-----+---------+----------------+
```

the `email` and `status` params are required, the rest is ignored. If the user is blocked, the contents of `status` are assumed to be `blocked`.
The query will look like this: `select status from userdata where email=?`
