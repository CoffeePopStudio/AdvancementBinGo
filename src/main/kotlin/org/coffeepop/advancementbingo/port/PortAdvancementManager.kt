package org.coffeepop.advancementbingo.port

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.advancement.Advancement
import org.bukkit.entity.Player

/**
 * 基于 Bukkit/Paper API 的成就管理器，对应原项目 IAdvancementManager。
 */
object PortAdvancementManager {

    fun listAdvancements(): List<NamespacedKey> {
        val result = mutableListOf<NamespacedKey>()
        val iterator = Bukkit.advancementIterator()
        while (iterator.hasNext()) {
            val advancement = iterator.next()
            if (advancement.key.key.startsWith("recipes/")) continue
            result += advancement.key
        }
        return result
    }

    fun getAdvancement(key: NamespacedKey): Advancement? = Bukkit.getAdvancement(key)

    fun getProgress(player: Player, key: NamespacedKey): Float {
        val advancement = getAdvancement(key) ?: return 0f
        val progress = player.getAdvancementProgress(advancement)
        if (progress.isDone) return 1f
        val awarded = progress.awardedCriteria.size
        val remaining = progress.remainingCriteria.size
        val total = awarded + remaining
        return if (total == 0) 0f else awarded.toFloat() / total.toFloat()
    }

    fun isDone(player: Player, key: NamespacedKey): Boolean {
        val advancement = getAdvancement(key) ?: return false
        return player.getAdvancementProgress(advancement).isDone
    }

    fun clearAdvancements(player: Player) {
        val iterator = Bukkit.advancementIterator()
        while (iterator.hasNext()) {
            val advancement = iterator.next()
            val progress = player.getAdvancementProgress(advancement)
            for (criteria in progress.awardedCriteria) {
                progress.revokeCriteria(criteria)
            }
        }
    }
}
