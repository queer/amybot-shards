FROM openjdk:9-jre

ENTRYPOINT ["/usr/bin/java", "-Xmx1G", "-jar", "/usr/share/amybot/shard.jar"]

COPY target/discord-*.jar /usr/share/amybot/shard.jar
