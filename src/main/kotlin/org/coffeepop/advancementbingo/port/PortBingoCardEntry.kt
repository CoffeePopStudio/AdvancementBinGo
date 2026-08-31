package org.coffeepop.advancementbingo.port

/**
 * 从 bingo-main 移植的卡牌格子。
 */
data class PortBingoCardEntry(
    val objectiveId: String,
    val tier: String? = null,
    val source: String? = null,
)
