FROM adoptopenjdk/openjdk8:jdk8u202-b08-alpine

LABEL maintainer="Comaas team <DL-eCG-comaas-dev@ebay.com>"

COPY maven/target/dependency /app

# This does not work without --network host - but we cannot add it because maven plugin that we use for building comaas
# does not support it. Without a proxy we cannot fetch the packages e.g. ca-certificates - ignore for now
# COMAAS-1773 - for a proper fix
# ENV HTTP_PROXY "http://proxy.cloud.ecg.so:3128"
# ENV NO_PROXY ".ecg.so,.ams1.cloud,.dus1.cloud,.corp.ebay.com,.ecg.tools"
# RUN apk add --no-cache ca-certificates

COPY ./docker-entrypoint.sh /

ENTRYPOINT ["/docker-entrypoint.sh"]