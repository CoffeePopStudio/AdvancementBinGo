package org.coffeepop.advancementbingo.port

import org.bukkit.entity.Player

/**
 * 对应原项目 AdvancementObjectiveManager 的 Paper 简化实现。
 */
object PortAdvancementObjectiveManager {

    fun tick(card: PortBingoCard, playersByTeam: Map<String, List<Player>>) {
        for (objective in card.objectives.values) {
            if (objective !is PortBingoObjective.Advancement) continue

            for ((team, players) in playersByTeam) {
                if (players.any { PortAdvancementManager.isDone(it, objective.advancementKey) }) {
                    objective.markAchieved(team)
                }
            }
        }
    }
}
