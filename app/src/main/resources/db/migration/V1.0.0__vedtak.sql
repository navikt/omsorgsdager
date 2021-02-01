-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE vedtak
(
    id                          BIGSERIAL PRIMARY KEY,
    behandlingId                VARCHAR(50),
    saksnummer                  VARCHAR(50),
    mottatt                     TIMESTAMP WITH TIME ZONE NOT NULL,
    statusSistEndret            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    status                      VARCHAR(50),
    fom                         DATE NOT NULL,
    tom                         DATE NOT NULL,

    grunnlag                    JSONB NOT NULL,

    legeerklaering              JSONB,
    medlemskap                  JSONB,
    yrkesaktivitet              JSONB
);