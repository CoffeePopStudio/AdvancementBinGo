package org.coffeepop.advancementbingo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BingoGame {

    public enum State {
        STARTING,
        PLAYING,
        FINISHED
    }

    private final AdvancementBinGo plugin;
    private final UUID id = UUID.randomUUID();
    private final World world;
    private final BingoCard card;
    private final Map<UUID, Team> teams = new HashMap<>();
    private final Map<Team, Set<Integer>> completed = new HashMap<>();
    private final Map<UUID, Set<NamespacedKey>> precompleted = new HashMap<>();
    private final Location redSpawn;
    private final Location blueSpawn;
    private final int winScore;
    private State state = State.STARTING;

    public BingoGame(AdvancementBinGo plugin, World world, BingoCard card,
                     Map<UUID, Team> teams, Location redSpawn, Location blueSpawn, int winScore) {
        this.plugin = plugin;
        this.world = world;
        this.card = card;
        this.teams.putAll(teams);
        this.redSpawn = redSpawn;
        this.blueSpawn = blueSpawn;
        this.winScore = winScore;
        for (Team team : Team.values()) {
            completed.put(team, new HashSet<>());
        }
        snapshotPrecompleted();
    }

    public UUID id() {
        return id;
    }

    public World world() {
        return world;
    }

    public BingoCard card() {
        return card;
    }

    public State state() {
        return state;
    }

    public Team teamOf(UUID playerId) {
        return teams.get(playerId);
    }

    public Team teamOf(Player player) {
        return teamOf(player.getUniqueId());
    }

    public Location spawnOf(Team team) {
        return team == Team.RED ? redSpawn : blueSpawn;
    }

    public int score(Team team) {
        return completed.getOrDefault(team, Set.of()).size();
    }

    public boolean isCompleted(Team team, int index) {
        return completed.getOrDefault(team, Set.of()).contains(index);
    }

    public boolean isRevealed(Team viewer, int index) {
        // 新规则：所有成就名对双方始终可见，对方完成不会影响我方 GUI
        return true;
    }

    public void start() {
        state = State.STARTING;
        int observationSeconds = plugin.getConfig().getInt("game.observation-seconds", 5);

        plugin.getBingoManager().applySidebar(this);

        for (Player player : players()) {
            preparePlayer(player);
            player.teleport(spawnOf(teamOf(player)));
            player.setAllowFlight(true);
            player.setFlying(true);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendTitle(
                    "观察时间",
                    observationSeconds + "秒后开始",
                    10, observationSeconds * 20, 10
            );
        }

        broadcast(Component.text("游戏开始！请在观察点查看周围环境。", NamedTextColor.GOLD));
        sendRules();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != State.STARTING) {
                return;
            }
            state = State.PLAYING;
            int floatSeconds = plugin.getConfig().getInt("game.float-seconds", 10);
            PotionEffectType effectType = parseFloatEffect(plugin.getConfig().getString("game.float-effect", "SLOW_FALLING"));

            for (Player player : players()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setInvulnerable(false);
                if (effectType != null) {
                    player.addPotionEffect(new PotionEffect(effectType, floatSeconds * 20, 0, false, false, true));
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);
                player.sendTitle(
                        "开始！",
                        "完成成就为队伍得分",
                        5, 30, 10
                );
                player.sendActionBar(Component.text("目标: " + winScore + " 分", NamedTextColor.GOLD));
            }

            broadcast(Component.text("游戏正式开始！率先达到 " + winScore + " 分的队伍获胜。", NamedTextColor.GREEN));
        }, observationSeconds * 20L);
    }

    public void handleAdvancement(Player player, Advancement advancement) {
        if (state != State.PLAYING) {
            return;
        }
        Team team = teamOf(player);
        if (team == null) {
            return;
        }
        NamespacedKey key = advancement.getKey();
        int index = card.indexOf(key);
        if (!card.hasIndex(index)) {
            return;
        }
        if (precompleted.getOrDefault(player.getUniqueId(), Set.of()).contains(key)) {
            return;
        }
        Set<Integer> done = completed.get(team);
        if (!done.add(index)) {
            return;
        }

        Component name = card.nameAt(index);
        int newScore = done.size();

        Component teamMessage = Component.text()
                .append(Component.text("[" + team.displayName() + "] ", team.chatColor()))
                .append(Component.text("完成了成就 ", NamedTextColor.WHITE))
                .append(name.color(NamedTextColor.YELLOW))
                .append(Component.text("！", NamedTextColor.WHITE))
                .append(Component.text(" 当前 " + newScore + "/" + winScore + " 分", NamedTextColor.GRAY))
                .build();

        Component otherMessage = Component.text()
                .append(Component.text("[" + team.displayName() + "] ", team.chatColor()))
                .append(Component.text("完成了一个成就！", NamedTextColor.WHITE))
                .append(Component.text(" 当前 " + newScore + "/" + winScore + " 分", NamedTextColor.GRAY))
                .build();

        sendToTeam(team, teamMessage);
        sendToTeam(team.other(), otherMessage);

        for (Player p : players()) {
            if (teamOf(p) == team) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            } else {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.7f);
            }
            plugin.getBingoManager().refreshCardItem(p);
        }

        plugin.getBingoManager().updateSidebar(this);

        if (newScore >= winScore) {
            endGame(team);
        }
    }

    public void endGame(Team winner) {
        if (state == State.FINISHED) {
            return;
        }
        state = State.FINISHED;

        Component title = Component.text(winner.displayName() + " 获胜！", winner.chatColor());
        for (Player player : players()) {
            player.sendTitle(winner.displayName() + " 获胜！", "恭喜完成 " + score(winner) + " 个成就", 10, 100, 20);
            player.sendMessage(title);
            if (teamOf(player) == winner) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            }
        }
        broadcast(Component.text("本局成就Bingo结束。", NamedTextColor.GOLD));

        Bukkit.getScheduler().runTaskLater(plugin, this::cleanup, 120L);
    }

    public void cleanup() {
        state = State.FINISHED;
        for (Player player : players()) {
            plugin.getBingoManager().sendToLobby(player);
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        teams.clear();
        plugin.getBingoManager().removeGame(this);
        // 每局结束后异步重置该房间（主世界+地狱+末地），重置完成前房间不可用
        plugin.getBingoManager().resetRoomAsync(world);
    }

    public List<Player> players() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : teams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    private void broadcast(Component message) {
        for (Player player : players()) {
            player.sendMessage(message);
        }
    }

    private void sendRules() {
        for (String rule : plugin.getConfig().getStringList("game.rules")) {
            String formatted = rule.replace("{win-score}", String.valueOf(winScore));
            Component message = LegacyComponentSerializer.legacySection().deserialize(formatted);
            broadcast(prefix().append(message));
        }
    }

    private Component prefix() {
        String raw = plugin.getConfig().getString("messages.prefix", "&8[&6成就Bingo&8] ");
        return LegacyComponentSerializer.legacySection().deserialize(raw);
    }

    private void sendToTeam(Team team, Component message) {
        for (Player player : players()) {
            if (teamOf(player) == team) {
                player.sendMessage(message);
            }
        }
    }

    public boolean contains(UUID playerId) {
        return teams.containsKey(playerId);
    }

    public void removePlayer(Player player) {
        teams.remove(player.getUniqueId());
        precompleted.remove(player.getUniqueId());
    }

    private void snapshotPrecompleted() {
        for (UUID uuid : teams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Set<NamespacedKey> doneBefore = new HashSet<>();
            for (NamespacedKey key : card.keys()) {
                if (key == null) {
                    continue;
                }
                Advancement advancement = Bukkit.getAdvancement(key);
                if (advancement != null && player.getAdvancementProgress(advancement).isDone()) {
                    doneBefore.add(key);
                }
            }
            precompleted.put(uuid, doneBefore);
        }
    }

    private void preparePlayer(Player player) {
        player.getInventory().clear();
        // 不再发放纸物品，游戏中通过 潜行+F 打开成就 GUI
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setInvulnerable(true);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setGameMode(GameMode.ADVENTURE);
        player.closeInventory();
    }

    private PotionEffectType parseFloatEffect(String name) {
        PotionEffectType effect = PotionEffectType.getByName(name);
        return effect != null ? effect : PotionEffectType.SLOW_FALLING;
    }
}
