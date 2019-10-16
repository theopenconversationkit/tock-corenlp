#!/usr/bin/env bash
echo "branch : $TRAVIS_BRANCH"
echo "pull_request : $TRAVIS_PULL_REQUEST"
echo "tag : $TRAVIS_TAG"
if [[ $TRAVIS_BRANCH = *"build"* || $TRAVIS_BRANCH = "master" ]] && [ "$TRAVIS_PULL_REQUEST" = 'false' ]; then
  openssl aes-256-cbc -K $encrypted_fb256eec881b_key -iv $encrypted_fb256eec881b_iv -in etc/codesigning.asc.enc -out etc/codesigning.asc -d
  gpg --fast-import etc/codesigning.asc
  if [ "$TRAVIS_TAG" = '' ];
  then
    mvn deploy -DskipTests=true -Dtravis --settings etc/deploy-settings.xml -U
  else
    if [[ $TRAVIS_TAG == *"build"* ]];
    then
      export TOCK_VERSION=$(cat pom.xml | grep "^    <version>.*</version>$" | awk -F'[><]' '{print $3}')
      echo "tock version : $TOCK_VERSION"
      echo "tock tag : $TRAVIS_TAG"
      mvn versions:set -DnewVersion="$TRAVIS_TAG"
      mvn versions:update-parent -DparentVersion="[$TRAVIS_TAG]"
      mvn deploy -Dmilestone --settings etc/deploy-settings.xml
    fi
  fi
fi