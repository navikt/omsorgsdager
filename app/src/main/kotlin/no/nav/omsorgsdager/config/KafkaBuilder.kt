package no.nav.omsorgsdager.config

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.net.InetAddress
import java.util.*

object KafkaBuilder {

    fun Environment.kafkaProducer() : KafkaProducer<String, String> {
        val producerConfig = kafkaBaseConfig(
            bootstrapServers = hentRequiredEnv("KAFKA_BOOTSTRAP_SERVERS"),
            credentials = username()?.let { username ->
                username to password()
            },
            truststore = hentOptionalEnv("NAV_TRUSTSTORE_PATH")?.let { truststore ->
                truststore to hentRequiredEnv("NAV_TRUSTSTORE_PASSWORD")
            }
        ).withProducerConfig(clientId = generateClientId())

        return KafkaProducer(
            producerConfig,
            StringSerializer(),
            StringSerializer()
        )
    }

    private val logger = LoggerFactory.getLogger(KafkaBuilder::class.java)

    private fun kafkaBaseConfig(
        bootstrapServers: String,
        credentials: Pair<String, String>?,
        truststore: Pair<String, String>?
    ) = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")

        credentials?.also { (username,password) ->
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
            )
        }

        truststore?.also { (truststore, truststorePassword) ->
            try {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(truststore).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
                logger.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
            } catch (ex: Exception) {
                logger.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
            }
        }
    }

    private fun Properties.withProducerConfig(clientId: String) : Properties {
        put(ProducerConfig.CLIENT_ID_CONFIG, "producer-$clientId")
        put(ProducerConfig.ACKS_CONFIG, "1")
        put(ProducerConfig.LINGER_MS_CONFIG, "0")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        return this
    }

    private fun Environment.generateClientId(): String {
        if (harEnv("NAIS_APP_NAME")) return InetAddress.getLocalHost().hostName
        return UUID.randomUUID().toString()
    }

    private fun username() = "/var/run/secrets/nais.io/service_user/username".readFile()
    private fun password() = requireNotNull("/var/run/secrets/nais.io/service_user/password".readFile()) {
        "Mangler passord på path '/var/run/secrets/nais.io/service_user/password'"
    }

    private fun String.readFile() =
        try {
            File(this).readText(Charsets.UTF_8)
        } catch (err: FileNotFoundException) {
            null
        }
}
