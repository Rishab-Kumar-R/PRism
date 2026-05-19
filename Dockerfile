FROM eclipse-temurin:21-jre-jammy

WORKDIR /work

COPY --chown=1001:1001 build/quarkus-app/lib/ /work/lib/
COPY --chown=1001:1001 build/quarkus-app/*.jar /work/
COPY --chown=1001:1001 build/quarkus-app/app/ /work/app/
COPY --chown=1001:1001 build/quarkus-app/quarkus/ /work/quarkus/

EXPOSE 8080

USER 1001

ENTRYPOINT ["java", "-Dquarkus.profile=prod", "-jar", "/work/quarkus-run.jar"]
