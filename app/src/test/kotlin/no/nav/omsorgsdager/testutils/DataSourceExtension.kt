package no.nav.omsorgsdager.testutils

import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal fun DataSource.cleanAndMigrate() {
    Flyway
        .configure()
        .dataSource(this)
        .load()
        .also {
            it.clean()
            it.migrate()
        }
}