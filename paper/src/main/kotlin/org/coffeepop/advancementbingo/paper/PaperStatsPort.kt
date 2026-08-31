package org.coffeepop.advancementbingo.paper

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Statistic
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.coffeepop.advancementbingo.core.port.StatsPort
import java.util.UUID

/**
 * Paper implementation of [StatsPort].
 *
 * Supported stat types:
 * - "kill" / "mine" / "craft" / "use" with a namespaced id or entity name
 * - "move" with a statistic name such as "walk_one_cm"
 */
class PaperStatsPort : StatsPort {

    override fun value(playerId: String, statType: String, statName: String): Int {
        val player = player(playerId) ?: return 0

        return try {
            when (statType.lowercase()) {
                "kill" -> {
                    val entityType = entityType(statName) ?: return 0
                    player.getStatistic(Statistic.KILL_ENTITY, entityType)
                }
                "mine" -> {
                    val material = material(statName) ?: return 0
                    player.getStatistic(Statistic.MINE_BLOCK, material)
                }
                "craft" -> {
                    val material = material(statName) ?: return 0
                    player.getStatistic(Statistic.CRAFT_ITEM, material)
                }
                "use" -> {
                    val material = material(statName) ?: return 0
                    player.getStatistic(Statistic.USE_ITEM, material)
                }
                "move" -> player.getStatistic(Statistic.valueOf(statName.uppercase()))
                else -> 0
            }
        } catch (_: IllegalArgumentException) {
            0
        }
    }

    private fun material(name: String): org.bukkit.Material? {
        val key = NamespacedKey.fromString(name) ?: return null
        return Registry.MATERIAL.get(key)
    }

    private fun entityType(name: String): EntityType? =
        runCatching { EntityType.valueOf(name.uppercase()) }.getOrNull()

    private fun player(playerId: String): Player? =
        runCatching { Bukkit.getPlayer(UUID.fromString(playerId)) }.getOrNull()
}
