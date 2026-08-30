package org.coffeepop.advancementbingo;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class AdvancementBinGo extends JavaPlugin {

    private BingoManager bingoManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.bingoManager = new BingoManager(this);
        bingoManager.upgradeConfig();
        getServer().getPluginManager().registerEvents(new BingoListener(this, bingoManager), this);

        BingoCommand command = new BingoCommand(this);
        PluginCommand bingoCommand = getCommand("bingo");
        Objects.requireNonNull(bingoCommand, "bingo command missing").setExecutor(command);
        bingoCommand.setTabCompleter(command);

        // 启动时创建3个主世界房间（以及各自独立的地狱/末地）
        bingoManager.createWorlds();

        getLogger().info("成就Bingo 已启用。");
    }

    @Override
    public void onDisable() {
        if (bingoManager != null) {
            for (BingoGame game : bingoManager.activeGames()) {
                game.cleanup();
            }
        }
        getLogger().info("成就Bingo 已禁用。");
    }

    public BingoManager getBingoManager() {
        return bingoManager;
    }
}
