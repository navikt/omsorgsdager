CREATE TABLE parter
(
    id                          BIGSERIAL PRIMARY KEY,
    vedtak_id                   BIGINT NOT NULL,
    identitetsnummer            VARCHAR(25),
    fodselsdato                 DATE,
    type                        VARCHAR(50),
    omsorgspenger_saksnummer    VARCHAR(50),
    CONSTRAINT foreign_key_vedtak FOREIGN KEY(vedtak_id) REFERENCES vedtak(id)
);

CREATE INDEX index_parter_vedtak_id ON parter(vedtak_id);
CREATE INDEX index_parter_omsorgsdager_saksnummer ON parter(omsorgspenger_saksnummer);
