# gtau-filter-ebayservices

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-ebayservicesfilters-plugin
(original git hash: ee1ac811c9d338cb81436f711d1ee8f49182c403)

# Description

This plugin is a collection of three filters communicating with eBay Webservices. These filters exist:

* IP 2 Country Filter
* IP Risk
* Account state of a mail address

*Attention:* Service Quality of these filters is somewhat unreliable, failure rates of more than 10% are rather common - especially with socket timeouts. Expect these filters to fail frequently.

Difference with the `core-filter-ebayservices`:
- IP2CountryFilter is using LBS2 Library

### IP 2 Country Filter
Does Ip2Country resolution and assigns a score depending on that country.

Works on every  mail in conversation that has `X-CUST-IP`. Checks if that mail has a header `X-CUST-IP` containing an IPV4 address (assuming that was the sender's
ip address when he filled out the contact poster form on the website).


### IP Risk Filter
eBay maintains a list of IP addresses that are likely to be abused for spam/fraud.

Works on every  mail in conversation that has `X-CUST-IP`. Checks if that mail has a header `X-CUST-IP` containing an IPV4 address (assuming that was the sender's
ip address when he filled out the contact poster form on the website).

### User State filter
Check's if the sender's mail address is unknown to eBay, has an eBay account or is blocked ad ebay. Assigns configurable scores for each of these options.

## Configuration

These filters will need valid eBay API credentials in order to work. (IAF tokens, app names and proxy settings for talking to that API). The filters will expect to find them in the global `replyts.properties` at:

```
replyts2-ebayservicesfilters-plugin.ip.appname
replyts2-ebayservicesfilters-plugin.ip.iaftoken

 # attention: normally different IAF tokens apply for the user filter than for the IP filters.
replyts2-ebayservicesfilters-plugin.user.appname
replyts2-ebayservicesfilters-plugin.user.iaftoken
```



### IP 2 Country plugin instance configuration (rules)
```
{
    'DEFAULT': 100,
    'DE': 0,
    'UK':200
}
```
The configs maps ISO country codes to a score mails from this country will get assigned. If the country a mail originates
from is not listed in the configuration, the `DEFAULT` score will be given.

curl -X PUT http://kreplyts44-1.220:42001/configv2/com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country.Ip2CountryFilterFactory/Ip2Country -H "Content-Type: application/json" -d '
{
    state: "ENABLED",
    priority: 100,
    configuration: {
        'DEFAULT': 100,
        'LV': 75,
        'ID': 75,
        'HK': 75,
        'MA': 75,
        'HR': 75,
        'BA': 75,
        'BR': 75,
        'IT': 75,
        'BJ': 75,
        'US': 75,
        'HU': 75,
        'GB': 75,
        'TH': 75,
        'MY': 75,
        'NA': 75,
        'CN': 75,
        'CM': 75,
        'RO': 75,
        'LT': 75,
        'CA': 75,
        'NG': 500,
        'DE': 0
} }'

### IP Risk Filter plugin instance configuration
```
{
    'VERY_BAD': 100,
    'BAD': 100,
    'MEDIUM_BAD': 0,
    'GOOD' : 0
}
```
*Attention:* to our experience, the distinction between the different levels of badness is not quite good.
Especially `BAD` and `MEDIUM_BAD` produce rather unreliable results. We got best results with the configuration from the example.

```
curl -X PUT http://kreplyts44-1.220:42001/configv2/com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk.IpRiskFilterFactory/IpRisk -H "Content-Type: application/json" -d '
{
    state: "ENABLED",
    priority: 100,
    configuration: {
        'VERY_BAD': 100,
        'BAD': 100,
        'MEDIUM_BAD': 0,
        'GOOD' : 0
} }'
```

### User State Filter
```
{
    'UNKNOWN': 0,
    'CONFIRMED': -50,
    'SUSPENDED': 100
}
```
put a filter config to replyts api:

```
curl -X PUT http://kreplyts44-1.220:42001/configv2/com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate.UserStateFilterFactory/UserStateFilter -H "Content-Type: application/json" -d '
{
    state: "ENABLED",
    priority: 100,
    configuration: {
        UNKNOWN: 0,
        CONFIRMED: -50,
        SUSPENDED: 30
} }'
```


## Contributor
Matthias Huttar/Andre Charton from eBay Kleinanzeigen Germany: mhuttar@ebay-kleinanzeigen.de
Matt Darapour from Gumtree Australia: mdarapour@ebay.com