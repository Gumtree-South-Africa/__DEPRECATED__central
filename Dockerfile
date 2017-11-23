FROM docker-registry.ecg.so/comaas/openjdk:8u131-jre-alpine

LABEL maintainer="Comaas team <DL-eCG-comaas-dev@ebay.com>"

ARG src_dir=docker_image_src

VOLUME /opt/replyts/dropfolder /opt/replyts/logs

ADD docker/docker-entrypoint.sh /

ADD $src_dir/* /opt/replyts

ENTRYPOINT ["/docker-entrypoint.sh"]
