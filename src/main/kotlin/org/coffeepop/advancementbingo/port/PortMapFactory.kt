package org.coffeepop.advancementbingo.port

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta

/**
 * 创建原版地图物品并挂载 Bingo 卡渲染器。
 */
object PortMapFactory {

    fun createMapItem(world: World, card: PortBingoCard, team: String): ItemStack {
        val mapView = Bukkit.createMap(world)
        mapView.isUnlimitedTracking = false
        mapView.renderers.forEach { mapView.removeRenderer(it) }
        mapView.addRenderer(PortCardMapRenderer(card, team))

        val item = ItemStack(Material.FILLED_MAP)
        val meta = item.itemMeta as? MapMeta ?: return item
        meta.setMapView(mapView)
        item.itemMeta = meta
        return item
    }
}
