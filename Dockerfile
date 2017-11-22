FROM docker-registry.ecg.so/comaas/openjdk:8u151-jre-alpine

LABEL maintainer="Comaas team <DL-eCG-comaas-dev@ebay.com>"

VOLUME /opt/replyts/dropfolder /opt/replyts/logs

ADD docker/docker-entrypoint.sh /

ADD distribution/target/docker/distribution /opt/replyts

ENTRYPOINT ["/docker-entrypoint.sh"]
