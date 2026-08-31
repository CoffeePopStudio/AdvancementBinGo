package org.coffeepop.advancementbingo.port

/**
 * 从 bingo-main 移植的轻量游戏状态。
 */
class PortGameState {
    val cards = mutableListOf<PortBingoCard>()
    val teams = mutableMapOf<String, PortTeam>()

    fun activeCard(): PortBingoCard? = cards.firstOrNull()

    fun pushCard(card: PortBingoCard) {
        cards.add(0, card)
    }
}
