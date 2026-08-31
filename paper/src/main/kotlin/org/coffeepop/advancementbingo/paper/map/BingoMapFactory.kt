package org.coffeepop.advancementbingo.paper.map

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.coffeepop.advancementbingo.core.model.BingoCard

/**
 * Creates vanilla map items backed by [BingoMapRenderer].
 */
object BingoMapFactory {

    fun createMapItem(world: World, card: BingoCard, team: String): ItemStack {
        val mapView = Bukkit.createMap(world)
        mapView.isUnlimitedTracking = false
        mapView.renderers.forEach { mapView.removeRenderer(it) }
        mapView.addRenderer(BingoMapRenderer(card, team))

        val item = ItemStack(Material.FILLED_MAP)
        val meta = item.itemMeta as? MapMeta ?: return item
        meta.setMapView(mapView)
        item.itemMeta = meta
        return item
    }
}
