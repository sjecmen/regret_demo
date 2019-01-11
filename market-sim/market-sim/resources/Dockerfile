# Dockerfile for use by runner
FROM ubuntu:15.10

RUN apt-get update && apt-get install -y apt-utils make ca-certificates-java maven openjdk-8-jdk
# TODO Ideally this would also install maven dependencies, but that's hard without direct docker access
