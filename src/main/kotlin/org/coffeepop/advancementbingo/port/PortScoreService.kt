package org.coffeepop.advancementbingo.port

/**
 * 从 bingo-main 移植的计分逻辑（先保留 items/lines）。
 */
object PortScoreService {

    fun countItems(card: PortBingoCard, team: String): Int = card.countItems(team)

    fun countLines(card: PortBingoCard, team: String): Int = card.countLines(team)

    fun hasWon(card: PortBingoCard, team: String, winScore: Int): Boolean {
        return countItems(card, team) >= winScore
    }
}
