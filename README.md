# ReplyTS2 for Comaas

## Dev Setup

As a first step setup vagrant as described here: https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki/ReplyTS-Environment-Setup 

**Skip this step when you are using mpnexus**.

 Currently this project depends on ReplyTS2 Core to be available via maven, which means that we need to get ReplyTS2 Core in the correct version (see pom) and then install that
```
git clone git@github.corp.ebay.com:ReplyTS/replyts2-core.git
git checkout tags/<version>
mvn clean install
```

In the next step we clone the project itself as well as all the submodules (filters/plugins)
```
git clone git@github.corp.ebay.com:ecg-comaas/replyts2.git
cd replyts2
git submodule update --init
git submodule foreach git checkout master
git submodule foreach git pull origin master
```

To run RTS2 create the following run configuration:

* Type: application
* Main class: `com.ecg.replyts2.ECGReplyTS2`
* VM arguments: `-DconfDir=replyts2-wrapper/conf -DlogDir=.`
* Module: `replyts2-wrapper`
* Working directory: the project directory (which is the default)

