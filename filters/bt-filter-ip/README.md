# bt-filter-ip

Originally taken from https://github.corp.ebay.com/ecg-global/replyts2-commonfilter-plugin
(original git hash: 06171090fe018ca1622aed6921fdc3df9915fe1b)

# replyts2-commonfilter-plugin

A common filter to scoring the message with configued custom headers


Deploy:

1. put the jar in the rts lib

2. REST API to config the plugin:configv2/com.ecg.replyts.commonattributefilter.CommonAttributeFilterFactory/{instance ID}


The config format:

       {
          attribute: "IP",
          rules: [
              {regexp: "192.168.16.1", score: 100}
          ]
       }

<b>attribute</b>: should be set as a cust header for a conversition, such as: 
      
      X-CUST-IP:XXX.XXX.XXX
