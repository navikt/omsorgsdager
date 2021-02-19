CREATE TABLE parter
(
    id                          BIGSERIAL PRIMARY KEY,
    behandling_id               BIGINT NOT NULL,
    identitetsnummer            VARCHAR(25),
    fodselsdato                 DATE,
    type                        VARCHAR(50),
    omsorgspenger_saksnummer    VARCHAR(50),
    CONSTRAINT foreign_key_behandling FOREIGN KEY(behandling_id) REFERENCES behandling(id)
);

CREATE INDEX index_parter_behandling_id ON parter(behandling_id);
CREATE INDEX index_parter_omsorgspenger_saksnummer ON parter(omsorgspenger_saksnummer);
