package org.coffeepop.advancementbingo.port

import io.papermc.paper.advancement.AdvancementDisplay.Frame
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.advancement.Advancement
import org.bukkit.inventory.ItemStack
import java.util.Locale
import kotlin.random.Random

/**
 * 从 bingo-main 的 CardService 提取并适配 Paper 的成就卡生成器。
 */
object PortCardGenerator {

    private val random = Random.Default

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

    private enum class Tier { EASY, MEDIUM, HARD }

    private data class Candidate(
        val advancement: Advancement,
        val difficulty: Int,
        val tier: Tier,
        val weight: Int,
    )

    fun generate(easyCount: Int = 12, mediumCount: Int = 8, hardCount: Int = 5): PortBingoCard {
        val all = mutableListOf<Candidate>()
        val easy = mutableListOf<Candidate>()
        val medium = mutableListOf<Candidate>()
        val hard = mutableListOf<Candidate>()

        val iterator = Bukkit.advancementIterator()
        while (iterator.hasNext()) {
            val advancement = iterator.next()
            val key = advancement.key
            val display = advancement.display ?: continue
            if (key.key.lowercase(Locale.ROOT).startsWith("recipes/")) continue

            val difficulty = difficulty(advancement)
            val tier = when {
                difficulty <= 2 -> Tier.EASY
                difficulty <= 4 -> Tier.MEDIUM
                else -> Tier.HARD
            }

            val baseWeight = when (tier) {
                Tier.EASY -> 100
                Tier.MEDIUM -> 60
                Tier.HARD -> 30
            }
            val weight = if (key.key in extremeHardKeys) 5 else baseWeight

            val candidate = Candidate(advancement, difficulty, tier, weight)
            all += candidate
            when (tier) {
                Tier.EASY -> easy += candidate
                Tier.MEDIUM -> medium += candidate
                Tier.HARD -> hard += candidate
            }
        }

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

        val selected = mutableListOf<Advancement>()
        val usedKeys = mutableSetOf<NamespacedKey>()

        addPicked(selected, usedKeys, pickWeighted(easy, easyTarget))
        addPicked(selected, usedKeys, pickWeighted(medium, mediumTarget))
        addPicked(selected, usedKeys, pickWeighted(hard, hardTarget))

        if (selected.size < 25) {
            val rest = all.map { it.advancement }.filter { !usedKeys.contains(it.key) }.shuffled(random)
            for (advancement in rest) {
                if (selected.size >= 25) break
                selected += advancement
                usedKeys += advancement.key
            }
        }

        selected.shuffle(random)

        val entries = mutableListOf<PortBingoCardEntry>()
        val objectives = mutableMapOf<String, PortBingoObjective>()

        for ((index, advancement) in selected.withIndex()) {
            val id = advancement.key.toString()
            entries += PortBingoCardEntry(objectiveId = id)
            objectives[id] = PortBingoObjective.Advancement(
                id = id,
                advancementKey = advancement.key,
                name = displayName(advancement),
                icon = iconOf(advancement),
            )
        }

        var emptyIndex = 0
        while (entries.size < 25) {
            val id = "empty:$emptyIndex"
            emptyIndex++
            entries += PortBingoCardEntry(objectiveId = id)
            objectives[id] = PortBingoObjective.Advancement(
                id = id,
                advancementKey = NamespacedKey.minecraft("empty"),
                name = Component.text("???"),
                icon = null,
            )
        }

        return PortBingoCard(entries, objectives)
    }

    private fun addPicked(
        target: MutableList<Advancement>,
        usedKeys: MutableSet<NamespacedKey>,
        picked: List<Advancement>,
    ) {
        for (advancement in picked) {
            if (target.size >= 25) break
            if (usedKeys.add(advancement.key)) {
                target += advancement
            }
        }
    }

    private fun pickWeighted(pool: List<Candidate>, count: Int): List<Advancement> {
        val remaining = pool.toMutableList()
        val picked = mutableListOf<Advancement>()
        while (picked.size < count && remaining.isNotEmpty()) {
            val totalWeight = remaining.sumOf { it.weight }
            if (totalWeight <= 0) break
            var roll = random.nextInt(totalWeight)
            var chosen: Candidate? = null
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
            picked += chosen.advancement
            remaining.remove(chosen)
        }
        return picked
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

    private fun displayName(advancement: Advancement): Component {
        return advancement.display?.title() ?: Component.text(advancement.key.key)
    }

    private fun iconOf(advancement: Advancement): ItemStack? {
        return runCatching { advancement.display?.icon()?.clone() }.getOrNull()
    }
}
