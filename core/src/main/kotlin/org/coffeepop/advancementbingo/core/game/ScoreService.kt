package org.coffeepop.advancementbingo.core.game

import org.coffeepop.advancementbingo.core.model.BingoCard
import org.coffeepop.advancementbingo.core.model.BingoObjective
import org.coffeepop.advancementbingo.core.model.GameMode

/**
 * Pure scoring logic shared by all game modes.
 */
object ScoreService {

    fun countObjectives(card: BingoCard, team: String): Int =
        card.countCompletedObjectives(team)

    fun countLines(card: BingoCard, team: String): Int =
        card.countCompletedLines(team)

    /**
     * Returns true when a team can still claim the objective in the given mode.
     */
    fun canClaim(mode: GameMode, objective: BingoObjective, team: String, enemyTeam: String): Boolean {
        if (mode == GameMode.LOCKOUT && objective.isAchievedBy(enemyTeam)) {
            return false
        }
        return !objective.isAchievedBy(team)
    }
}
