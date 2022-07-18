package no.nav.omsorgsdager.vedtak

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdager.*
import no.nav.omsorgsdager.Json.Companion.somJson
import no.nav.omsorgsdager.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class InnvilgedeVedtakApisTest(
    applicationContextBuilder: ApplicationContext.Builder
) : InnvilgedeVedtakKontrakt(applicationContextBuilder) {

    override fun hentInnvilgedeVedtak(jsonRequest: Json): Json {
        return withTestApplication({ omsorgsdager(applicationContext) }) {
            this.hentInnvilgedeVedtak(
                jsonRequest = jsonRequest
            ).second!!
        }
    }

    @Test
    fun `Ingen authorization header`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            val actual = hentInnvilgedeVedtak(
                jsonRequest = requestUtenVedtak(),
                authorizationHeader = null
            )
            assertEquals(HttpStatusCode.Unauthorized to null, actual)
        }
    }

    @Test
    fun `Ikke autorisert system`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            val actual = hentInnvilgedeVedtak(
                jsonRequest = requestUtenVedtak(),
                authorizationHeader = Azure.V2_0.generateJwt(
                    clientId = "k9-sak",
                    audience = "omsorgsdager",
                    accessAsApplication = false
                ).let { "Bearer $it" }
            )
            assertEquals(HttpStatusCode.Forbidden to ForventetResponseForbidden, actual)
        }
    }

    @Test
    fun `Ugyldig request`() {
        withTestApplication({ omsorgsdager(applicationContext) }) {
            val actual = hentInnvilgedeVedtak(
                jsonRequest = InnvilgedeVedtakRequest(
                    fom = "fom",
                    tom = "tom",
                    identitetsnummer = "identitetsnummer"
                ).jsonRequest
            )
            assertEquals(HttpStatusCode.BadRequest to null, actual)
        }
    }

    private companion object {
        val ForventetResponseForbidden =
            """{"detail":"Requesten inneholder ikke tilstrekkelige tilganger.","instance":"about:blank","type":"/problem-details/unauthorized","title":"unauthorized","status":403}""".somJson()

        fun TestApplicationEngine.hentInnvilgedeVedtak(
            jsonRequest: Json,
            authorizationHeader: String? = Azure.V2_0.generateJwt(
                clientId = "k9-aarskvantum",
                audience = "omsorgsdager",
                accessAsApplication = true
            ).let { "Bearer $it" }
        ): Pair<HttpStatusCode, Json?> {
            return with(this) {
                handleRequest(HttpMethod.Post, "/api/innvilgede-vedtak-utvidet-rett") {
                    authorizationHeader?.let {
                        addHeader(HttpHeaders.Authorization, authorizationHeader)
                    }
                    addHeader(HttpHeaders.XCorrelationId, "${CorrelationId.genererCorrelationId()}")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    setBody(jsonRequest.raw)
                }.let {
                    it.response.status()!! to it.response.content?.somJson()
                }
            }
        }
    }
}