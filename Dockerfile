FROM openjdk:8-jre

ENTRYPOINT ["/usr/bin/java", "-Xmx1G", "-jar", "/usr/share/amybot/shard.jar"]

ADD target/discord-*.jar /usr/share/amybot/shard.jar
