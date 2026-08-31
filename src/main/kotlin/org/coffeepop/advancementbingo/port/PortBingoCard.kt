package org.coffeepop.advancementbingo.port

/**
 * 从 bingo-main 移植的 5x5 Bingo 卡。
 */
class PortBingoCard(
    val entries: List<PortBingoCardEntry>,
    val objectives: Map<String, PortBingoObjective>,
) {
    init {
        require(entries.size == 25) { "BINGO card was created with less than 25 entries! (${entries.size})" }
    }

    fun entry(x: Int, y: Int): PortBingoCardEntry {
        require(x in 0 until 5) { "x=$x is not within 0..4" }
        require(y in 0 until 5) { "y=$y is not within 0..4" }
        return entries[x + y * 5]
    }

    fun objective(x: Int, y: Int): PortBingoObjective? {
        return objectives[entry(x, y).objectiveId]
    }

    fun lines(): Sequence<List<PortBingoCardEntry>> = sequence {
        for (row in 0 until 5) {
            yield((0 until 5).map { entry(row, it) })
        }
        for (col in 0 until 5) {
            yield((0 until 5).map { entry(it, col) })
        }
        yield((0 until 5).map { entry(it, it) })
        yield((0 until 5).map { entry(it, 4 - it) })
    }

    fun countItems(team: String): Int = objectives.values.count { it.hasTeamAchieved(team) }

    fun countLines(team: String): Int = lines().count { line ->
        line.mapNotNull { objectives[it.objectiveId] }.all { it.hasTeamAchieved(team) }
    }
}
