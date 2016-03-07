# mde-postprocessor-uniqueid

Originally taken from https://github.corp.ebay.com/mobile-de/replyts2-mobilede-unique-id-plugin
(original git hash: d64d208f709fa3a3dbc51f7b99d3c78622e43a77)

# Description

Add a unique header id of the buyer for message sent to dealer sellers from buyers

name of header entry X-MOBILEDE-BUYER-ID

It supports the following configuration properties

replyts.uniqueid.order - order number of that plugin (mandatory parameter)
replyts.uniqueid.pepper - pepper that is used to generate a buyer id (optional parameter)
replyts.uniqueid.ignoredBuyerAddresses - comma separated list of buyer addresses for which no id shall be generated (optional parameter)







