FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/data

COPY hotel-node/target/hotel-node-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
EXPOSE 8082

CMD ["java", "-jar", "app.jar"]