FROM openjdk:8u151-jdk-alpine

LABEL maintainer="Comaas team <DL-eCG-comaas-dev@ebay.com>"

COPY maven/target/dependency /app

RUN apk add --no-cache ca-certificates

COPY ./docker-entrypoint.sh /

ENTRYPOINT ["/docker-entrypoint.sh"]