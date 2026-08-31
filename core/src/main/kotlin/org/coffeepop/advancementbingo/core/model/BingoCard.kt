package org.coffeepop.advancementbingo.core.model

/**
 * A 5x5 Bingo card.
 */
class BingoCard(
    val entries: List<BingoCardEntry>,
    val objectives: Map<String, BingoObjective>,
) {
    init {
        require(entries.size == 25) { "Bingo card must contain exactly 25 entries, got ${entries.size}" }
    }

    fun entry(x: Int, y: Int): BingoCardEntry {
        require(x in 0 until 5) { "x must be in 0..4, got $x" }
        require(y in 0 until 5) { "y must be in 0..4, got $y" }
        return entries[x + y * 5]
    }

    fun objectiveAt(x: Int, y: Int): BingoObjective? = objectives[entry(x, y).objectiveId]

    /**
     * Returns every horizontal, vertical and diagonal line.
     */
    fun lines(): Sequence<List<BingoCardEntry>> = sequence {
        for (row in 0 until 5) {
            yield((0 until 5).map { entry(row, it) })
        }
        for (column in 0 until 5) {
            yield((0 until 5).map { entry(it, column) })
        }
        yield((0 until 5).map { entry(it, it) })
        yield((0 until 5).map { entry(it, 4 - it) })
    }

    fun countCompletedObjectives(team: String): Int =
        objectives.values.count { it.isAchievedBy(team) }

    fun countCompletedLines(team: String): Int =
        lines().count { line -> line.mapNotNull { objectives[it.objectiveId] }.all { it.isAchievedBy(team) } }
}
