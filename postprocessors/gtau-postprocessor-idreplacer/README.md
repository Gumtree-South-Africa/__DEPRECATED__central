# gtau-postprocessor-idreplacer

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-id-replacer
(original git hash: 9fed873b8065744b988e9827e9b6d52400afa44b)

# Description

# ID Replacer Plugin
This Postprocessor plugin is used to generate a series of parameters for spam reporting, it searches for the HASH, 
CONVERSATION_ID and MESSAGE_ID keys in the conversation content and replaces them with their actual values.

In detail, this plugin will:

search the outgoing emails' text content based on the following regex and will replace them with their values
in the conversation:
`<%%CONVERSATION_ID%%>`
`<%%MESSAGE_ID%%>`
`<%%HASH%%>`

Immutable messages are ignored by the plugin.

## Configuration
Order can be defined by setting 'replyts-id-replacer.plugin.order' in replyts.properties file.
