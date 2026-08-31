package org.coffeepop.advancementbingo.core.game

import org.coffeepop.advancementbingo.core.model.BingoCard
import org.coffeepop.advancementbingo.core.model.BingoObjective
import org.coffeepop.advancementbingo.core.port.AdvancementPort
import org.coffeepop.advancementbingo.core.port.ItemPort
import org.coffeepop.advancementbingo.core.port.StatsPort

/**
 * Updates objective completion state using platform ports.
 *
 * This class contains no Bukkit/Paper code and can be unit tested with fakes.
 */
class ObjectiveTracker(
    private val advancementPort: AdvancementPort,
    private val itemPort: ItemPort,
    private val statsPort: StatsPort,
) {

    /**
     * @param playersByTeam maps a team key to the platform player IDs in that team.
     */
    fun tick(card: BingoCard, playersByTeam: Map<String, List<String>>) {
        for (objective in card.objectives.values) {
            for ((team, playerIds) in playersByTeam) {
                if (objective.isAchievedBy(team)) continue

                val completed = when (objective) {
                    is BingoObjective.Advancement ->
                        playerIds.any { advancementPort.isDone(it, objective.advancementKey) }

                    is BingoObjective.Item -> {
                        val total = playerIds.sumOf { itemPort.count(it, objective.itemKey) }
                        total >= objective.requiredCount
                    }

                    is BingoObjective.Stats ->
                        playerIds.any { statsPort.value(it, objective.statType, objective.statName) >= objective.min }
                }

                if (completed) {
                    objective.markAchieved(team)
                }
            }
        }
    }
}
