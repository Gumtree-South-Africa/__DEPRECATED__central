# mde-listener-dealerratinginvite

Originally taken from https://github.corp.ebay.com/mobile-de/replyts2-dealer-rating-invite-plugin
(original git hash: 99da1e3d1b5e130277665e555084cd542540d5c8)

# Description

Plugin that notifies dealer-rating-service to schedule new invites


How to test changes in integra
------------------------------

* merge changes into master branch

* release a new version of this artifact at:  
https://ci-jenkins.corp.mobile.de/jenkins/job/ebay-github/job/mobile-de/job/replyts2-dealer-rating-invite-plugin

* use this new version in coma-replyts-config in pom.xml:   
(http://git.corp.mobile.de/replyts/coma-replyts-config)  
    * use non-master branch, e.g. "team-fraud-fighters-branch-1"

* go to jenkins.corp.mobile.de and build coma-replyts-config:    https://jenkins.corp.mobile.de/jenkins/job/coma-replyts-config-and-filters-team-fraud-fighters-branch-1/

* use git-hash as revision in Autodeploy and deploy "comareplytsconfig" (ID 1429) (Applications: mobile-comareplyts-config.tar (COMAREPLYTS))

* restart comareplyts by
    * go to comareplyts44-1.XXX
    * sudo /etc/init.d/replyts-comareplyts44-1_b01_4200 restart
    * go to comareplyts44-2.XXX
    * sudo /etc/init.d/replyts-comareplyts44-2_b01_4200 restart
