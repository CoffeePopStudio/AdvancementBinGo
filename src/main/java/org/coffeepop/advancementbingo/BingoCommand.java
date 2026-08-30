package org.coffeepop.advancementbingo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BingoCommand implements CommandExecutor, TabCompleter {

    private final AdvancementBinGo plugin;

    public BingoCommand(AdvancementBinGo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该指令只能由玩家执行。", NamedTextColor.RED));
                    return true;
                }
                plugin.getBingoManager().joinLobby(player);
                return true;
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该指令只能由玩家执行。", NamedTextColor.RED));
                    return true;
                }
                plugin.getBingoManager().leave(player);
                return true;
            }
            case "lobby" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该指令只能由玩家执行。", NamedTextColor.RED));
                    return true;
                }
                plugin.getBingoManager().goLobby(player);
                player.sendMessage(Component.text("已传送到等待大厅。", NamedTextColor.GREEN));
                return true;
            }
            case "setlobby" -> {
                if (!sender.hasPermission("advancementbingo.admin")) {
                    sender.sendMessage(Component.text("你没有权限执行此指令。", NamedTextColor.RED));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("请站在想设为大厅的位置后使用玩家身份执行。", NamedTextColor.RED));
                    return true;
                }
                plugin.getBingoManager().setLobby(player);
                return true;
            }
            case "start" -> {
                if (!sender.hasPermission("advancementbingo.admin")) {
                    sender.sendMessage(Component.text("你没有权限执行此指令。", NamedTextColor.RED));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("请使用玩家身份执行开始指令。", NamedTextColor.RED));
                    return true;
                }
                plugin.getBingoManager().startGame(player);
                return true;
            }
            case "stop" -> {
                if (!sender.hasPermission("advancementbingo.admin")) {
                    sender.sendMessage(Component.text("你没有权限执行此指令。", NamedTextColor.RED));
                    return true;
                }
                int stopped = 0;
                for (BingoGame game : List.copyOf(plugin.getBingoManager().activeGames())) {
                    game.cleanup();
                    stopped++;
                }
                sender.sendMessage(Component.text("已停止 " + stopped + " 场成就Bingo游戏。", NamedTextColor.GOLD));
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("advancementbingo.admin")) {
                    sender.sendMessage(Component.text("你没有权限执行此指令。", NamedTextColor.RED));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getBingoManager().upgradeConfig();
                sender.sendMessage(Component.text("配置已重载。", NamedTextColor.GREEN));
                return true;
            }
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(List.of("join", "leave", "lobby", "help"));
            if (sender.hasPermission("advancementbingo.admin")) {
                suggestions.addAll(List.of("setlobby", "start", "stop", "reload"));
            }
            return suggestions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("==== 成就Bingo ====", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bingo join - 进入等待大厅", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bingo lobby - 传送到等待大厅", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/bingo leave - 离开", NamedTextColor.YELLOW));
        if (sender.hasPermission("advancementbingo.admin")) {
            sender.sendMessage(Component.text("/bingo setlobby - 把当前位置设为等待大厅", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/bingo start - 开始游戏", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/bingo stop - 停止所有游戏", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/bingo reload - 重载配置", NamedTextColor.YELLOW));
        }
    }
}
