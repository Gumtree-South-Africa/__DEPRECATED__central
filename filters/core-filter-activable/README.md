# core-filter-activable

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-activable-filter
(original git hash: 5ca2584df739e5c19278a3ed081b7526aa8e901f)

# Description

This module contains some helper classes that allow filters to execute conditionally
for the initial conversation starter (replies coming from the platform, not from users'
mail clients).

Your filter will need to extend `ActivableFilter`.

Right now, the two criteria are user type and category.

User Type
---------

1. Change your filter to extend `ActivableFilter`.
2. Make sure your platform sets the `X-Process-Poster-Usertype` header on the initial
email.
3. Set the `runFor` key in your filter's configuration. For example, the
volume filter config may look like this:

    ```json
    {
       "rules": [
         {
           "allowance": 3,
           "perTimeValue": 1,
           "perTimeUnit": "MINUTES",
           "score": 100
         }
       ],
       "runFor": {
           "userType": [
             "FSBO", "REALESTATE"
           ]
       }
    }
    ```

This would only run the filter on initial-contact emails that have the
`X-Process-Poster-Usertype` header set to `FSBO` or `REALESTATE`.


Categories
----------

1. Change your filter to extend `ActivableFilter`.
2. Make sure your platform sets the `X-Cust-Ad-Categories` header on the 
initial email. This header should contain a comma-separated list of 
category IDs for the ad starting with the highest-level ID followed by its 
children. For example, the header value of "27,174" would indicate that the ad
is in category.
    
    ```
    root
    |-> 27
      |-> 174
    ```
3. Set the `runFor` key in your filter's configuration. For example, the
volume filter config may look like this:

    ```json
    {
       "rules": [
         {
           "allowance": 3,
           "perTimeValue": 1,
           "perTimeUnit": "MINUTES",
           "score": 100
         }
       ],
       "runFor": {
           "exceptCategories": [174],
           "categories": [27, 200]
       }
    }
    ```
The above means that the filter will run for categories 27 and 200 and their 
children, but not for category 174 and its children (even though 174 is a child
of 27).

Notes
-----

* The above criteria can be combined. This will form a conjunction (AND).
* The `runFor` config is optional. If not specified, the filter will run for
all mail.
* The code only looks at mail headers, so if the headers are missing (as they
would be in post-initial replies) the filters will not execute. Basically, this
means that if you specify a `runFor` rule in your filter config, the filter
will not run on follow-up mails. Yes, this is an issue and should be fixed.
* Mail headers aren't customizable right now.
