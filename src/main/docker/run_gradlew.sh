#!/usr/bin/env sh

PYTHON_VERSION="${1}"

if [ "$PYTHON_VERSION" != "3.9" ] && [ "$PYTHON_VERSION" != "3.8" ] ; then
  echo "First argument should be python version 3.8 or 3.9."
  exit 1
fi
shift;

. "/build/venv-${PYTHON_VERSION}/bin/activate"
echo "Activated virtual environment... /build/venv-${PYTHON_VERSION}"
./gradlew "$@"