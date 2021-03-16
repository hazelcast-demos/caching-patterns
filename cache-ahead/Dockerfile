FROM adoptopenjdk/openjdk14:alpine as build

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN ./mvnw -B dependency:copy-dependencies

COPY src src

RUN ./mvnw -B compile

FROM adoptopenjdk/openjdk14:alpine-jre

WORKDIR app

ADD https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar libs/slf4j-simple-1.7.25.jar
ADD https://repo1.maven.org/maven2/io/debezium/debezium-connector-mysql/1.2.5.Final/debezium-connector-mysql-1.2.5.Final.jar libs/debezium-connector-mysql-1.2.5.Final.jar
COPY --from=build target/classes classes
COPY --from=build target/dependency libs

ENTRYPOINT ["java", "-cp", "/app/classes:/app/libs/*", "org.hazelcast.cache.ahead.Main"]
