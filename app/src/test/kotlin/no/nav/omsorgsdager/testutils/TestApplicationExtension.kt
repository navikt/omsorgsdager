package no.nav.omsorgsdager.testutils

import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.omsorgsdager.testutils.MockedEnvironment.Companion.omsorgsdager
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.concurrent.TimeUnit

internal class TestApplicationExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {

        private val mockedEnvironment = MockedEnvironment().start()

        @KtorExperimentalAPI
        internal val testApplicationEngine = TestApplicationEngine(
            environment = createTestEnvironment {
                omsorgsdager(mockedEnvironment.applicationContext)
            }
        )

        init {
            testApplicationEngine.start(wait = true)
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    testApplicationEngine.stop(10, 60, TimeUnit.SECONDS)
                    mockedEnvironment.stop()
                }
            )
        }
    }

    private val støttedeParametre = listOf(
        TestApplicationEngine::class.java
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    @KtorExperimentalAPI
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return testApplicationEngine
    }
}