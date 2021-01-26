CREATE TABLE omsorgsdager
(
    id                          BIGSERIAL PRIMARY KEY,
    fom                         DATE NOT NULL,
    tom                         DATE NOT NULL,
    fra                         VARCHAR(50) NOT NULL,
    til                         VARCHAR(50) NOT NULL,
);

CREATE INDEX index_overforing_fra ON overforing(fra);
CREATE INDEX index_overforing_til ON overforing(til);