package org.coffeepop.advancementbingo.core.model

import java.time.Instant

/**
 * Represents a single objective on a Bingo card.
 *
 * The core module is platform-agnostic, so objectives only store logical
 * identifiers and display names. Platform modules are responsible for
 * resolving those identifiers into real Minecraft/Bukkit objects.
 */
sealed class BingoObjective {

    abstract val id: String
    abstract val displayName: String

    private val teamsAchieved = mutableMapOf<String, Instant>()

    fun isAchievedBy(team: String): Boolean = teamsAchieved.containsKey(team)

    fun markAchieved(team: String) {
        teamsAchieved.putIfAbsent(team, Instant.now())
    }

    fun achievedAt(team: String): Instant? = teamsAchieved[team]

    /**
     * An advancement-based objective, e.g. "minecraft:story/mine_stone".
     */
    class Advancement(
        override val id: String,
        val advancementKey: String,
        override val displayName: String,
    ) : BingoObjective()

    /**
     * An item-based objective, e.g. hold 16 iron ingots in the inventory.
     */
    class Item(
        override val id: String,
        val itemKey: String,
        val requiredCount: Int,
        override val displayName: String,
    ) : BingoObjective()

    /**
     * A stats-based objective, e.g. kill 10 zombies or mine 64 stone.
     */
    class Stats(
        override val id: String,
        val statType: String,
        val statName: String,
        val min: Int,
        val max: Int,
        override val displayName: String,
    ) : BingoObjective()
}
