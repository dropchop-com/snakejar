FROM ghcr.io/graalvm/graalvm-ce:java11-21

RUN microdnf module enable python38 \
  && microdnf install python38-pip \
  && microdnf install python38-devel \
  && microdnf install python38-wheel \
  && microdnf module enable python39 \
  && microdnf install python39-pip \
  && microdnf install python39-devel \
  && microdnf install python39-wheel \
  && microdnf clean all

ARG UNAME=testuser
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID -o $UNAME
RUN useradd -m -u $UID -g $GID -o -s /bin/bash $UNAME
RUN mkdir -p /build/venv-3.8 \
  && mkdir -p /build/venv-3.9 \
  && mkdir -p /build/src \
  && chown -R $UID:$GID /build

WORKDIR /build/src
USER $UNAME

COPY requirements.txt /build/

RUN python3.8 -m venv /build/venv-3.8
RUN  . /build/venv-3.8/bin/activate \
  && python3.8 -m pip install --upgrade pip \
  && pip3.8 install -r /build/requirements.txt

RUN python3.9 -m venv /build/venv-3.9
RUN  . /build/venv-3.9/bin/activate \
  && python3.9 -m pip install --upgrade pip \
  && pip3.9 install -r /build/requirements.txt

COPY src/main/docker/run_gradlew.sh /build/

CMD /build/run_gradlew.sh