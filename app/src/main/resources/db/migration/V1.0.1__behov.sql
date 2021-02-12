CREATE TABLE behov
(
    id                          BIGSERIAL PRIMARY KEY,
    vedtak_id                   BIGINT NOT NULL,
    navn                        VARCHAR(100) NOT NULL,
    status                      VARCHAR(50) NOT NULL DEFAULT 'ULØST',
    versjon                     SMALLINT,
    losning                     JSONB,
    lovanvendelser              JSONB,
    CONSTRAINT foreign_key_vedtak FOREIGN KEY(vedtak_id) REFERENCES vedtak(id),
    UNIQUE (vedtak_id, navn)
);

CREATE INDEX index_behov_vedtak_id ON behov(vedtak_id);