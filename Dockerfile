FROM navikt/java:14
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgsdager
COPY app/build/libs/*.jar app.jar
