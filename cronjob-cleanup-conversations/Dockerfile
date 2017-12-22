FROM openjdk:8u131-jre-alpine

LABEL maintainer="Comaas team <DL-eCG-comaas-dev@ebay.com>"

ADD target/cronjobs-cleanup-conversations-1-SNAPSHOT-jar-with-dependencies.jar /

ENTRYPOINT ["java", "-jar", "cronjobs-cleanup-conversations-1-SNAPSHOT-jar-with-dependencies.jar"]
