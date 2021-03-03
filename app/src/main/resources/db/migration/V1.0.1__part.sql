CREATE TABLE part
(
    id                          BIGSERIAL PRIMARY KEY,
    behandling_id               BIGINT NOT NULL,
    identitetsnummer            VARCHAR(25) NOT NULL,
    aktor_id                    VARCHAR(50) NOT NULL,
    type                        VARCHAR(50) NOT NULL,
    omsorgspenger_saksnummer    VARCHAR(50) NOT NULL,
    fodselsdato                 DATE,
    CONSTRAINT foreign_key_behandling FOREIGN KEY(behandling_id) REFERENCES behandling(id)
);

CREATE INDEX index_part_behandling_id ON part(behandling_id);
CREATE INDEX index_part_omsorgspenger_saksnummer ON part(omsorgspenger_saksnummer);
CREATE INDEX index_part_identitetsnummer ON part(identitetsnummer);

