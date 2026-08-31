package org.coffeepop.advancementbingo.paper

import io.papermc.paper.advancement.AdvancementDisplay.Frame
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.advancement.Advancement
import org.coffeepop.advancementbingo.core.card.ObjectiveCandidate
import org.coffeepop.advancementbingo.core.model.BingoObjective

/**
 * Builds [ObjectiveCandidate] objects from Bukkit advancements.
 */
class PaperObjectiveResolver {

    private val extremeHardKeys = setOf(
        "nether/all_effects",
        "nether/all_potions",
        "adventure/arbalistic",
        "adventure/very_very_frightening",
        "adventure/sniper_duel",
        "adventure/two_birds_one_arrow",
        "adventure/whos_the_pillager_now",
        "nether/obtain_ancient_debris",
        "end/levitate",
        "adventure/kill_all_mobs",
        "husbandry/complete_catalogue",
        "husbandry/balanced_diet",
        "husbandry/obtain_all_music_discs",
    )

    fun advancementCandidates(): List<ObjectiveCandidate> {
        val result = mutableListOf<ObjectiveCandidate>()
        val iterator = Bukkit.advancementIterator()

        while (iterator.hasNext()) {
            val advancement = iterator.next()
            val display = advancement.display ?: continue
            val key = advancement.key
            if (key.key.startsWith("recipes/")) continue

            val difficulty = difficulty(advancement)
            val weight = if (key.key in extremeHardKeys) 5 else ObjectiveCandidate.defaultWeight(difficulty)

            result += ObjectiveCandidate(
                objective = BingoObjective.Advancement(
                    id = key.toString(),
                    advancementKey = key.toString(),
                    displayName = display.title().toString(),
                ),
                difficulty = difficulty,
                weight = weight,
            )
        }

        return result
    }

    private fun difficulty(advancement: Advancement): Int {
        val display = advancement.display ?: return 0
        val base = when (display.frame()) {
            Frame.CHALLENGE -> 4
            Frame.GOAL -> 2
            Frame.TASK -> 0
        }

        var depth = 0
        var current = advancement.parent
        val seen = mutableSetOf<NamespacedKey>()
        while (current != null && seen.add(current.key)) {
            depth++
            current = current.parent
        }

        return base + depth
    }
}
