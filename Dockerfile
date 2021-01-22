FROM navikt/java:14
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgsdager
COPY build/libs/*.jar app.jar
