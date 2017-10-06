FROM openjdk:8u131-jre-alpine

LABEL maintainer="Comaas team <TODO>"

VOLUME /opt/replyts/dropfolder /opt/replyts/logs

ADD docker/docker-entrypoint.sh /

ADD distribution/target/docker/distribution /opt/replyts

ENTRYPOINT ["/docker-entrypoint.sh"]
