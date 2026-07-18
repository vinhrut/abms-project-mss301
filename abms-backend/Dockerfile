FROM maven:3.9.9-eclipse-temurin-17 AS build

ARG MODULE
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY ${MODULE}/pom.xml ./${MODULE}/pom.xml
COPY ${MODULE}/src ./${MODULE}/src

RUN mvn -f ${MODULE}/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

ARG MODULE
ARG PORT
WORKDIR /app

COPY --from=build /workspace/${MODULE}/target/${MODULE}.jar app.jar

EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
