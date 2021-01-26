package no.nav.omsorgsdager

import javax.sql.DataSource

internal class ApplicationContext(
    internal val env: Environment,
    internal val dataSource: DataSource,
) {

    internal fun start() {
        dataSource.migrate()
    }

    internal fun stop() {}

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
    ) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
            )
        }
    }
}
