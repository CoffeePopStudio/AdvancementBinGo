package org.coffeepop.advancementbingo.storage

import java.sql.Connection
import java.sql.DriverManager

/**
 * SQLite-backed statistics repository.
 */
class StatsRepository(
    private val jdbcUrl: String = "jdbc:sqlite:advancementbingo.db",
) {
    private val connection: Connection by lazy {
        DriverManager.getConnection(jdbcUrl).apply {
            createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS player_stats (
                        player_id TEXT PRIMARY KEY,
                        games INTEGER NOT NULL DEFAULT 0,
                        wins INTEGER NOT NULL DEFAULT 0,
                        objectives_completed INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }
    }

    data class PlayerStats(
        val playerId: String,
        val games: Int,
        val wins: Int,
        val objectivesCompleted: Int,
    )

    fun recordGame(playerId: String, won: Boolean, objectivesCompleted: Int) {
        connection.prepareStatement(
            """
            INSERT INTO player_stats (player_id, games, wins, objectives_completed)
            VALUES (?, 1, ?, ?)
            ON CONFLICT(player_id) DO UPDATE SET
                games = games + 1,
                wins = wins + excluded.wins,
                objectives_completed = objectives_completed + excluded.objectives_completed
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, playerId)
            statement.setInt(2, if (won) 1 else 0)
            statement.setInt(3, objectivesCompleted)
            statement.executeUpdate()
        }
    }

    fun getStats(playerId: String): PlayerStats? {
        connection.prepareStatement(
            "SELECT player_id, games, wins, objectives_completed FROM player_stats WHERE player_id = ?"
        ).use { statement ->
            statement.setString(1, playerId)
            statement.executeQuery().use { result ->
                if (!result.next()) return null
                return PlayerStats(
                    playerId = result.getString("player_id"),
                    games = result.getInt("games"),
                    wins = result.getInt("wins"),
                    objectivesCompleted = result.getInt("objectives_completed"),
                )
            }
        }
    }
}
