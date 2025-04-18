FROM ubuntu:24.04

# Install python3.12
RUN apt-get update && \
    apt-get install -y python3.12 python3-pip && \
    rm -rf /var/cache/apt/* && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY requirements.txt .

# Setup and pack the required dependencies
RUN apt update && apt install -y python3.12-venv
RUN python3 -m venv venv
RUN . venv/bin/activate && pip install setuptools
RUN . venv/bin/activate && pip install -r requirements.txt
RUN . venv/bin/activate && venv-pack -o venv.tar.gz
