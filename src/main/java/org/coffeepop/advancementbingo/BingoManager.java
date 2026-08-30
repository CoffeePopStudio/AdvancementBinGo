package org.coffeepop.advancementbingo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class BingoManager {

    public static final int CONFIG_VERSION = 3;

    private final AdvancementBinGo plugin;
    private final List<World> roomWorlds = new ArrayList<>();
    private final List<BingoGame> games = new ArrayList<>();
    private final Set<UUID> lobbyPlayers = new HashSet<>();
    private final Map<UUID, Team> selectedTeams = new HashMap<>();
    private final Map<UUID, BingoGame> playerGames = new HashMap<>();
    private final Map<UUID, Scoreboard> gameScoreboards = new HashMap<>();
    private final Random random = new Random();

    private BossBar lobbyBossBar;
    private int countdownTaskId = -1;
    private int countdownSecondsLeft = 0;
    private boolean countdownActive = false;
    private final Set<String> resettingRooms = new HashSet<>();

    private static final int[] CARD_SLOTS = {
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33,
            38, 39, 40, 41, 42,
            47, 48, 49, 50, 51
    };

    public BingoManager(AdvancementBinGo plugin) {
        this.plugin = plugin;
        this.lobbyBossBar = Bukkit.createBossBar(
                "等待玩家...",
                BarColor.YELLOW,
                BarStyle.SOLID
        );
    }

    public void upgradeConfig() {
        FileConfiguration config = plugin.getConfig();
        if (plugin.getResource("config.yml") == null) {
            return;
        }
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(plugin.getResource("config.yml"), StandardCharsets.UTF_8)
        );
        config.setDefaults(defaults);
        config.options().copyDefaults(true);

        int version = config.getInt("config-version", 0);
        if (version < CONFIG_VERSION) {
            // 未来需要做配置迁移时在这里按版本升级
            plugin.getLogger().info("检测到旧版配置文件，正在补充缺失配置项...");
        }
        config.set("config-version", CONFIG_VERSION);
        plugin.saveConfig();
    }

    public void createWorlds() {
        FileConfiguration config = plugin.getConfig();
        int count = Math.max(1, config.getInt("rooms.count", 3));
        String prefix = config.getString("rooms.prefix", "bingo");

        for (int i = 0; i < count; i++) {
            String overworldName = prefix + "_" + i;
            World overworld = Bukkit.getWorld(overworldName);
            if (overworld == null) {
                overworld = new WorldCreator(overworldName)
                        .environment(World.Environment.NORMAL)
                        .seed(random.nextLong())
                        .generateStructures(true)
                        .createWorld();
            }
            if (overworld != null) {
                roomWorlds.add(overworld);
                prepareRoomWorld(overworld);
            }

            String netherName = overworldName + "_nether";
            World nether = Bukkit.getWorld(netherName);
            if (nether == null) {
                nether = new WorldCreator(netherName)
                        .environment(World.Environment.NETHER)
                        .seed(random.nextLong())
                        .generateStructures(true)
                        .createWorld();
            }
            if (nether != null) {
                prepareRoomWorld(nether);
            }

            String endName = overworldName + "_end";
            World end = Bukkit.getWorld(endName);
            if (end == null) {
                end = new WorldCreator(endName)
                        .environment(World.Environment.THE_END)
                        .seed(random.nextLong())
                        .generateStructures(true)
                        .createWorld();
            }
            if (end != null) {
                prepareRoomWorld(end);
            }
        }
    }

    private void prepareRoomWorld(World world) {
        try {
            world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, true);
        } catch (Exception ignored) {
        }
    }

    public Location lobbyLocation() {
        FileConfiguration config = plugin.getConfig();
        String worldName = config.getString("lobby.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                config.getDouble("lobby.x", world.getSpawnLocation().getX()),
                config.getDouble("lobby.y", world.getSpawnLocation().getY()),
                config.getDouble("lobby.z", world.getSpawnLocation().getZ()),
                (float) config.getDouble("lobby.yaw", world.getSpawnLocation().getYaw()),
                (float) config.getDouble("lobby.pitch", world.getSpawnLocation().getPitch())
        );
    }

    public void sendToLobby(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, createTeamItem());
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(true);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.closeInventory();
        clearSidebar(player);
        Location lobby = lobbyLocation();
        if (lobby != null) {
            player.teleport(lobby);
        }
        lobbyPlayers.add(player.getUniqueId());
        lobbyBossBar.removePlayer(player);
        lobbyBossBar.addPlayer(player);
        updateLobbyDisplay();
    }

    public void joinLobby(Player player) {
        if (getGame(player) != null) {
            player.sendMessage(prefix().append(Component.text("你已经在游戏中！", NamedTextColor.RED)));
            return;
        }
        sendToLobby(player);
        player.sendMessage(prefix().append(Component.text("你已进入等待大厅，点击快捷栏的「选择队伍」选择队伍。", NamedTextColor.GREEN)));
        broadcastToLobby(prefix().append(Component.text(player.getName() + " 进入了等待大厅 (" + lobbyPlayers.size() + "/" + requiredPlayers() + ")", NamedTextColor.GRAY)));
        updateLobbyDisplay();
    }

    public void leave(Player player) {
        BingoGame game = getGame(player);
        if (game != null) {
            game.removePlayer(player);
            playerGames.remove(player.getUniqueId());
            if (game.players().isEmpty()) {
                game.cleanup();
            }
        }
        boolean wasInLobby = lobbyPlayers.remove(player.getUniqueId());
        selectedTeams.remove(player.getUniqueId());
        if (wasInLobby) {
            lobbyBossBar.removePlayer(player);
            updateLobbyDisplay();
            broadcastToLobby(prefix().append(Component.text(player.getName() + " 离开了等待大厅 (" + lobbyPlayers.size() + "/" + requiredPlayers() + ")", NamedTextColor.GRAY)));
        }
        player.getInventory().clear();
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setGameMode(GameMode.ADVENTURE);
        clearSidebar(player);
        Location lobby = lobbyLocation();
        if (lobby != null) {
            player.teleport(lobby);
        }
        player.sendMessage(prefix().append(Component.text("你已离开成就Bingo。", NamedTextColor.GREEN)));
    }

    public void setLobby(Player player) {
        Location location = player.getLocation();
        FileConfiguration config = plugin.getConfig();
        config.set("lobby.world", location.getWorld().getName());
        config.set("lobby.x", location.getX());
        config.set("lobby.y", location.getY());
        config.set("lobby.z", location.getZ());
        config.set("lobby.yaw", (double) location.getYaw());
        config.set("lobby.pitch", (double) location.getPitch());
        plugin.saveConfig();
        player.sendMessage(prefix().append(Component.text("等待大厅已设置为当前位置。", NamedTextColor.GREEN)));
    }

    public void goLobby(Player player) {
        Location lobby = lobbyLocation();
        if (lobby != null) {
            player.teleport(lobby);
        }
    }

    public void startGame(Player sender) {
        startGameInternal(sender);
    }

    public void autoStartGame() {
        startGameInternal(null);
    }

    private void startGameInternal(Player sender) {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : lobbyPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }

        int required = requiredPlayers();
        if (players.size() < required) {
            if (sender != null) {
                sender.sendMessage(prefix().append(Component.text("至少需要 " + required + " 名玩家才能开始。", NamedTextColor.RED)));
            } else {
                broadcastToLobby(prefix().append(Component.text("人数不足，无法开始。", NamedTextColor.RED)));
            }
            return;
        }

        World world = pickAvailableRoom();
        if (world == null) {
            if (sender != null) {
                sender.sendMessage(prefix().append(Component.text("没有可用的房间，请等待其他游戏结束。", NamedTextColor.RED)));
            } else {
                broadcastToLobby(prefix().append(Component.text("没有可用的房间，请等待其他游戏结束。", NamedTextColor.RED)));
            }
            return;
        }

        Map<UUID, Team> teamMap = assignTeams(players);
        if (teamMap == null) {
            if (sender != null) {
                sender.sendMessage(prefix().append(Component.text("无法分配队伍（人数不足）。", NamedTextColor.RED)));
            }
            return;
        }

        BingoCard card = BingoCard.createRandom(
                plugin.getConfig().getInt("game.easy-count", 12),
                plugin.getConfig().getInt("game.medium-count", 8),
                plugin.getConfig().getInt("game.hard-count", 5)
        );
        Location redSpawn = randomSpawn(world);
        Location blueSpawn = randomSpawnAwayFrom(world, redSpawn);

        BingoGame game = new BingoGame(plugin, world, card, teamMap, redSpawn, blueSpawn,
                plugin.getConfig().getInt("game.win-score", 13));
        games.add(game);
        for (UUID uuid : teamMap.keySet()) {
            playerGames.put(uuid, game);
        }

        for (UUID uuid : lobbyPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                lobbyBossBar.removePlayer(player);
            }
        }
        lobbyPlayers.clear();
        cancelCountdownSilently();

        if (sender != null) {
            sender.sendMessage(prefix().append(Component.text("游戏开始！房间: " + world.getName(), NamedTextColor.GOLD)));
        } else {
            for (Player player : players) {
                player.sendMessage(prefix().append(Component.text("人数已达标，游戏开始！房间: " + world.getName(), NamedTextColor.GOLD)));
            }
        }
        game.start();
    }

    private Map<UUID, Team> assignTeams(List<Player> players) {
        Map<UUID, Team> result = new HashMap<>();
        int red = 0;
        int blue = 0;

        // 先尊重玩家已选的队伍
        for (Player player : players) {
            Team selected = selectedTeams.get(player.getUniqueId());
            if (selected == Team.RED) {
                result.put(player.getUniqueId(), Team.RED);
                red++;
            } else if (selected == Team.BLUE) {
                result.put(player.getUniqueId(), Team.BLUE);
                blue++;
            }
        }

        // 未选择的玩家自动加入人数较少的一方
        for (Player player : players) {
            if (result.containsKey(player.getUniqueId())) {
                continue;
            }
            Team team = red <= blue ? Team.RED : Team.BLUE;
            result.put(player.getUniqueId(), team);
            if (team == Team.RED) red++;
            else blue++;
        }

        // 如果某一队没人，把另一队的人随机移过去，保证两队都有玩家
        if (red == 0 || blue == 0) {
            List<UUID> majority = new ArrayList<>();
            for (Map.Entry<UUID, Team> entry : result.entrySet()) {
                if (entry.getValue() == (red == 0 ? Team.BLUE : Team.RED)) {
                    majority.add(entry.getKey());
                }
            }
            if (majority.isEmpty()) {
                return null;
            }
            UUID move = majority.get(random.nextInt(majority.size()));
            Team newTeam = red == 0 ? Team.RED : Team.BLUE;
            result.put(move, newTeam);
        }

        return result;
    }

    private World pickAvailableRoom() {
        List<World> available = new ArrayList<>();
        for (World world : roomWorlds) {
            if (resettingRooms.contains(world.getName())) {
                continue;
            }
            boolean used = false;
            for (BingoGame game : games) {
                if (game.world().equals(world)) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                available.add(world);
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        return available.get(random.nextInt(available.size()));
    }

    private Location safeLocation(World world, double x, double z) {
        int highest = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        int y = Math.max(highest + 1, world.getMinHeight() + 1);
        return new Location(world, Math.floor(x) + 0.5, y, Math.floor(z) + 0.5);
    }

    private Location randomSpawn(World world) {
        int radius = plugin.getConfig().getInt("rooms.radius", 300);
        int airHeight = plugin.getConfig().getInt("rooms.spawn-air-height", 25);
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = radius * (0.2 + random.nextDouble() * 0.8);
        int x = (int) (world.getSpawnLocation().getX() + Math.cos(angle) * distance);
        int z = (int) (world.getSpawnLocation().getZ() + Math.sin(angle) * distance);
        int groundY = Math.max(world.getHighestBlockYAt(x, z), world.getSpawnLocation().getBlockY());
        int y = Math.min(world.getMaxHeight() - 30, groundY + airHeight);
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private Location randomSpawnAwayFrom(World world, Location other) {
        int minDistance = plugin.getConfig().getInt("rooms.min-team-distance", 120);
        int radius = plugin.getConfig().getInt("rooms.radius", 300);
        int airHeight = plugin.getConfig().getInt("rooms.spawn-air-height", 25);
        Location candidate = null;
        for (int i = 0; i < 40; i++) {
            Location loc = randomSpawn(world);
            if (loc.distance(other) >= minDistance) {
                candidate = loc;
                break;
            }
        }
        if (candidate == null) {
            candidate = randomSpawn(world);
        }
        return candidate;
    }

    public BingoGame getGame(Player player) {
        return playerGames.get(player.getUniqueId());
    }

    public BingoGame getGame(UUID playerId) {
        return playerGames.get(playerId);
    }

    public boolean isInLobby(Player player) {
        return lobbyPlayers.contains(player.getUniqueId());
    }

    public void onPlayerQuit(Player player) {
        // 如果玩家在游戏中，保留其游戏数据，允许重进后回到原游戏/原队伍
        boolean wasInLobby = lobbyPlayers.remove(player.getUniqueId());
        if (wasInLobby) {
            lobbyBossBar.removePlayer(player);
            updateLobbyDisplay();
            broadcastToLobby(prefix().append(Component.text(player.getName() + " 离开了等待大厅 (" + lobbyPlayers.size() + "/" + requiredPlayers() + ")", NamedTextColor.GRAY)));
        }
        selectedTeams.remove(player.getUniqueId());
    }

    public void onPlayerJoin(Player player) {
        BingoGame game = playerGames.get(player.getUniqueId());
        if (game != null && game.teamOf(player) != null) {
            restoreToGame(player, game);
            return;
        }

        sendToLobby(player);
        player.sendMessage(prefix().append(Component.text("你已进入等待大厅，点击快捷栏的「选择队伍」选择队伍。", NamedTextColor.GREEN)));
        broadcastToLobby(prefix().append(Component.text(player.getName() + " 进入了等待大厅 (" + lobbyPlayers.size() + "/" + requiredPlayers() + ")", NamedTextColor.GRAY)));
        updateLobbyDisplay();
    }

    private void restoreToGame(Player player, BingoGame game) {
        Team team = game.teamOf(player);
        if (team == null) {
            sendToLobby(player);
            return;
        }

        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(game.spawnOf(team));
        player.sendMessage(prefix().append(Component.text("欢迎回来，你仍在 " + team.displayName() + "。", team.chatColor())));

        if (game.state() == BingoGame.State.STARTING) {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setInvulnerable(true);
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setInvulnerable(false);
        }

        applySidebar(game);
    }

    public List<BingoGame> activeGames() {
        return List.copyOf(games);
    }

    public void applySidebar(BingoGame game) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(
                "bingo",
                "dummy",
                Component.text("成就Bingo", NamedTextColor.GOLD)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        gameScoreboards.put(game.id(), board);
        for (Player player : game.players()) {
            player.setScoreboard(board);
        }
        updateSidebar(game);
    }

    public void updateSidebar(BingoGame game) {
        Scoreboard board = gameScoreboards.get(game.id());
        if (board == null) {
            return;
        }
        Objective objective = board.getObjective("bingo");
        if (objective == null) {
            return;
        }
        int winScore = plugin.getConfig().getInt("game.win-score", 13);

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        objective.getScore("目标分数: " + winScore).setScore(3);
        objective.getScore(" ").setScore(2);
        objective.getScore("红队: " + game.score(Team.RED) + "/" + winScore).setScore(1);
        objective.getScore("蓝队: " + game.score(Team.BLUE) + "/" + winScore).setScore(0);
    }

    public void clearSidebar(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void removeGame(BingoGame game) {
        games.remove(game);
        gameScoreboards.remove(game.id());
        for (UUID uuid : new ArrayList<>(playerGames.keySet())) {
            if (playerGames.get(uuid) == game) {
                playerGames.remove(uuid);
            }
        }
    }

    public int requiredPlayers() {
        return Math.max(2, plugin.getConfig().getInt("lobby.required-players", 10));
    }

    private boolean hasAvailableRoom() {
        for (World world : roomWorlds) {
            if (resettingRooms.contains(world.getName())) {
                continue;
            }
            boolean used = false;
            for (BingoGame game : games) {
                if (game.world().equals(world)) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                return true;
            }
        }
        return false;
    }

    public void updateLobbyDisplay() {
        updateLobbyState();

        int current = lobbyPlayers.size();
        int required = requiredPlayers();
        String actionbar = "&e等待大厅 &f" + current + "&7/" + required
                + (countdownActive ? " &6倒计时 " + countdownSecondsLeft + "s" : (current >= required ? " &a准备开始..." : " &7等待更多玩家"));
        Component actionComponent = LegacyComponentSerializer.legacySection().deserialize(actionbar);

        for (UUID uuid : lobbyPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendActionBar(actionComponent);
            }
        }

        String bossbarText = "等待玩家: " + current + "/" + required
                + (countdownActive ? " 倒计时 " + countdownSecondsLeft + "s" : "");
        lobbyBossBar.setTitle(bossbarText.replace("&", "§"));
        lobbyBossBar.setProgress(required > 0 ? Math.min(1.0, current / (double) required) : 0.0);
        if (countdownActive) {
            lobbyBossBar.setColor(countdownSecondsLeft <= 5 ? BarColor.RED : BarColor.GREEN);
        } else {
            lobbyBossBar.setColor(BarColor.YELLOW);
        }
    }

    private void updateLobbyState() {
        int current = lobbyPlayers.size();
        int required = requiredPlayers();

        if (!countdownActive && current >= required && hasAvailableRoom()) {
            startCountdown();
        } else if (countdownActive && (current < required || !hasAvailableRoom())) {
            cancelCountdown("人数不足或没有空闲房间，倒计时取消");
        }
    }

    private void startCountdown() {
        if (countdownActive) {
            return;
        }
        countdownActive = true;
        countdownSecondsLeft = Math.max(1, plugin.getConfig().getInt("lobby.countdown-seconds", 30));
        broadcastToLobby(prefix().append(Component.text("人数已达标！" + countdownSecondsLeft + " 秒后自动开始。", NamedTextColor.GREEN)));
        updateLobbyDisplay();

        countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!countdownActive) {
                return;
            }
            if (lobbyPlayers.size() < requiredPlayers() || !hasAvailableRoom()) {
                cancelCountdown("人数不足或没有空闲房间，倒计时取消");
                return;
            }

            countdownSecondsLeft--;
            if (countdownSecondsLeft <= 0) {
                int task = countdownTaskId;
                countdownTaskId = -1;
                countdownActive = false;
                Bukkit.getScheduler().cancelTask(task);
                autoStartGame();
                return;
            }

            if (countdownSecondsLeft <= 5) {
                for (UUID uuid : lobbyPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
                    }
                }
            }
            updateLobbyDisplay();
        }, 20L, 20L).getTaskId();
    }

    private void cancelCountdown(String reason) {
        if (!countdownActive) {
            return;
        }
        cancelCountdownSilently();
        broadcastToLobby(prefix().append(Component.text(reason, NamedTextColor.RED)));
        updateLobbyDisplay();
    }

    private void cancelCountdownSilently() {
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
        countdownActive = false;
    }

    private void broadcastToLobby(Component message) {
        for (UUID uuid : lobbyPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    public void resetRoomAsync(World overworld) {
        if (overworld == null) {
            return;
        }
        String baseName = overworld.getName();
        if (resettingRooms.contains(baseName)) {
            return;
        }

        resettingRooms.add(baseName);
        roomWorlds.remove(overworld);

        String netherName = baseName + "_nether";
        String endName = baseName + "_end";

        World nether = Bukkit.getWorld(netherName);
        World end = Bukkit.getWorld(endName);

        File overworldFolder = overworld.getWorldFolder();
        File netherFolder = nether != null ? nether.getWorldFolder() : new File(Bukkit.getWorldContainer(), netherName);
        File endFolder = end != null ? end.getWorldFolder() : new File(Bukkit.getWorldContainer(), endName);

        // 先把还在这些世界里的玩家送回大厅
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            if (world.equals(overworld) || world.equals(nether) || world.equals(end)) {
                sendToLobby(player);
            }
        }

        // 卸载世界（必须主线程）
        boolean allUnloaded = true;
        if (nether != null) {
            allUnloaded &= Bukkit.unloadWorld(nether, false);
        }
        if (end != null) {
            allUnloaded &= Bukkit.unloadWorld(end, false);
        }
        if (overworld != null) {
            allUnloaded &= Bukkit.unloadWorld(overworld, false);
        }

        if (!allUnloaded) {
            resettingRooms.remove(baseName);
            World stillLoaded = Bukkit.getWorld(baseName);
            if (stillLoaded != null) {
                roomWorlds.add(stillLoaded);
            }
            plugin.getLogger().warning("房间 " + baseName + " 卸载失败，已取消本次重置。");
            updateLobbyDisplay();
            return;
        }

        plugin.getLogger().info("开始异步重置房间 " + baseName + " ...");

        // 异步删除世界文件夹，避免阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean deleted = deleteFolder(overworldFolder) & deleteFolder(netherFolder) & deleteFolder(endFolder);

            // 删除完成后回到主线程重新生成世界
            Bukkit.getScheduler().runTask(plugin, () -> {
                recreateRoom(baseName);
                if (deleted) {
                    plugin.getLogger().info("房间 " + baseName + " 重置完成（已使用新种子）。");
                } else {
                    plugin.getLogger().warning("房间 " + baseName + " 部分文件删除失败，已尝试重新生成。");
                }
            });
        });
    }

    private void recreateRoom(String baseName) {
        World overworld = new WorldCreator(baseName)
                .environment(World.Environment.NORMAL)
                .seed(random.nextLong())
                .generateStructures(true)
                .createWorld();
        if (overworld != null) {
            roomWorlds.add(overworld);
            prepareRoomWorld(overworld);
        }

        World nether = new WorldCreator(baseName + "_nether")
                .environment(World.Environment.NETHER)
                .seed(random.nextLong())
                .generateStructures(true)
                .createWorld();
        if (nether != null) {
            prepareRoomWorld(nether);
        }

        World end = new WorldCreator(baseName + "_end")
                .environment(World.Environment.THE_END)
                .seed(random.nextLong())
                .generateStructures(true)
                .createWorld();
        if (end != null) {
            prepareRoomWorld(end);
        }

        resettingRooms.remove(baseName);
        updateLobbyDisplay();
    }

    private boolean deleteFolder(File folder) {
        if (folder == null || !folder.exists()) {
            return true;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        return folder.delete();
    }

    public ItemStack createTeamItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("选择队伍", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("右键打开队伍选择界面", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bingo_item"), PersistentDataType.STRING, "team_selector");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createCardItem(BingoGame game, Team team) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("成就Bingo卡", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("队伍: " + team.displayName(), team.chatColor()).decoration(TextDecoration.ITALIC, false),
                    Component.text("得分: " + game.score(team) + "/" + plugin.getConfig().getInt("game.win-score", 13), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                    Component.text("右键查看成就板", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bingo_item"), PersistentDataType.STRING, "bingo_card");
            item.setItemMeta(meta);
        }
        return item;
    }

    public void refreshCardItem(Player player) {
        BingoGame game = getGame(player);
        if (game == null || game.teamOf(player) == null) {
            return;
        }
        // 不再使用纸物品；潜行+F 打开 GUI，因此这里只刷新已经打开的 GUI
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof BingoCardHolder holder
                && holder.game() == game && holder.viewer().getUniqueId().equals(player.getUniqueId())) {
            openBingoCard(player);
        }
    }

    public void openTeamSelection(Player player) {
        TeamSelectHolder holder = new TeamSelectHolder(player);
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("选择队伍", NamedTextColor.GOLD));
        holder.setInventory(inv);
        inv.setItem(2, teamIcon(Team.RED, "点击加入红队"));
        inv.setItem(4, new ItemStack(Material.BARRIER));
        inv.setItem(6, teamIcon(Team.BLUE, "点击加入蓝队"));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        player.openInventory(inv);
    }

    public void openBingoCard(Player player) {
        BingoGame game = getGame(player);
        if (game == null) {
            return;
        }
        Team team = game.teamOf(player);
        if (team == null) {
            return;
        }
        BingoCardHolder holder = new BingoCardHolder(game, player);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("成就Bingo - " + team.displayName(), team.chatColor()));
        holder.setInventory(inv);
        fillBorder(inv, team);
        BingoCard card = game.card();
        for (int i = 0; i < BingoCard.CELLS; i++) {
            inv.setItem(CARD_SLOTS[i], createCellItem(game, team, card, i));
        }
        inv.setItem(4, scoreItem(game, team));
        player.openInventory(inv);
    }

    private void fillBorder(Inventory inv, Team team) {
        Material border = team == Team.RED ? Material.RED_STAINED_GLASS_PANE : Material.BLUE_STAINED_GLASS_PANE;
        for (int slot = 0; slot < 54; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                ItemStack pane = new ItemStack(border);
                ItemMeta meta = pane.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
                    pane.setItemMeta(meta);
                }
                inv.setItem(slot, pane);
            }
        }
    }

    private ItemStack createCellItem(BingoGame game, Team team, BingoCard card, int index) {
        boolean done = game.isCompleted(team, index);
        Component name = card.nameAt(index);
        Component display;
        List<Component> lore = new ArrayList<>();

        ItemStack item = card.iconAt(index);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (done) {
                // 自己队伍完成：用队伍颜色标记
                display = Component.text("✔ ", team.chatColor()).append(name.color(team.chatColor()));
                lore.add(Component.text("已由你的队伍完成", team.chatColor()));
            } else {
                display = name.color(NamedTextColor.WHITE);
                lore.add(Component.text("尚未完成", NamedTextColor.GRAY));
            }
            meta.displayName(display.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack scoreItem(BingoGame game, Team team) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("得分: " + game.score(team) + "/" + plugin.getConfig().getInt("game.win-score", 13), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack teamIcon(Team team, String action) {
        Material material = team == Team.RED ? Material.RED_WOOL : Material.BLUE_WOOL;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(team.displayName(), team.chatColor()).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(action, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof TeamSelectHolder holder) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            Material type = event.getCurrentItem().getType();
            if (type == Material.RED_WOOL) {
                selectedTeams.put(player.getUniqueId(), Team.RED);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                player.sendMessage(prefix().append(Component.text("你已选择红队！", Team.RED.chatColor())));
            } else if (type == Material.BLUE_WOOL) {
                selectedTeams.put(player.getUniqueId(), Team.BLUE);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.8f);
                player.sendMessage(prefix().append(Component.text("你已选择蓝队！", Team.BLUE.chatColor())));
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof BingoCardHolder) {
            event.setCancelled(true);
            return;
        }

        // 防止在等待大厅/游戏中移动关键物品
        if (lobbyPlayers.contains(player.getUniqueId()) || getGame(player) != null) {
            if (isKeyItem(event.getCurrentItem()) || isKeyItem(event.getCursor())) {
                event.setCancelled(true);
            }
        }
    }

    public void handleInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (isTeamItem(item) && lobbyPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            openTeamSelection(player);
            return;
        }
        if (isCardItem(item) && getGame(player) != null) {
            event.setCancelled(true);
            openBingoCard(player);
        }
    }

    private boolean isKeyItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return isTeamItem(item) || isCardItem(item);
    }

    private boolean isTeamItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && "team_selector".equals(meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "bingo_item"), PersistentDataType.STRING));
    }

    private boolean isCardItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && "bingo_card".equals(meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "bingo_item"), PersistentDataType.STRING));
    }

    public void handlePortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World from = player.getWorld();
        String prefix = plugin.getConfig().getString("rooms.prefix", "bingo");

        if (from == null || !from.getName().startsWith(prefix + "_")) {
            return;
        }

        String fromName = from.getName();
        boolean isNether = from.getEnvironment() == World.Environment.NETHER;
        boolean isEnd = from.getEnvironment() == World.Environment.THE_END;
        boolean isOverworld = from.getEnvironment() == World.Environment.NORMAL;

        String baseName;
        if (isNether && fromName.endsWith("_nether")) {
            baseName = fromName.substring(0, fromName.length() - "_nether".length());
        } else if (isEnd && fromName.endsWith("_end")) {
            baseName = fromName.substring(0, fromName.length() - "_end".length());
        } else {
            baseName = fromName;
        }

        World target;
        Location destination;
        if (isOverworld) {
            if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                target = Bukkit.getWorld(fromName + "_end");
                destination = target == null ? null : safeLocation(target, target.getSpawnLocation().getX(), target.getSpawnLocation().getZ());
            } else {
                target = Bukkit.getWorld(fromName + "_nether");
                if (target == null) {
                    return;
                }
                // 直接传送到该房间地狱的安全出生点，避免生成在天花板上
                destination = target.getSpawnLocation().clone();
            }
        } else if (isNether) {
            target = Bukkit.getWorld(baseName);
            if (target == null) {
                return;
            }
            Location loc = player.getLocation();
            destination = safeLocation(target, loc.getX() * 8.0, loc.getZ() * 8.0);
        } else if (isEnd) {
            target = Bukkit.getWorld(baseName);
            if (target == null) {
                return;
            }
            destination = safeLocation(target, target.getSpawnLocation().getX(), target.getSpawnLocation().getZ());
        } else {
            return;
        }

        if (target == null || destination == null) {
            return;
        }
        destination.setY(Math.min(destination.getY(), target.getMaxHeight() - 5));
        event.setTo(destination);
        event.setCancelled(false);
    }

    private Component prefix() {
        String raw = plugin.getConfig().getString("messages.prefix", "&8[&6成就Bingo&8] ");
        return LegacyComponentSerializer.legacySection().deserialize(raw);
    }
}
