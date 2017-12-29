FROM openjdk:8u131-jre-alpine

LABEL maintainer="Comaas team <DL-eCG-comaas-dev@ebay.com>"

RUN apk --no-cache add bind-tools

COPY target/cronjobs-cleanup-conversations-1-SNAPSHOT.jar /
COPY dc_aware_wrapper.sh /dc_aware_wrapper.sh
COPY src/main/resources/* /

ENTRYPOINT ["/dc_aware_wrapper.sh"]
