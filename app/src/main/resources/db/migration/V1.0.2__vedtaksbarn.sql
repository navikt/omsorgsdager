CREATE TABLE vedtaksbarn
(
    id                          BIGSERIAL PRIMARY KEY,
    vedtak_id                   BIGINT NOT NULL,
    identitetsnummer            VARCHAR(25),
    fodselsdato                 DATE,
    CONSTRAINT foreign_key_vedtak FOREIGN KEY(vedtak_id) REFERENCES vedtak(id)
);

CREATE INDEX index_vedtaksbarn_vedtak_id ON vedtaksbarn(vedtak_id);