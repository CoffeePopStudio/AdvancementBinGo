package org.coffeepop.advancementbingo.port

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import java.time.Instant

/**
 * 从 bingo-main 移植的 Bingo 目标核心（先只包含成就目标）。
 */
sealed class PortBingoObjective {

    abstract val id: String
    abstract val name: Component
    abstract val icon: ItemStack?

    private val teamsAchieved = mutableMapOf<String, Instant>()

    fun hasTeamAchieved(team: String): Boolean = teamsAchieved.containsKey(team)

    fun markAchieved(team: String) {
        teamsAchieved.putIfAbsent(team, Instant.now())
    }

    fun achievedAt(team: String): Instant? = teamsAchieved[team]

    /**
     * 成就目标
     */
    class Advancement(
        override val id: String,
        val advancementKey: org.bukkit.NamespacedKey,
        override val name: Component,
        override val icon: ItemStack?,
    ) : PortBingoObjective()
}
