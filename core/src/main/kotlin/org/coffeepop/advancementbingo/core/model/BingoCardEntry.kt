package org.coffeepop.advancementbingo.core.model

/**
 * A single cell on the 5x5 Bingo card.
 */
data class BingoCardEntry(
    val objectiveId: String,
    val tier: String? = null,
    val source: String? = null,
)
