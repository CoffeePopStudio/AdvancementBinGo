package org.coffeepop.advancementbingo.core.port

/**
 * Platform abstraction for advancement progress.
 */
interface AdvancementPort {

    fun isDone(playerId: String, advancementKey: String): Boolean

    fun progress(playerId: String, advancementKey: String): Float

    fun clear(playerId: String)
}

/**
 * Platform abstraction for item counts in a player inventory.
 */
interface ItemPort {

    fun count(playerId: String, itemKey: String): Int
}

/**
 * Platform abstraction for Minecraft statistics.
 */
interface StatsPort {

    fun value(playerId: String, statType: String, statName: String): Int
}
