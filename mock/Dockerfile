FROM wiremock/wiremock:3.2.0-alpine
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgsdager/mock

COPY mappings/ /home/wiremock/mappings/
COPY __files/ /home/wiremock/__files/
ENTRYPOINT ["/docker-entrypoint.sh", "--local-response-templating"]
