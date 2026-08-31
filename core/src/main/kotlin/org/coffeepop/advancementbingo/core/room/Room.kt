package org.coffeepop.advancementbingo.core.room

/**
 * Game mode configured for a single room.
 */
enum class RoomMode {
    /** Advancement-only Bingo. */
    ADVANCEMENT,

    /** Item-based Bingo. */
    ITEM,

    /** Stats-based Bingo. */
    STATS,
}

/**
 * Immutable room configuration.
 */
data class RoomConfig(
    val id: String,
    val worldName: String,
    val mode: RoomMode,
    val requiredPlayers: Int,
    val countdownSeconds: Int,
)
