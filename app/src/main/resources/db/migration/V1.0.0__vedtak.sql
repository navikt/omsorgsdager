CREATE TABLE vedtak
(
    id                          BIGSERIAL PRIMARY KEY,
    k9_saksnummer               VARCHAR(50) NOT NULL,
    k9_behandling_id            VARCHAR(50) NOT NULL,
    type                        VARCHAR(50) NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    status_sist_endret          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    fom                         DATE,
    tom                         DATE,
    grunnlag                    JSONB NOT NULL,
    UNIQUE (k9_behandling_id)
);

CREATE INDEX index_vedtak_k9_saksnummer ON vedtak(k9_saksnummer);
CREATE INDEX index_vedtak_k9_behandling_id ON vedtak(k9_behandling_id);
