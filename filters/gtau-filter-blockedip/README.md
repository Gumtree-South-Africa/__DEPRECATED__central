# gtau-filter-blockedip

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-blockedipfilter-plugin
(original git hash: e17bd0c0c972cc6d3a81245623e9cb791caf25c2)

# Description

# Gumtree Australia Blocked IP Address Filter Plugin
 This plugin implements a basic blocked IP address filter. It connects to a belen/box style database and queries if the IP exists and is valid.
 If the IP is not expired, it drops the message.

# Configuration

There are two parts in this plugin's configuration:
* datasource configuration in `replyts.properties`
* filter instance (dummy) configuration

### Dummy Filter Instance Configuration
This plugin is rather complex, therefore the config is put into the `replyts.properties` file in your conf dir.
A plugin instance configuration needs to be present tough to activate this filter. This configuration is as simple as can be, an empty JSON Object.

The below curl command will create such a dummy filter configuration.

```
curl -H "Content-Type: application/json"  -X PUT "http://localhost:8081/configv2/com.ebay.au.gumtree.replyts.blockedip.BlockedIpFilterFactory/blocked-ips" -d "{'state': 'ENABLED', 'priority':100, 'configuration':{}}"
```

### Datasource configuration
Datasource configuration is kept in `replyts.properties` within the `confDir`.
Internally, this plugin uses a c3p0 datasource for connection management. The connection is read-only.
```
replyts2-blockedipfilter-plugin.dataSource.url # jdbc connection url to the correct datasource
replyts2-blockedipfilter-plugin.username #default: box
replyts2-blockedipfilter-plugin.password #default: <empty>
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.initialPoolSize #default: 5
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.minPoolSize #default: 3
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.maxPoolSize #default: 100
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.acquireIncrement #default: 5
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.maxIdleTime #default: 90
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.maxConnectionAge #default: 900
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.idleConnectionTestPeriod #default: 30
replyts2-blockedipfilter-plugin.dataSource.name #default: belen_tns
replyts2-blockedipfilter-plugin.dataSource.pool.checkoutTimeout #default: 2000
replyts2-blockedipfilter-plugin.dataSource.pool.c3p0.numHelperThreads #default: 10
```

# Database Schema Assumptions
This plugin assumes to find a table named `ip_ranges` looking like this:

```
+-------------------+-------------+------+-----+---------+----------------+
| Field             | Type        | Null | Key | Default | Extra          |
+-------------------+-------------+------+-----+---------+----------------+
| action_log        | text        | NO   |     | NULL    |                |
| admin_id          | bigint(20)  | YES  |     | NULL    |                |
| admin_username    | varchar(50) | YES  |     | NULL    |                |
| begin_ip          | varchar(15) | NO   | MUL | NULL    |                |
| creation_date     | datetime    | NO   |     | NULL    |                |
| duration_minutes  | int(11)     | NO   |     | NULL    |                |
| end_ip            | varchar(15) | NO   |     | NULL    |                |
| expiration_date   | datetime    | NO   |     | NULL    |                |
| id                | bigint(20)  | NO   | PRI | NULL    | auto_increment |
| modification_date | datetime    | NO   |     | NULL    |                |
| version           | bigint(20)  | YES  |     | NULL    |                |
+-------------------+-------------+------+-----+---------+----------------+

```

the `begin_ip`, `end_ip` and `expiration_date` cols are required, the rest is ignored. If the IP is blocked, the contents of `expiration_date` are assumed to be a timestamp after the current timestamp.
The query will look like this: `select expiration_date from ip_ranges where ? between begin_ip and end_ip order by expiration_date desc`
