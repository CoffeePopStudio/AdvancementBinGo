package org.coffeepop.advancementbingo.port

import net.kyori.adventure.text.format.NamedTextColor

/**
 * 从 bingo-main 移植的队伍模型。
 */
data class PortTeam(
    val key: String,
    val displayName: String,
    val color: NamedTextColor,
) {
    companion object {
        val RED = PortTeam("red", "红队", NamedTextColor.RED)
        val BLUE = PortTeam("blue", "蓝队", NamedTextColor.BLUE)
    }
}
