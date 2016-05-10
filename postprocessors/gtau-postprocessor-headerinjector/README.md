# gtau-postprocessor-headerinjector

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-header-injector-preparator
(original git hash: ffed0fca2becb5b614415aba23a54f786df82497)

# Description

# Header Injector Plugin
This Postprocessor plugin adds configurable headers to the outgoing emails' header.

In detail, this plugin will:

Look for the configured headers in every conversation and will inject them into the outgoing email. for instance if
header "Foo" is defined to be injected to the outgoing email, this plugin looks for "Foo" header in the conversation,
once the header's found and has value, it will be injected to the cutome headers of the outgoing email.

## Configuration
just put the plugin into your classpath and add these entries to `replyts.properties`:

```

#comma seperated list of headers
replyts.header-injector.headers=Foo,Bar;
```