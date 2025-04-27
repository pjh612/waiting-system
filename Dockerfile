FROM azul/zulu-openjdk:17
EXPOSE 8080
AR  G JAR_FILE
COPY ${JAR_FILE} app.jar
#"-Djava.security.egd=file:/dev/./urandom",
#"-Dspring.profiles.active=prod",
ENTRYPOINT ["java", "-jar", "/app.jar"]