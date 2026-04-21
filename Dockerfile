FROM eclipse-temurin:8-jre

MAINTAINER Leanstar

RUN mkdir -p /sleep-parser/server/logs \
    /sleep-parser/server/temp

WORKDIR /sleep-parser/server

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENV TZ="Asia/Shanghai"

RUN cp /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ADD ./target/sleep-parser.jar ./app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dserver.port=${SERVER_PORT}", \
            "-jar", "app.jar"]

