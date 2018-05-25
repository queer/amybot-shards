FROM maven:3
COPY . /app
WORKDIR /app
RUN mvn clean package
RUN mkdir -pv /usr/share/amybot
RUN cp target/discord-*.jar /usr/share/amybot/shard.jar

FROM openjdk:8-jre
ENTRYPOINT ["/usr/bin/java", "-Xmx1G", "-jar", "/usr/share/amybot/shard.jar"]
