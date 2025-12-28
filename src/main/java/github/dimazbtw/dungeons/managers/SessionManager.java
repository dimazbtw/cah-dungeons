package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.*;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Main plugin;
    private final Map<String, DungeonSession> sessions;
    private final Map<String, DungeonSession> dungeonSessions;
    private final Map<UUID, DungeonSession> playerSessions;

    public SessionManager(Main plugin) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
        this.dungeonSessions = new ConcurrentHashMap<>();
        this.playerSessions = new ConcurrentHashMap<>();
    }

    public DungeonSession createSession(Dungeon dungeon) {
        if (dungeonSessions.containsKey(dungeon.getId())) {
            return dungeonSessions.get(dungeon.getId());
        }

        DungeonSession session = new DungeonSession(dungeon);
        sessions.put(session.getSessionId(), session);
        dungeonSessions.put(dungeon.getId(), session);

        return session;
    }

    public boolean joinSession(Player player, DungeonSession session) {
        if (playerSessions.containsKey(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "already-in-dungeon");
            return false;
        }

        if (!session.isWaiting() && !session.isStarting()) {
            plugin.getMessageManager().sendMessage(player, "dungeon-in-progress");
            return false;
        }

        Dungeon dungeon = session.getDungeon();

        // Verificar se a dungeon está aberta (schedule)
        if (!dungeon.isOpen()) {
            player.sendMessage(ColorUtils.colorize(dungeon.getClosedMessage()));
            return false;
        }

        if (session.getPlayerCount() >= dungeon.getMaxPlayers()) {
            plugin.getMessageManager().sendMessage(player, "dungeon-full");
            return false;
        }

        // Store player inventory
        if (plugin.getConfig().getBoolean("dungeon-settings.save-inventory", true)) {
            session.storeInventory(player, new PlayerInventoryData(player));
        }

        // Clear inventory
        if (plugin.getConfig().getBoolean("dungeon-settings.clear-inventory", false)) {
            player.getInventory().clear();
        }

        // Give weapon
        if (plugin.getConfig().getBoolean("dungeon-settings.give-weapon", true)) {
            plugin.getWeaponManager().giveWeapon(player);
        }

        // Apply dungeon equipment (armor + extra items)
        plugin.getEquipmentManager().applyEquipment(player);

        // Give custom items
        giveCustomItems(player);

        // Teleport to entry
        Location entryLoc = dungeon.getRandomEntryLocation();
        if (entryLoc != null) {
            player.teleport(entryLoc);
        }

        session.addPlayer(player);
        playerSessions.put(player.getUniqueId(), session);

        // Send messages
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("dungeon", dungeon.getDisplayName());
        plugin.getMessageManager().sendMessage(player, "dungeon-joined", placeholders);

        // Broadcast
        Map<String, String> broadcastPlaceholders = new HashMap<>();
        broadcastPlaceholders.put("player", player.getName());
        broadcastPlaceholders.put("current", String.valueOf(session.getPlayerCount()));
        broadcastPlaceholders.put("max", String.valueOf(dungeon.getMaxPlayers()));

        String joinMessage = plugin.getMessageManager().getMessageWithPrefix("player-joined", broadcastPlaceholders);
        session.broadcast(joinMessage);

        // Apply scoreboard
        plugin.getScoreboardManager().applyWaitingScoreboard(player, session);

        // Check start
        checkStart(session);

        return true;
    }

    private void giveCustomItems(Player player) {
        var itemSection = plugin.getConfig().getConfigurationSection("custom-items.leave-item");
        if (itemSection != null) {
            String materialStr = itemSection.getString("material", "REDSTONE");
            String name = ColorUtils.colorize(itemSection.getString("name", "&cLeave"));
            List<String> lore = itemSection.getStringList("lore").stream()
                    .map(ColorUtils::colorize)
                    .toList();
            int slot = itemSection.getInt("default-slot", 8);

            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.valueOf(materialStr.toUpperCase())
            );
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(new ArrayList<>(lore));
                item.setItemMeta(meta);
            }

            player.getInventory().setItem(slot, item);
        }
    }

    public void leaveSession(Player player) {
        DungeonSession session = playerSessions.remove(player.getUniqueId());
        if (session == null) return;

        // Verificar se o jogador está morto (perdeu equipamento) ou vivo (sincronizar)
        boolean playerDied = session.isPlayerDead(player.getUniqueId());
        
        session.removePlayer(player);

        // Remove from boss bar
        plugin.getBossBarManager().removePlayerFromBossBar(session, player);

        // Reset player stats (vitality, strength, agility) para valores vanilla
        github.dimazbtw.dungeons.menus.PerksMenu.resetStats(player);

        // Sincronizar equipamento se jogador NÃO morreu (consumíveis usados)
        // Se morreu, o equipamento já foi limpo
        if (!playerDied) {
            plugin.getEquipmentManager().syncEquipmentFromInventory(player);
        }

        // Restore inventory
        PlayerInventoryData storedData = session.getStoredInventory(player);
        if (storedData != null) {
            storedData.restore(player);
            session.removeStoredInventory(player);
        }

        // Garantir vida máxima vanilla após restaurar inventário
        player.setHealth(Math.min(player.getHealth(), 20.0));

        // Teleport to exit
        Location exitLoc = session.getDungeon().getExitLocation();
        if (exitLoc != null) {
            player.teleport(exitLoc);
        }

        // Remove scoreboard
        plugin.getScoreboardManager().removeScoreboard(player);

        plugin.getMessageManager().sendMessage(player, "dungeon-left");

        // Broadcast
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("current", String.valueOf(session.getPlayerCount()));
        placeholders.put("max", String.valueOf(session.getDungeon().getMaxPlayers()));

        String leaveMessage = plugin.getMessageManager().getMessageWithPrefix("player-left", placeholders);
        session.broadcast(leaveMessage);

        // Check session state
        if (session.getPlayerCount() == 0) {
            endSession(session, false);
        } else if (session.isStarting() && session.getPlayerCount() < session.getDungeon().getMinPlayers()) {
            session.cancelCountdownTask();
            session.setState(DungeonSession.SessionState.WAITING);
            session.broadcast(plugin.getMessageManager().getMessageWithPrefix("starting-cancelled"));
        } else if (session.isInProgress() && session.getAlivePlayerCount() == 0) {
            endSession(session, false);
        }
    }

    private void checkStart(DungeonSession session) {
        Dungeon dungeon = session.getDungeon();

        if (session.getPlayerCount() >= dungeon.getMinPlayers() && session.isWaiting()) {
            startCountdown(session);
        }
    }

    private void startCountdown(DungeonSession session) {
        session.setState(DungeonSession.SessionState.STARTING);

        int countdown = plugin.getConfig().getInt("dungeon-settings.start-countdown", 10);

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = countdown;

            @Override
            public void run() {
                if (session.isEnded()) {
                    cancel();
                    return;
                }

                if (session.getPlayerCount() < session.getDungeon().getMinPlayers()) {
                    session.setState(DungeonSession.SessionState.WAITING);
                    session.broadcast(plugin.getMessageManager().getMessageWithPrefix("starting-cancelled"));
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    startDungeon(session);
                    cancel();
                    return;
                }

                // Update scoreboards
                for (Player player : session.getOnlinePlayers()) {
                    plugin.getScoreboardManager().updateWaitingScoreboard(player, session, timeLeft);
                }

                // Send countdown
                if (timeLeft <= 5 || timeLeft == 10 || timeLeft == 30) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", String.valueOf(timeLeft));
                    session.broadcast(plugin.getMessageManager().getMessageWithPrefix("starting-in", placeholders));

                    // Play countdown sound
                    for (Player player : session.getOnlinePlayers()) {
                        plugin.getMessageManager().playSound(player, "countdown", 1.0f, 1.0f + (0.1f * (5 - timeLeft)));
                    }
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        session.setCountdownTask(task);
    }

    private void startDungeon(DungeonSession session) {
        session.setState(DungeonSession.SessionState.IN_PROGRESS);
        session.setStartTime(System.currentTimeMillis());
        session.setCurrentRound(0);

        Dungeon dungeon = session.getDungeon();

        // Teleportar jogadores para spawn de entrada
        List<Location> entryLocations = dungeon.getEntryLocations();
        int locIndex = 0;
        
        for (Player player : session.getOnlinePlayers()) {
            Location entryLoc;
            if (!entryLocations.isEmpty()) {
                // Distribuir jogadores pelas localizações de entrada
                entryLoc = entryLocations.get(locIndex % entryLocations.size());
                locIndex++;
            } else {
                entryLoc = dungeon.getRandomEntryLocation();
            }
            
            if (entryLoc != null) {
                player.teleport(entryLoc);
            }
            
            // Aplicar stats de vitalidade/força/agilidade APENAS quando dungeon começa
            PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
            if (data != null) {
                github.dimazbtw.dungeons.menus.PerksMenu.applyStats(player, data);
                // Curar para vida máxima com vitality
                player.setHealth(data.getMaxHealth());
            }
            
            // Send title and apply scoreboard
            plugin.getMessageManager().sendTitle(player, "dungeon-start");
            plugin.getScoreboardManager().applyInGameScoreboard(player, session);
        }

        session.broadcast(plugin.getMessageManager().getMessageWithPrefix("dungeon-started"));

        // Start first round
        Bukkit.getScheduler().runTaskLater(plugin, () -> startNextRound(session), 40L);

        // Start action bar task
        startActionBarTask(session);
    }

    public void startNextRound(DungeonSession session) {
        if (session.isEnded()) return;

        // Reviver jogadores mortos antes de começar nova ronda
        reviveDeadPlayers(session);

        session.nextRound();
        Dungeon dungeon = session.getDungeon();

        // Check if complete
        if (dungeon.isLimitedRounds() && session.getCurrentRound() > dungeon.getTotalRounds()) {
            completeDungeon(session);
            return;
        }

        RoundConfig roundConfig = dungeon.getRound(session.getCurrentRound());

        // Send round title
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("round", String.valueOf(session.getCurrentRound()));

        for (Player player : session.getOnlinePlayers()) {
            plugin.getMessageManager().sendTitle(player, "round-start", placeholders);
        }

        session.broadcast(plugin.getMessageManager().getMessageWithPrefix("round-start", placeholders));

        // Spawn mobs
        if (roundConfig != null) {
            spawnRoundMobs(session, roundConfig);
        } else if (!dungeon.isLimitedRounds()) {
            spawnUnlimitedMobs(session);
        }
    }

    private void spawnRoundMobs(DungeonSession session, RoundConfig roundConfig) {
        Dungeon dungeon = session.getDungeon();
        int mobLimit = plugin.getDungeonManager().getMobLimit(dungeon.getId());

        for (MobSpawnConfig mobConfig : roundConfig.getMobs()) {
            int toSpawn = Math.min(mobConfig.getCount(), mobLimit - session.getAliveMobCount());

            for (int i = 0; i < toSpawn; i++) {
                Location spawnLoc = dungeon.getRandomMobSpawnLocation();
                if (spawnLoc != null) {
                    final int index = i;
                    long delay = (long) (mobConfig.getSpawnDelay() * 20 * index);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!session.isEnded()) {
                            plugin.getMobManager().spawnMob(mobConfig.getMobId(), spawnLoc, session);
                            session.addMobsSpawned(1);
                        }
                    }, delay);
                }
            }
        }
    }

    private void spawnUnlimitedMobs(DungeonSession session) {
        Dungeon dungeon = session.getDungeon();
        int mobLimit = plugin.getDungeonManager().getMobLimit(dungeon.getId());

        for (MobSpawnConfig mobConfig : dungeon.getUnlimitedMobs()) {
            int toSpawn = Math.min(mobConfig.getCount(), mobLimit - session.getAliveMobCount());

            for (int i = 0; i < toSpawn; i++) {
                Location spawnLoc = dungeon.getRandomMobSpawnLocation();
                if (spawnLoc != null) {
                    plugin.getMobManager().spawnMob(mobConfig.getMobId(), spawnLoc, session);
                    session.addMobsSpawned(1);
                }
            }
        }

        // Check for boss spawn in unlimited
        if (dungeon.isSpawnBossInUnlimited() && session.getCurrentRound() == dungeon.getBossSpawnRound()) {
            session.setState(DungeonSession.SessionState.BOSS_ROUND);
            plugin.getBossManager().getBossIds().stream()
                    .findFirst()
                    .ifPresent(bossId -> spawnBoss(session, bossId));
        }
    }

    private void spawnBoss(DungeonSession session, String bossId) {
        Dungeon dungeon = session.getDungeon();
        Location bossSpawn = dungeon.getBossSpawnLocation();

        if (bossSpawn == null) {
            bossSpawn = dungeon.getRandomMobSpawnLocation();
        }

        if (bossSpawn != null) {
            // Announce boss spawning
            session.broadcast(plugin.getMessageManager().getMessageWithPrefix("boss-spawning"));

            // Play boss spawn sound
            for (Player player : session.getOnlinePlayers()) {
                plugin.getMessageManager().playSound(player, "boss-spawn");
            }

            // Spawn boss
            LivingEntity boss = plugin.getBossManager().spawnBoss(bossId, bossSpawn, session);

            if (boss != null) {
                var bossConfig = plugin.getBossManager().getBoss(bossId);
                String bossName = bossConfig != null ? bossConfig.getDisplayName() : bossId;

                // Create boss bar
                plugin.getBossBarManager().createBossBar(session, boss, bossName);

                // Send boss spawn title
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("boss_name", bossName);

                for (Player player : session.getOnlinePlayers()) {
                    plugin.getMessageManager().sendTitle(player, "boss-spawn", placeholders);
                }

                session.broadcast(plugin.getMessageManager().getMessageWithPrefix("boss-spawned"));
            }
        }
    }

    public void onMobKilled(DungeonSession session, boolean isBoss) {
        session.incrementMobsKilled();

        // Update scoreboards
        for (Player player : session.getOnlinePlayers()) {
            plugin.getScoreboardManager().updateInGameScoreboard(player, session);
        }

        if (isBoss) {
            session.setActiveBoss(null);
            session.setState(DungeonSession.SessionState.IN_PROGRESS);

            // Remove boss bar
            plugin.getBossBarManager().removeBossBar(session);

            session.broadcast(plugin.getMessageManager().getMessageWithPrefix("boss-defeated"));

            // Play boss defeated sound
            for (Player player : session.getOnlinePlayers()) {
                plugin.getMessageManager().playSound(player, "boss-defeated");
            }

            // Check if dungeon complete
            Dungeon dungeon = session.getDungeon();
            if (dungeon.isLimitedRounds() && session.getCurrentRound() >= dungeon.getTotalRounds()) {
                completeDungeon(session);
                return;
            }

            // Wait and start next round
            int roundInterval = plugin.getConfig().getInt("dungeon-settings.round-interval", 5);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!session.isEnded()) {
                    startNextRound(session);
                }
            }, roundInterval * 20L);
            return;
        }

        // Check if all mobs are dead
        if (session.getAliveMobCount() == 0 && !session.hasBoss()) {
            session.broadcast(plugin.getMessageManager().getMessageWithPrefix("all-mobs-killed"));

            // Play round complete sound
            for (Player player : session.getOnlinePlayers()) {
                plugin.getMessageManager().playSound(player, "round-complete");
            }

            // Check if current round has a boss to spawn
            Dungeon dungeon = session.getDungeon();
            RoundConfig roundConfig = dungeon.getRound(session.getCurrentRound());

            if (roundConfig != null && roundConfig.hasBoss() && !session.isBossRound()) {
                // Spawn boss after all mobs are killed
                session.setState(DungeonSession.SessionState.BOSS_ROUND);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!session.isEnded()) {
                        spawnBoss(session, roundConfig.getBossId());
                    }
                }, 40L);
                return;
            }

            // No boss, wait and start next round
            int roundInterval = plugin.getConfig().getInt("dungeon-settings.round-interval", 5);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!session.isEnded()) {
                    startNextRound(session);
                }
            }, roundInterval * 20L);
        }
    }

    public void onPlayerDeath(Player player, DungeonSession session) {
        session.markPlayerDead(player);

        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data != null) {
            data.incrementDeaths();
        }

        // Limpar equipamento de dungeon (jogador perde ao morrer)
        plugin.getEquipmentManager().clearEquipment(player.getUniqueId());

        // Curar jogador e colocar em modo espectador
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);

        // Broadcast
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        session.broadcast(plugin.getMessageManager().getMessageWithPrefix("player-died", placeholders));

        // Send title and sound
        plugin.getMessageManager().sendTitle(player, "player-death");
        plugin.getMessageManager().playSound(player, "player-death");

        // Notificar que perdeu equipamento e está em espectador
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("equipment-lost"));
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("spectator-mode"));

        // Check if all dead
        if (session.getAlivePlayerCount() == 0) {
            endSession(session, false);
        }
    }

    /**
     * Revive todos os jogadores mortos (chamado entre rounds)
     */
    public void reviveDeadPlayers(DungeonSession session) {
        List<Player> deadPlayers = session.getDeadOnlinePlayers();
        
        if (deadPlayers.isEmpty()) return;

        for (Player player : deadPlayers) {
            // Tirar do modo espectador
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            
            // Curar
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            
            // Teleportar para entrada
            Location entryLoc = session.getDungeon().getRandomEntryLocation();
            if (entryLoc != null) {
                player.teleport(entryLoc);
            }
            
            // Dar arma novamente
            if (plugin.getConfig().getBoolean("dungeon-settings.give-weapon", true)) {
                plugin.getWeaponManager().giveWeapon(player);
            }
            
            // Aplicar stats
            PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
            if (data != null) {
                github.dimazbtw.dungeons.menus.PerksMenu.applyStats(player, data);
            }
            
            // Notificar
            plugin.getMessageManager().sendTitle(player, "player-revived");
            plugin.getMessageManager().playSound(player, "player-revived");
        }
        
        // Limpar lista de mortos
        session.reviveAllDeadPlayers();
        
        // Notificar todos
        if (!deadPlayers.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(deadPlayers.size()));
            session.broadcast(plugin.getMessageManager().getMessageWithPrefix("players-revived", placeholders));
        }
    }

    private void completeDungeon(DungeonSession session) {
        session.setState(DungeonSession.SessionState.ENDED);
        session.cancelAllTasks();

        // Clear mobs and boss bar
        plugin.getMobManager().clearSessionMobs(session);
        plugin.getBossBarManager().removeBossBar(session);

        // Send victory and process rewards
        for (Player player : session.getOnlinePlayers()) {
            plugin.getMessageManager().sendTitle(player, "dungeon-complete");
            plugin.getMessageManager().playSound(player, "dungeon-complete");

            // Process rewards (goes to pending rewards)
            plugin.getRewardManager().processDungeonRewards(player, session);

            PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
            if (data != null) {
                data.incrementDungeonsCompleted();
                data.setHighestRound(session.getCurrentRound());
                data.addPlayTime(session.getDuration());
            }
        }

        session.broadcast(plugin.getMessageManager().getMessageWithPrefix("dungeon-completed"));

        // Cleanup after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupSession(session), 100L);
    }

    public void endSession(DungeonSession session, boolean completed) {
        if (session.isEnded()) return;

        session.setState(DungeonSession.SessionState.ENDED);
        session.cancelAllTasks();

        // Clear mobs and boss bar
        plugin.getMobManager().clearSessionMobs(session);
        plugin.getBossBarManager().removeBossBar(session);

        if (!completed) {
            for (Player player : session.getOnlinePlayers()) {
                plugin.getMessageManager().sendTitle(player, "dungeon-failed");
                plugin.getMessageManager().playSound(player, "dungeon-failed");

                PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (data != null) {
                    data.incrementDungeonsFailed();
                    data.addPlayTime(session.getDuration());
                }
            }

            session.broadcast(plugin.getMessageManager().getMessageWithPrefix("dungeon-failed"));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupSession(session), 60L);
    }

    private void cleanupSession(DungeonSession session) {
        for (UUID playerId : new HashSet<>(session.getPlayers())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                // Restaurar modo de jogo para survival (caso esteja em espectador)
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                
                // Verificar se o jogador morreu (perdeu equipamento) ou sobreviveu
                boolean playerDied = session.isPlayerDead(playerId);
                
                // Sincronizar equipamento se jogador NÃO morreu (consumíveis usados)
                if (!playerDied) {
                    plugin.getEquipmentManager().syncEquipmentFromInventory(player);
                }
                
                // Restaurar inventário original
                PlayerInventoryData storedData = session.getStoredInventory(player);
                if (storedData != null) {
                    storedData.restore(player);
                }

                // Resetar stats para valores vanilla (sem vitalidade)
                github.dimazbtw.dungeons.menus.PerksMenu.resetStats(player);
                
                // Garantir vida máxima vanilla
                player.setHealth(Math.min(player.getHealth(), 20.0));

                // Teleportar para saída
                Location exitLoc = session.getDungeon().getExitLocation();
                if (exitLoc != null) {
                    player.teleport(exitLoc);
                }

                // Remover scoreboard
                plugin.getScoreboardManager().removeScoreboard(player);
            }
            playerSessions.remove(playerId);
        }

        sessions.remove(session.getSessionId());
        dungeonSessions.remove(session.getDungeon().getId());
    }

    private void startActionBarTask(DungeonSession session) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (session.isEnded()) {
                    cancel();
                    return;
                }

                for (Player player : session.getOnlinePlayers()) {
                    // Placeholders básicos
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("dungeon", session.getDungeon().getDisplayName());
                    placeholders.put("round", String.valueOf(session.getCurrentRound()));
                    placeholders.put("mobs_left", String.valueOf(session.getMobsLeft()));
                    placeholders.put("player", player.getName());

                    // Placeholders de atributos (da espada)
                    PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
                    
                    // Placeholders de vida - usa max_health do PlayerData se disponível
                    double maxHealth = data != null ? data.getMaxHealth() : player.getMaxHealth();
                    placeholders.put("health", String.format("%.1f", player.getHealth()));
                    placeholders.put("max_health", String.format("%.1f", maxHealth));
                    placeholders.put("health_percent", String.format("%.0f", (player.getHealth() / maxHealth) * 100));

                    if (data != null) {
                        // Atributos base
                        placeholders.put("vitality", String.valueOf(data.getVitality()));
                        placeholders.put("strength", String.valueOf(data.getStrength()));
                        placeholders.put("agility", String.valueOf(data.getAgility()));
                        
                        // Encantamentos
                        placeholders.put("smite", String.valueOf(data.getEnchantLevel("smite")));
                        placeholders.put("wisdom", String.valueOf(data.getEnchantLevel("wisdom")));
                        placeholders.put("crit", String.valueOf(data.getEnchantLevel("crit")));
                        placeholders.put("thor", String.valueOf(data.getEnchantLevel("thor")));
                        placeholders.put("lifesteal", String.valueOf(data.getEnchantLevel("lifesteal")));
                        placeholders.put("shockwave", String.valueOf(data.getEnchantLevel("shockwave")));
                        
                        // Estatísticas
                        placeholders.put("xp", String.valueOf(data.getExperience()));
                        placeholders.put("points", String.valueOf(data.getPoints()));
                        placeholders.put("level", String.valueOf(data.getLevel()));
                        placeholders.put("kills", String.valueOf(data.getMobsKilled()));
                    }

                    // Bônus calculados da arma
                    double smiteBonus = plugin.getWeaponManager().getSmiteDamageBonus(player);
                    double wisdomBonus = plugin.getWeaponManager().getWisdomBonus(player);
                    placeholders.put("smite_bonus", String.format("%.1f", smiteBonus));
                    placeholders.put("wisdom_bonus", String.format("%.1f", wisdomBonus));

                    String actionBar = plugin.getMessageManager().getActionBar("in-dungeon", placeholders);

                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionBar));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        session.setActionBarTask(task);
    }

    public void endAllSessions() {
        for (DungeonSession session : new ArrayList<>(sessions.values())) {
            endSession(session, false);
        }
    }

    // Getters
    public DungeonSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public DungeonSession getSessionByDungeon(String dungeonId) {
        return dungeonSessions.get(dungeonId);
    }

    public DungeonSession getPlayerSession(Player player) {
        return playerSessions.get(player.getUniqueId());
    }

    public DungeonSession getPlayerSession(UUID playerId) {
        return playerSessions.get(playerId);
    }

    public boolean isInDungeon(Player player) {
        return playerSessions.containsKey(player.getUniqueId());
    }

    public Collection<DungeonSession> getAllSessions() {
        return sessions.values();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public int getTotalPlayersInDungeons() {
        return playerSessions.size();
    }
}
