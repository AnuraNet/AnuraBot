package de.anura.bot.database

import de.anura.bot.config.AppConfig
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.useHandleUnchecked

object Database {

    private val config = AppConfig.mysql
    private lateinit var jdbi: Jdbi

    fun connect() {
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

            // Table for groups with a time requirement
            handle.execute("CREATE TABLE IF NOT EXISTS `time_group` ( " +
                    "  `ts_group` int(11) NOT NULL, " +
                    "  `required_time` int(11) DEFAULT NULL COMMENT 'Time in seconds', " +
                    "  PRIMARY KEY (`ts_group`) " +
                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1")

            // Table for the users
            handle.execute("CREATE TABLE IF NOT EXISTS `ts_user` ( " +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT, " +
                    "  `uid` varchar(50) DEFAULT NULL COMMENT 'TeamSpeak Unique ID', " +
                    "  `time` int(11) DEFAULT NULL, " +
                    "  `steam_id` varchar(100) DEFAULT NULL, " +
                    "  `permission` int(2) DEFAULT '0', " +
                    "  PRIMARY KEY (`id`) " +
                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1")

            // Table for the selected games of the user
            handle.execute("CREATE TABLE IF NOT EXISTS `selected_game` ( " +
                    "  `user_id` int(11) NOT NULL, " +
                    "  `game_id` int(11) NOT NULL COMMENT 'The Steam App Id', " +
                    "  PRIMARY KEY (`user_id`,`game_id`), " +
                    "  CONSTRAINT `selected_game_ts_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `ts_user` (`id`) " +
                    "    ON DELETE CASCADE ON UPDATE CASCADE " +
                    ") ENGINE=InnoDB DEFAULT CHARSET=latin1")
        }
    }

    fun get(): Jdbi {
        return jdbi
    }

}