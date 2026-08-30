package org.coffeepop.advancementbingo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class BingoListener implements Listener {

    private final AdvancementBinGo plugin;
    private final BingoManager manager;

    public BingoListener(AdvancementBinGo plugin, BingoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        BingoGame game = manager.getGame(player);
        if (game != null) {
            // 接管原版成就提示：屏蔽默认聊天广播，只保留玩家自己的 toast
            event.message(null);
            game.handleAdvancement(player, event.getAdvancement());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (manager.getGame(player) != null) {
            event.setCancelled(true);
            manager.openBingoCard(player);
        } else if (manager.isInLobby(player)) {
            event.setCancelled(true);
            manager.openTeamSelection(player);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            BingoGame game = manager.getGame(player);
            if (manager.isInLobby(player) || (game != null && game.state() == BingoGame.State.STARTING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        manager.handlePortal(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        manager.handleInventoryClick(event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        manager.handleInteract(event);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (manager.getGame(player) != null || manager.isInLobby(player)) {
            if (event.getItemDrop().getItemStack().getType() != org.bukkit.Material.AIR) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (manager.getGame(event.getEntity()) != null) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        BingoGame game = manager.getGame(player);
        if (game == null) {
            return;
        }
        Team team = game.teamOf(player);
        if (team != null) {
            event.setRespawnLocation(game.spawnOf(team));
        }
    }
}
