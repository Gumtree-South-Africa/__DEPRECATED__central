FROM openjdk:8u151-jdk-alpine

LABEL maintainer="Comaas team <DL-eCG-comaas-dev@ebay.com>"

ARG src_dir=docker_image_src

RUN apk add --no-cache ca-certificates

ADD docker/docker-entrypoint.sh /

ADD $src_dir/* /opt/replyts

ENTRYPOINT ["/docker-entrypoint.sh"]
