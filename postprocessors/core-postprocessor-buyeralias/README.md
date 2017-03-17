# ebayk-postprocessor-buyeralias

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-buyeralias-plugin
(original git hash: 6ffec0ccaeb6a1b87a3b3738def3b69b0ad9882d)

# Description

replyts2-buyeralias-plugin (now also with seller alias ;) )
==========================

Set a buyer/seller alias as mail from, e.g.: buyername &lt;buyer123@domain.de>
This plugin is based on a custom header, name: `X-CUST-BUYER-NAME` and `X-CUST-SELLER-NAME`.
If the headers are not available feature is turned of. (but only one of both can be used too)

Properties Change
==========================
replyts.buyeralias.formatPattern=%s from eBay Classifieds


