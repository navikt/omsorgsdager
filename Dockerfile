FROM navikt/java:15
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgsdager
COPY app/build/libs/app.jar app.jar
