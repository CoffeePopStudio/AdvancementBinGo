package org.coffeepop.advancementbingo.paper.map

import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import org.coffeepop.advancementbingo.core.model.BingoCard
import java.awt.Color

/**
 * Renders a 5x5 Bingo card on a vanilla map.
 *
 * The map intentionally shows colors and numbers only, because vanilla map
 * fonts cannot render Chinese/Unicode names reliably. Names are available in
 * the GUI/chat instead.
 */
class BingoMapRenderer(
    private val card: BingoCard,
    private val team: String,
) : MapRenderer() {

    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
        val background = Color(20, 20, 24)
        for (x in 0 until 128) {
            for (y in 0 until 128) {
                canvas.setPixelColor(x, y, background)
            }
        }

        val cellSize = 24
        val offset = 4

        for (index in 0 until 25) {
            val column = index % 5
            val row = index / 5
            val startX = offset + column * cellSize
            val startY = offset + row * cellSize

            val objective = card.objectives.values.elementAtOrNull(index)
            val color = if (objective?.isAchievedBy(team) == true) {
                Color(60, 180, 80)
            } else {
                Color(70, 70, 80)
            }

            for (dx in 0 until cellSize) {
                for (dy in 0 until cellSize) {
                    canvas.setPixelColor(startX + dx, startY + dy, color)
                }
            }

            val text = (index + 1).toString()
            val textX = startX + (cellSize - MinecraftFont.Font.getWidth(text)) / 2
            val textY = startY + (cellSize - MinecraftFont.Font.getHeight()) / 2
            canvas.drawText(textX, textY, MinecraftFont.Font, text)
        }

        val lineColor = Color(220, 220, 220)
        for (i in 0..5) {
            val pos = offset + i * cellSize
            for (j in 0 until 128) {
                canvas.setPixelColor(pos, j, lineColor)
                canvas.setPixelColor(j, pos, lineColor)
            }
        }
    }
}
