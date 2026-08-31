package org.coffeepop.advancementbingo.core.model

/**
 * Available game modes.
 */
enum class GameMode {
    /** Standard achievement/item/stats bingo. */
    NORMAL,

    /** A completed objective can only be claimed by one team. */
    LOCKOUT,

    /** Objectives completed by the enemy team are hidden from our board. */
    HIDDEN_ITEMS,
}
