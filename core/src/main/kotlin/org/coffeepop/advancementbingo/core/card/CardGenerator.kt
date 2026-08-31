package org.coffeepop.advancementbingo.core.card

import org.coffeepop.advancementbingo.core.model.BingoCard
import org.coffeepop.advancementbingo.core.model.BingoCardEntry
import org.coffeepop.advancementbingo.core.model.BingoObjective
import kotlin.random.Random

/**
 * A candidate objective for card generation.
 *
 * [difficulty] is a platform-provided difficulty score.
 * [weight] controls how likely the candidate is to be picked inside its tier.
 */
data class ObjectiveCandidate(
    val objective: BingoObjective,
    val difficulty: Int,
    val weight: Int = defaultWeight(difficulty),
) {
    enum class Tier {
        EASY,
        MEDIUM,
        HARD,
    }

    val tier: Tier
        get() = when {
            difficulty <= 2 -> Tier.EASY
            difficulty <= 4 -> Tier.MEDIUM
            else -> Tier.HARD
        }

    companion object {
        fun defaultWeight(difficulty: Int): Int = when {
            difficulty <= 2 -> 100
            difficulty <= 4 -> 60
            else -> 30
        }
    }
}

/**
 * Generates a 5x5 Bingo card with a configurable easy/medium/hard ratio.
 */
class CardGenerator(
    private val random: Random = Random.Default,
) {

    fun generate(
        candidates: List<ObjectiveCandidate>,
        easyCount: Int,
        mediumCount: Int,
        hardCount: Int,
    ): BingoCard {
        val easy = candidates.filter { it.tier == ObjectiveCandidate.Tier.EASY }
        val medium = candidates.filter { it.tier == ObjectiveCandidate.Tier.MEDIUM }
        val hard = candidates.filter { it.tier == ObjectiveCandidate.Tier.HARD }

        var easyTarget = easyCount.coerceAtLeast(0)
        var mediumTarget = mediumCount.coerceAtLeast(0)
        var hardTarget = hardCount.coerceAtLeast(0)

        var overflow = easyTarget + mediumTarget + hardTarget - 25
        if (overflow > 0) {
            val reduceHard = minOf(hardTarget, overflow)
            hardTarget -= reduceHard
            overflow -= reduceHard

            val reduceMedium = minOf(mediumTarget, overflow)
            mediumTarget -= reduceMedium
            overflow -= reduceMedium

            val reduceEasy = minOf(easyTarget, overflow)
            easyTarget -= reduceEasy
        }

        val selected = mutableListOf<BingoObjective>()
        val usedIds = mutableSetOf<String>()

        addPicked(selected, usedIds, pickWeighted(easy, easyTarget))
        addPicked(selected, usedIds, pickWeighted(medium, mediumTarget))
        addPicked(selected, usedIds, pickWeighted(hard, hardTarget))

        if (selected.size < 25) {
            val rest = candidates.map { it.objective }
                .filter { it.id !in usedIds }
                .shuffled(random)
            for (objective in rest) {
                if (selected.size >= 25) break
                selected += objective
                usedIds += objective.id
            }
        }

        selected.shuffle(random)

        val entries = mutableListOf<BingoCardEntry>()
        val objectives = mutableMapOf<String, BingoObjective>()

        for (objective in selected.take(25)) {
            entries += BingoCardEntry(objectiveId = objective.id)
            objectives[objective.id] = objective
        }

        // Fill empty slots if the server does not provide enough objectives.
        var emptyIndex = 0
        while (entries.size < 25) {
            val id = "empty:$emptyIndex"
            emptyIndex++
            entries += BingoCardEntry(objectiveId = id)
            objectives[id] = BingoObjective.Advancement(
                id = id,
                advancementKey = "empty",
                displayName = "???",
            )
        }

        return BingoCard(entries, objectives)
    }

    private fun addPicked(
        target: MutableList<BingoObjective>,
        usedIds: MutableSet<String>,
        picked: List<BingoObjective>,
    ) {
        for (objective in picked) {
            if (target.size >= 25) break
            if (usedIds.add(objective.id)) {
                target += objective
            }
        }
    }

    private fun pickWeighted(pool: List<ObjectiveCandidate>, count: Int): List<BingoObjective> {
        val remaining = pool.toMutableList()
        val picked = mutableListOf<BingoObjective>()

        while (picked.size < count && remaining.isNotEmpty()) {
            val totalWeight = remaining.sumOf { it.weight }
            if (totalWeight <= 0) break

            var roll = random.nextInt(totalWeight)
            var chosen: ObjectiveCandidate? = null
            for (candidate in remaining) {
                roll -= candidate.weight
                if (roll < 0) {
                    chosen = candidate
                    break
                }
            }
            if (chosen == null) {
                chosen = remaining.last()
            }

            picked += chosen.objective
            remaining.remove(chosen)
        }

        return picked
    }
}
