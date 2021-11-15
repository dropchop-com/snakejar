#!/usr/bin/env bash

project_groupId=$1
project_version=$2

docker build \
  --build-arg UID="$(id -u)" \
  --build-arg GID="$(id -g)" \
  -f src/main/docker/Dockerfile \
  -t snakejar_build .

docker run -v "$(pwd)":/build/src snakejar_build \
  /build/run_gradlew.sh "3.8" clean build \
    -Pgroup="${project_groupId}" \
    -Pversion="${project_version}"

docker run -v "$(pwd)":/build/src snakejar_build \
  /build/run_gradlew.sh "3.9" build \
    -Pgroup="${project_groupId}" \
    -Pversion="${project_version}"