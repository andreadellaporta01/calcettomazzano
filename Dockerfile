# Stage 1: Build
FROM gradle:8.5.0-jdk17 AS build
WORKDIR /home/app
COPY . .
RUN gradle installDist

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /home/app/build/install/calcettomazzano/ /app/
ENV PORT=8080
EXPOSE 8080
CMD ["./bin/calcettomazzano"]