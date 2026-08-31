package org.coffeepop.advancementbingo.paper

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.coffeepop.advancementbingo.core.port.AdvancementPort
import java.util.UUID

/**
 * Paper implementation of [AdvancementPort].
 */
class PaperAdvancementPort : AdvancementPort {

    override fun isDone(playerId: String, advancementKey: String): Boolean {
        val player = player(playerId) ?: return false
        val advancement = Bukkit.getAdvancement(NamespacedKey.fromString(advancementKey) ?: return false) ?: return false
        return player.getAdvancementProgress(advancement).isDone
    }

    override fun progress(playerId: String, advancementKey: String): Float {
        val player = player(playerId) ?: return 0f
        val advancement = Bukkit.getAdvancement(NamespacedKey.fromString(advancementKey) ?: return 0f) ?: return 0f
        val progress = player.getAdvancementProgress(advancement)
        if (progress.isDone) return 1f

        val awarded = progress.awardedCriteria.size
        val total = awarded + progress.remainingCriteria.size
        return if (total == 0) 0f else awarded.toFloat() / total.toFloat()
    }

    override fun clear(playerId: String) {
        val player = player(playerId) ?: return
        val iterator = Bukkit.advancementIterator()
        while (iterator.hasNext()) {
            val advancement = iterator.next()
            val progress = player.getAdvancementProgress(advancement)
            for (criteria in progress.awardedCriteria) {
                progress.revokeCriteria(criteria)
            }
        }
    }

    private fun player(playerId: String): Player? =
        runCatching { Bukkit.getPlayer(UUID.fromString(playerId)) }.getOrNull()
}
