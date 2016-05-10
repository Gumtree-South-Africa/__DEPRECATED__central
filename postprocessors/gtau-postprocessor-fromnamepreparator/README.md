# gtau-postprocessor-alias

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-from-name-preparator
(original git hash: 99d5d45ed161a68eab58743cf7c4b35db26c6892)

# Description

# From Name Preparator Plugin
This Postprocessor plugin adds an alias to the 'From' field of outbound email's header.

### This plugin must be executed after the Anonymizer which has an Ordered value of 200.

## From Name Format
[From Name] \<Anonymous email\>

## Configuration
just put the plugin into your classpath and add these entries to `replyts.properties`:

```
#Buyer's alias name
replyts.from-name.header.buyer=Buyer
#Seller's alias name
replyts.from-name.header.seller=Seller
#Order of execution
replyts.from-name.plugin.order=250

```