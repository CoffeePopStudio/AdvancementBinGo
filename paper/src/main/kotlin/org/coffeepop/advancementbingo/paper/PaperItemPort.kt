package org.coffeepop.advancementbingo.paper

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.coffeepop.advancementbingo.core.port.ItemPort
import java.util.UUID

/**
 * Paper implementation of [ItemPort].
 *
 * The item key uses the vanilla namespaced id, e.g. "minecraft:iron_ingot".
 */
class PaperItemPort : ItemPort {

    override fun count(playerId: String, itemKey: String): Int {
        val player = player(playerId) ?: return 0
        val material = material(itemKey) ?: return 0
        return player.inventory.all(material).values.sumOf { it.amount }
    }

    private fun material(itemKey: String): Material? {
        val key = NamespacedKey.fromString(itemKey) ?: return null
        return Registry.MATERIAL.get(key)
    }

    private fun player(playerId: String): Player? =
        runCatching { Bukkit.getPlayer(UUID.fromString(playerId)) }.getOrNull()
}
