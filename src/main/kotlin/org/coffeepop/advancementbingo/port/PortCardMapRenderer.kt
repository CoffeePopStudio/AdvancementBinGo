package org.coffeepop.advancementbingo.port

import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import java.awt.Color

/**
 * 尝试用原版地图渲染 5x5 Bingo 卡。
 * 如果效果不好，可以回退到 GUI。
 */
class PortCardMapRenderer(
    private val card: PortBingoCard,
    private val team: String,
) : MapRenderer() {

    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
        val background = Color(20, 20, 24)
        for (x in 0 until 128) {
            for (y in 0 until 128) {
                canvas.setPixelColor(x, y, background)
            }
        }

        val cell = 24
        val offset = 4
        for (index in 0 until 25) {
            val col = index % 5
            val row = index / 5
            val x = offset + col * cell
            val y = offset + row * cell

            val objective = card.objectives.values.elementAtOrNull(index)
            val color = when {
                objective?.hasTeamAchieved(team) == true -> Color(60, 180, 80)
                else -> Color(70, 70, 80)
            }

            for (dx in 0 until cell) {
                for (dy in 0 until cell) {
                    canvas.setPixelColor(x + dx, y + dy, color)
                }
            }

            val text = (index + 1).toString()
            val textX = x + (cell - MinecraftFont.Font.getWidth(text)) / 2
            val textY = y + (cell - MinecraftFont.Font.getHeight()) / 2
            canvas.drawText(textX, textY, MinecraftFont.Font, text)
        }

        // 画网格线
        val lineColor = Color(220, 220, 220)
        for (i in 0..5) {
            val pos = offset + i * cell
            for (j in 0 until 128) {
                canvas.setPixelColor(pos, j, lineColor)
                canvas.setPixelColor(j, pos, lineColor)
            }
        }
    }
}
