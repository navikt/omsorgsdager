-- To behandlinger i preprod som hadde fått 9999-12-31 som tom
-- Oppdateres til å gjelde ut året barna fyller 18 år
UPDATE behandling SET tom='2037-12-31' WHERE behovssekvens_id='01F0E4NWHACNNS1DA2WVEEFT7H';
UPDATE behandling SET tom='2027-12-31' WHERE behovssekvens_id='01F0E4WV80EW7FCHCZ36NCD7CW';