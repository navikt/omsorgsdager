package no.nav.omsorgsdager.testutils

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.omsorgsdager.testutils.wiremock.*
import java.io.File
import java.nio.file.Files

internal class MockedEnvironment {
    private fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
        .setOverrideWorkingDirectory(tempDir)
        .setDataDirectory(tempDir.resolve("datadir"))
        .start()

    internal val embeddedPostgres = embeddedPostgress(Files.createTempDirectory("tmp_postgres").toFile())

    internal val wireMockServer = WireMockBuilder()
        .withAzureSupport()
        .withNaisStsSupport()
        .build()
        .stubTilgangApi()
        .stubInfotrygdRammevedtak()
        .stubOmsorgspengerSakApi()

    internal fun start() = this
    internal fun stop() {
        embeddedPostgres.close()
        wireMockServer.stop()
    }
}