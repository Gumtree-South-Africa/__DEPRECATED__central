# kjca-coremod-overrides

Originally taken from https://github.corp.ebay.com/ecg-kijiji-ca/replyts
(original git hash: b89efb90f7ffa69dd1b1096f04b428c584b991ab)

This module contains Canada-specific overrides for some functionality in RTS2 core:

* Anonymization / addressing based on a custom header
* Message body rewriting to prevent accidental de-anonymization
* Plugin for sending metrics about logging to Graphite
* Configuration change that ensures that held messages are released on a predictable schedule throughout the day.
