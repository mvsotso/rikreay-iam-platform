FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY ../pom.xml ./pom.xml
COPY ../iam-common ./iam-common
COPY iam-governance-service ./iam-governance-service
RUN --mount=type=cache,target=/root/.m2 \
    cd /build && mvn -pl iam-common,iam-governance-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S iam && adduser -S iam -G iam
WORKDIR /app
COPY --from=builder /build/iam-governance-service/target/*.jar app.jar
RUN chown -R iam:iam /app
USER iam

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
