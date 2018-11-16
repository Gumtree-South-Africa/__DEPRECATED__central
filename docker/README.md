docker run --rm -p 8080:8080 --network comaasdocker_default --volume $PWD/dropf:/opt/replyts/dropfolder --name comaas-gtuk comaas-gtuk

bin/build.sh -T gtuk -P container && bin/image.sh gtuk container

apk --no-cache update && apk --no-cache add bash curl