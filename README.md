# ReplyTS2 for Comaas

## Dev Setup

Before you start, setup vagrant as described here: https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki/ReplyTS-Environment-Setup

Clone this repository, and finally clone all the submodules:
```
git submodule update --init
#git submodule foreach git checkout master
#git submodule foreach git pull origin master
```

To run RTS2 create the following run configuration:

* Type: application
* Main class: `com.ecg.replyts2.ECGReplyTS2`
* VM arguments: `-DconfDir=replyts2-wrapper/conf -DlogDir=.`
* Module: `replyts2-wrapper`
* Working directory: the project directory (which is the default)
