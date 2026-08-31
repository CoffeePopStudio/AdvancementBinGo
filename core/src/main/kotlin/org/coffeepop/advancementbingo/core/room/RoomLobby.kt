package org.coffeepop.advancementbingo.core.room

/**
 * Mutable per-room lobby state.
 *
 * This class is intentionally platform-agnostic: player IDs are strings and
 * countdown logic is driven by an external scheduler.
 */
class RoomLobby(
    var config: RoomConfig,
) {
    val players: MutableSet<String> = linkedSetOf()
    val selectedTeams: MutableMap<String, String> = mutableMapOf()

    var countdownActive: Boolean = false
        private set

    var countdownSecondsLeft: Int = 0
        private set

    val playerCount: Int
        get() = players.size

    fun join(playerId: String) {
        players += playerId
    }

    fun leave(playerId: String) {
        players -= playerId
        selectedTeams.remove(playerId)
    }

    fun selectTeam(playerId: String, team: String) {
        selectedTeams[playerId] = team
    }

    fun startCountdown(seconds: Int) {
        countdownActive = true
        countdownSecondsLeft = seconds.coerceAtLeast(1)
    }

    fun tickCountdown(): Boolean {
        if (!countdownActive) return false
        countdownSecondsLeft--
        if (countdownSecondsLeft <= 0) {
            countdownActive = false
            return true
        }
        return false
    }

    fun cancelCountdown() {
        countdownActive = false
        countdownSecondsLeft = 0
    }
}
