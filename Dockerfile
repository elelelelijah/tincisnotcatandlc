FROM alpine

RUN mkdir /data
WORKDIR /data

RUN apk update
#RUN apk fetch openjdk8
#RUN apk add openjdk8 bash maven
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
# 1. Updated to package names for OpenJDK 17
RUN apk update && \
    apk add openjdk17-jre bash maven

# 2. Updated path to point to the Java 17 home directory
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

ADD . .
RUN ./build.sh
EXPOSE 4567/tcp
CMD ./run
