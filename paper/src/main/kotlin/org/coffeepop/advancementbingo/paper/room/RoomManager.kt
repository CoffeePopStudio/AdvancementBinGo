package org.coffeepop.advancementbingo.paper.room

import org.bukkit.Location
import org.bukkit.entity.Player
import org.coffeepop.advancementbingo.core.room.RoomConfig
import org.coffeepop.advancementbingo.core.room.RoomLobby
import org.coffeepop.advancementbingo.core.room.RoomMode

/**
 * Manages per-room lobbies and room modes on the Paper side.
 */
class RoomManager {

    private val lobbies = mutableMapOf<String, RoomLobby>()
    private val lobbyLocations = mutableMapOf<String, Location>()

    fun createRoom(config: RoomConfig, lobbyLocation: Location): RoomLobby {
        val lobby = RoomLobby(config)
        lobbies[config.id] = lobby
        lobbyLocations[config.id] = lobbyLocation
        return lobby
    }

    fun getLobby(roomId: String): RoomLobby? = lobbies[roomId]

    fun roomIds(): Set<String> = lobbies.keys

    fun joinRoom(player: Player, roomId: String): Boolean {
        val lobby = lobbies[roomId] ?: return false
        val location = lobbyLocations[roomId] ?: return false

        // Leave any existing room lobby first.
        leaveRoom(player)

        lobby.join(player.uniqueId.toString())
        player.teleport(location)
        return true
    }

    fun leaveRoom(player: Player) {
        val playerId = player.uniqueId.toString()
        for (lobby in lobbies.values) {
            if (playerId in lobby.players) {
                lobby.leave(playerId)
            }
        }
    }

    fun setMode(roomId: String, mode: RoomMode) {
        val lobby = lobbies[roomId] ?: return
        val old = lobby.config
        lobby.config = old.copy(mode = mode)
    }

    fun tickAllCountdowns() {
        for (lobby in lobbies.values) {
            if (lobby.countdownActive) {
                val finished = lobby.tickCountdown()
                if (finished) {
                    // TODO: start the actual game for this room.
                }
            }
        }
    }
}
