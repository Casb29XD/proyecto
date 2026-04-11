FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace/app

# Copy maven wrapper and pom file
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy source code
COPY src src

# Build the application
RUN ./mvnw install -DskipTests

# Run stage - using Debian-based image for full DNS SRV support (required for mongodb+srv://)
FROM eclipse-temurin:17-jre
WORKDIR /data
COPY --from=build /workspace/app/target/proyecto-0.0.1-SNAPSHOT.jar /app.jar

# Run the app
ENTRYPOINT ["java","-jar","/app.jar"]
