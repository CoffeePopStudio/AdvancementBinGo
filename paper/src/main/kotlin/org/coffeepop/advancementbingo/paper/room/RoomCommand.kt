package org.coffeepop.advancementbingo.paper.room

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.coffeepop.advancementbingo.core.room.RoomMode

/**
 * Command handler for per-room lobby management.
 *
 * Commands:
 * - /bingo join <room>
 * - /bingo room setmode <room> <mode>
 */
class RoomCommand(
    private val roomManager: RoomManager,
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("Usage: /bingo join <room> | /bingo room setmode <room> <mode>")
            return true
        }

        return when (args[0].lowercase()) {
            "join" -> handleJoin(sender, args)
            "room" -> handleRoom(sender, args)
            else -> {
                sender.sendMessage("Unknown subcommand.")
                true
            }
        }
    }

    private fun handleJoin(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can join a room.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("Usage: /bingo join <room>")
            return true
        }

        val roomId = args[1].lowercase()
        return if (roomManager.joinRoom(sender, roomId)) {
            sender.sendMessage("Joined room $roomId.")
            true
        } else {
            sender.sendMessage("Room $roomId not found.")
            false
        }
    }

    private fun handleRoom(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 4 || args[1].lowercase() != "setmode") {
            sender.sendMessage("Usage: /bingo room setmode <room> <mode>")
            return true
        }

        val roomId = args[2].lowercase()
        val mode = runCatching { RoomMode.valueOf(args[3].uppercase()) }.getOrNull()
            ?: run {
                sender.sendMessage("Unknown mode. Available: ${RoomMode.entries.joinToString()}")
                return true
            }

        roomManager.setMode(roomId, mode)
        sender.sendMessage("Room $roomId mode set to ${mode.name}.")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): MutableList<String> {
        return when (args.size) {
            1 -> mutableListOf("join", "room")
            2 -> when (args[0].lowercase()) {
                "join" -> roomManager.roomIds().toMutableList()
                "room" -> mutableListOf("setmode")
                else -> mutableListOf()
            }
            3 -> if (args[0].lowercase() == "room" && args[1].lowercase() == "setmode") {
                roomManager.roomIds().toMutableList()
            } else {
                mutableListOf()
            }
            4 -> if (args[0].lowercase() == "room" && args[1].lowercase() == "setmode") {
                RoomMode.entries.map { it.name }.toMutableList()
            } else {
                mutableListOf()
            }
            else -> mutableListOf()
        }
    }
}
