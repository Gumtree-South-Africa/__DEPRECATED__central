#!/bin/bash

URL="$1"
NAME="$2"
SUBFOLDER="$3"
BRANCH="master"

if [ -z "$URL" ] || [ -z "$NAME" ] ; then
  echo "$0: <url>[@branch] <new-name> [subfolder-in-repo]"

  exit -1
fi

# Split clone URL and branch (if provided)

FULLURL="$URL"

echo "${URL}" | grep -q '@[\.a-zA-Z0-9]*$' && \
  BRANCH="$(echo "${URL}" | sed 's/.*@\([\.a-zA-Z0-9]*\)$/\1/')" && \
  URL="$(echo "${URL}" | sed 's/\(.*\)@[\.a-zA-Z0-9]*$/\1/')"

# Copy either the whole branch or just a sub-folder

if [ ! -z "$SUBFOLDER" ] ; then
  PARENTNAME=`basename "$URL"`

  echo "Will only copy sub-folder $SUBFOLDER from $PARENTNAME"

  git clone -b "$BRANCH" "$URL"

  if [ ! -d ${PARENTNAME}/${SUBFOLDER} ] ; then
    echo "Unable to find sub-folder ${SUBFOLDER}"

    exit -1
  fi

  mv ${PARENTNAME}/${SUBFOLDER} ${NAME}

  cd ${PARENTNAME}
  HASH=`git rev-parse HEAD`
  cd .. && rm -rf ${PARENTNAME}
else
  git clone -b "$BRANCH" "$URL" ${NAME}

  cd ${NAME}
  HASH=`git rev-parse HEAD`
  cd ..
fi

# Remove original repository .git and .gitignore

[ -d ${NAME}/.git ] && rm -rf ${NAME}/.git
[ -f ${NAME}/.gitignore ] && rm -f ${NAME}/.gitignore

# Add the original URL + git hash to the README.md

[ -f ${NAME}/README.md ] && mv ${NAME}/README.md ${NAME}/README.md.old

echo -e "# ${NAME}\n" >> ${NAME}/README.md
echo -e "Originally taken from $FULLURL" >> ${NAME}/README.md
echo -e "(original git hash: ${HASH})" >> ${NAME}/README.md

if [ -s ${NAME}/README.md.old ] ; then
  echo -e "\n# Description\n" >> ${NAME}/README.md
  cat ${NAME}/README.md.old >> ${NAME}/README.md
fi

rm -f ${NAME}/README.md.old

# Make a best effort attempt to strip the POM file

if [ ! -f ${NAME}/pom.xml ] ; then
  echo "No POM found - will not try to modify"

  exit 1
fi

ARTIPACK="<artifactId>${NAME}<\/artifactId>\\"$'\n''    <packaging>jar<\/packaging>\'$'\n'

sed '/\<parent\>/,/\<\/parent\>/d' ${NAME}/pom.xml | 
  grep -v '<\?xml \|^<project \|xmlns:xsi\|xsi:schema\|<modelVersion\|<name>\|<packaging>' | \
  sed -e "1 s/<artifactId>.*<\/artifactId>/${ARTIPACK}/; t" -e "1,// s//${ARTIPACK}/" | \
  tail -r | sed '2s/^$/sll-empty/' | tail -r | \
  sed '1s/^$/sll-empty/' | grep -v 'sll-empty' \
  > ${NAME}/pom.xml.stripped

echo -e '<?xml version="1.0" encoding="UTF-8"?>' > ${NAME}/pom.xml
echo -e '<project xmlns="http://maven.apache.org/POM/4.0.0"' >> ${NAME}/pom.xml
echo -e '         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' >> ${NAME}/pom.xml
echo -e '         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">' >> ${NAME}/pom.xml
echo -e '    <modelVersion>4.0.0</modelVersion>\n' >> ${NAME}/pom.xml

echo -e '    <parent>' >> ${NAME}/pom.xml
echo -e '        <groupId>ecg.comaas</groupId>' >> ${NAME}/pom.xml
echo -e '        <artifactId>comaas</artifactId>' >> ${NAME}/pom.xml
echo -e '        <version>${revision}</version>' >> ${NAME}/pom.xml
echo -e '        <relativePath>../../pom.xml</relativePath>' >> ${NAME}/pom.xml
echo -e '    </parent>\n' >> ${NAME}/pom.xml

cat -s ${NAME}/pom.xml.stripped >> ${NAME}/pom.xml

rm -f ${NAME}/pom.xml.stripped
