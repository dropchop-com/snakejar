#!/usr/bin/env sh

PYTHON_VERSION="3.9"

for ARGUMENT in "$@"
do
    KEY=$(echo "${ARGUMENT}" | cut -f1 -d=)
    VALUE=$(echo "${ARGUMENT}" | cut -f2 -d=)
    case "$KEY" in
            -Ppython_version) PYTHON_VERSION=${VALUE} ;;
            *)
    esac
done

. "/build/venv-${PYTHON_VERSION}/bin/activate"
echo "Activated virtual environment... /build/venv-${PYTHON_VERSION}"
./gradlew "$@"