# gtau-filter-echelon

Originally taken from https://github.corp.ebay.com/GumtreeAU/replyts2-echelon-filter
(original git hash: 78cf39bfa7cbd2d1dfb2a94456c5ab513be01e19)

# Description

# replyts2-echelon-filter
Migration of the ReplyTS 1 echelon filter to ReplyTS 2. Made by Gumtree Australia.

Echelon filter sends notification to Echelon upon receiving an email to verify Machine-Id and IP address values.
Only messages that have the Machine-Id and IP Address headers will trigger the filter.

# Filter configuration

The filter factory creating filter instances is com.ebay.replyts.australia.echelon.EchelonFilterFactory`.

Filter configurations should look like this:
 ```
 {
    endpointUrl: 'http://foo.com/',
    endpointTimeout: 10000,
    score: 0
 }
 ```

 endpointUrl and endpointTimeout are mandatory, default value for score is zero if not provided.



# Contributor
Matt Darapour from Gumtree Australia: mdarapour@ebay.com
