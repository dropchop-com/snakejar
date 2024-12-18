#!/usr/bin/env sh

PYTHON_VERSION="${1}"

if [ "$PYTHON_VERSION" != "3.9" ] && [ "$PYTHON_VERSION" != "3.8" ] && [ "$PYTHON_VERSION" != "3.10" ] && [ "$PYTHON_VERSION" != "3.11" ] && [ "$PYTHON_VERSION" != "3.12" ] && [ "$PYTHON_VERSION" != "3.13" ] ; then
  echo "First argument should be python version 3.8, 3.9, 3.10, 3.11, 3.12 or 3.13"
  exit 1
fi
shift;

. "/build/venv-${PYTHON_VERSION}/bin/activate"
echo "Activated virtual environment... /build/venv-${PYTHON_VERSION}"
./gradlew "$@"