package de.anura.bot.database

import de.anura.bot.config.SqlConfig
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.useHandleUnchecked

object Database {

    private lateinit var jdbi: Jdbi

    fun connect(config: SqlConfig) {
        jdbi = Jdbi.create(
                "jdbc:mysql://${config.host}:${config.port}/${config.database}" +
                        "?useLegacyDatetimeCode=false&serverTimezone=UTC",
                config.user, config.password
        )
        jdbi.installPlugin(KotlinPlugin())
        createTables()
    }

    private fun createTables() {
        jdbi.useHandleUnchecked { handle ->
            // Table for the steam games with a icon
            handle.execute("CREATE TABLE IF NOT EXISTS `steam_game` ( " +
                    "  `id` int(11) NOT NULL COMMENT 'Steam'\'s App ID', " +
                    "  `icon_id` int(11) DEFAULT NULL COMMENT 'Teamspeak''s Icon ID', " +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1")

            // Table for the users
            handle.execute("CREATE TABLE IF NOT EXISTS `ts_user` ( " +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT, " +
                    "  `uid` varchar(50) DEFAULT NULL COMMENT 'TeamSpeak Unique ID', " +
                    "  `time` int(11) DEFAULT NULL, " +
                    "  `steam_id` varchar(100) DEFAULT NULL, " +
                    "  PRIMARY KEY (`id`) " +
                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1")
        }
    }

    fun get(): Jdbi {
        return jdbi
    }

}