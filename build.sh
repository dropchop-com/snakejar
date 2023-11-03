#!/usr/bin/env bash

project_groupId=$1
project_version=$2

docker build \
  --build-arg UID="$(id -u)" \
  --build-arg GID="$(id -g)" \
  -f src/main/docker/Dockerfile \
  -t ghcr.io/dropchop-com/snakejar/build:latest \
  -t ghcr.io/dropchop-com/snakejar/build:20231103 .

docker run -v "$(pwd)":/build/src ghcr.io/dropchop-com/snakejar/build:latest \
  /build/run_gradlew.sh "3.8" clean build --warning-mode all \
    -Pgroup="${project_groupId}" \
    -Pversion="${project_version}"

docker run -v "$(pwd)":/build/src ghcr.io/dropchop-com/snakejar/build:latest \
  /build/run_gradlew.sh "3.9" clean build --warning-mode all \
    -Pgroup="${project_groupId}" \
    -Pversion="${project_version}"

docker run -v "$(pwd)":/build/src ghcr.io/dropchop-com/snakejar/build:latest \
  /build/run_gradlew.sh "3.10" clean build --warning-mode all \
    -Pgroup="${project_groupId}" \
    -Pversion="${project_version}"

docker run -v "$(pwd)":/build/src ghcr.io/dropchop-com/snakejar/build:latest \
  /build/run_gradlew.sh "3.11" clean build --warning-mode all \
    -Pgroup="${project_groupId}" \
    -Pversion="${project_version}"

docker run -v "$(pwd)":/build/src ghcr.io/dropchop-com/snakejar/build:latest \
  /build/run_gradlew.sh "3.12" clean build --warning-mode all \
    -Pgroup="${project_groupId}" \
    -Pversion="${project_version}"