FROM ghcr.io/dropchop-com/snakejar/build-base:latest

ARG UNAME=testuser
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID -o $UNAME
RUN useradd -m -u $UID -g $GID -o -s /bin/bash $UNAME
RUN mkdir -p /build/venv-3.8 \
  && mkdir -p /build/venv-3.9 \
  && mkdir -p /build/venv-3.10 \
  && mkdir -p /build/venv-3.11 \
  && mkdir -p /build/venv-3.12 \
  && mkdir -p /build/src \
  && chown -R $UID:$GID /build

WORKDIR /build/src
USER $UNAME

COPY requirements.txt /build/

RUN python3.8 -m venv /build/venv-3.8
RUN  . /build/venv-3.8/bin/activate \
  && python3.8 -m pip install --upgrade pip \
  #&& python3.8 -m pip install wheel \
  && pip3.8 install -r /build/requirements.txt

RUN python3.9 -m venv /build/venv-3.9
RUN  . /build/venv-3.9/bin/activate \
  && python3.9 -m pip install --upgrade pip \
  #&& python3.9 -m pip install wheel \
  && pip3.9 install -r /build/requirements.txt

RUN python3.10 -m venv /build/venv-3.10
RUN  . /build/venv-3.10/bin/activate \
  && python3.10 -m pip install --upgrade pip \
  #&& python3.10 -m pip install wheel \
  && pip3.10 install -r /build/requirements.txt

RUN python3.11 -m venv /build/venv-3.11
RUN  . /build/venv-3.11/bin/activate \
  && python3.11 -m pip install --upgrade pip \
  #&& python3.11 -m pip install wheel \
  && pip3.11 install -r /build/requirements.txt

RUN python3.12 -m venv /build/venv-3.12
RUN  . /build/venv-3.12/bin/activate \
  && python3.12 -m pip install --upgrade pip \
  #&& python3.12 -m pip install fasttext-wheel \
  && pip3.12 install -r /build/requirements.txt


COPY src/main/docker/run_gradlew.sh /build/

CMD /build/run_gradlew.sh