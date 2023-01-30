package no.nav.omsorgsdager.tilgangsstyring

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.*
import io.ktor.http.*

internal data class Token(
    internal val jwt: String,
    internal val clientId: String,
    internal val erPersonToken: Boolean,
    internal val harTilgangSomSystem: Boolean
)

internal class TokenResolver(
    private val azureIssuers: Set<String>
) {

    internal fun resolve(call: ApplicationCall): Token {
        val token = call.token() ?: throw IllegalArgumentException("Finner ikke token i request.")
        val decodedJwt = JWT.decode(token)
        return when (decodedJwt.issuer) {
            in azureIssuers -> decodedJwt.tokenFraAzure()
            else -> throw IllegalStateException("Støtter ikke issuer '${decodedJwt.issuer}'")
        }
    }

    private fun DecodedJWT.tokenFraAzure() = Token(
        jwt = this.token,
        clientId = claims["azp"]?.asString() ?: throw IllegalStateException("Mangler 'azp' claim"),
        erPersonToken = claims.containsKey("oid") && claims.containsKey("preferred_username"),
        harTilgangSomSystem = (claims["roles"]?.asArray(String::class.java)
            ?: emptyArray<String>()).contains("access_as_application")
    )

    internal companion object {
        internal fun ApplicationCall.token() = when {
            request.headers.contains(HttpHeaders.Authorization) -> request.headers[HttpHeaders.Authorization]!!.substringAfter(
                " "
            )
            request.cookies["ID_token"] != null -> request.cookies["ID_token"]
            else -> null
        }
    }
}