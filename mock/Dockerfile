FROM wiremock/wiremock:2.32.0
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgsdager/mock
COPY mappings/ /home/wiremock/mappings/
COPY __files/ /home/wiremock/__files/
ENTRYPOINT ["/docker-entrypoint.sh", "--local-response-templating"]
