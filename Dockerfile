FROM openjdk:21
WORKDIR /app
COPY target/house-sharing-backend-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]