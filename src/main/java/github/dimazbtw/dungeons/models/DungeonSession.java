package github.dimazbtw.dungeons.models;

import github.dimazbtw.dungeons.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonSession {

    private final String sessionId;
    private final Dungeon dungeon;
    private final Set<UUID> players;
    private final Set<UUID> deadPlayers;
    private final Set<UUID> activeMobs;
    private UUID activeBoss;

    private SessionState state;
    private int currentRound;
    private int mobsKilled;
    private int totalMobsSpawned;
    private long startTime;
    private long roundStartTime;

    // Tasks
    private BukkitTask countdownTask;
    private BukkitTask mobSpawnTask;
    private BukkitTask actionBarTask;

    // Stored player inventories
    private final Map<UUID, PlayerInventoryData> storedInventories;

    public DungeonSession(Dungeon dungeon) {
        this.sessionId = UUID.randomUUID().toString().substring(0, 8);
        this.dungeon = dungeon;
        this.players = ConcurrentHashMap.newKeySet();
        this.deadPlayers = ConcurrentHashMap.newKeySet();
        this.activeMobs = ConcurrentHashMap.newKeySet();
        this.storedInventories = new ConcurrentHashMap<>();
        this.state = SessionState.WAITING;
        this.currentRound = 0;
        this.mobsKilled = 0;
        this.totalMobsSpawned = 0;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Dungeon getDungeon() {
        return dungeon;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public Set<UUID> getDeadPlayers() {
        return deadPlayers;
    }

    public Set<UUID> getActivePlayers() {
        Set<UUID> active = new HashSet<>(players);
        active.removeAll(deadPlayers);
        return active;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getAlivePlayerCount() {
        return players.size() - deadPlayers.size();
    }

    public boolean addPlayer(Player player) {
        if (players.size() >= dungeon.getMaxPlayers()) {
            return false;
        }
        return players.add(player.getUniqueId());
    }

    public boolean removePlayer(Player player) {
        deadPlayers.remove(player.getUniqueId());
        return players.remove(player.getUniqueId());
    }

    public boolean hasPlayer(Player player) {
        return players.contains(player.getUniqueId());
    }

    public boolean isPlayerDead(Player player) {
        return deadPlayers.contains(player.getUniqueId());
    }

    public boolean isPlayerDead(UUID playerId) {
        return deadPlayers.contains(playerId);
    }

    public void markPlayerDead(Player player) {
        deadPlayers.add(player.getUniqueId());
    }

    public void revivePlayer(Player player) {
        deadPlayers.remove(player.getUniqueId());
    }

    /**
     * Revive todos os jogadores mortos (chamado entre rounds)
     */
    public void reviveAllDeadPlayers() {
        deadPlayers.clear();
    }

    /**
     * Retorna lista de jogadores vivos online
     */
    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (UUID uuid : players) {
            if (!deadPlayers.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    alive.add(player);
                }
            }
        }
        return alive;
    }

    /**
     * Retorna lista de jogadores mortos online
     */
    public List<Player> getDeadOnlinePlayers() {
        List<Player> dead = new ArrayList<>();
        for (UUID uuid : deadPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                dead.add(player);
            }
        }
        return dead;
    }

    // Mob management
    public Set<UUID> getActiveMobs() {
        return activeMobs;
    }

    public void addMob(Entity entity) {
        activeMobs.add(entity.getUniqueId());
    }

    public void removeMob(Entity entity) {
        activeMobs.remove(entity.getUniqueId());
    }

    public boolean isMobInSession(Entity entity) {
        return activeMobs.contains(entity.getUniqueId());
    }

    public int getAliveMobCount() {
        // Clean up dead/invalid mobs
        activeMobs.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || entity.isDead() || !entity.isValid();
        });
        return activeMobs.size();
    }

    public void clearMobs() {
        for (UUID mobId : new HashSet<>(activeMobs)) {
            Entity entity = Bukkit.getEntity(mobId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        activeMobs.clear();
    }

    // Boss management
    public UUID getActiveBoss() {
        return activeBoss;
    }

    public void setActiveBoss(Entity boss) {
        this.activeBoss = boss != null ? boss.getUniqueId() : null;
    }

    public boolean hasBoss() {
        return activeBoss != null;
    }

    public LivingEntity getBossEntity() {
        if (activeBoss == null) return null;
        Entity entity = Bukkit.getEntity(activeBoss);
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    public boolean isBoss(Entity entity) {
        return activeBoss != null && entity.getUniqueId().equals(activeBoss);
    }

    // State management
    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public boolean isWaiting() {
        return state == SessionState.WAITING;
    }

    public boolean isStarting() {
        return state == SessionState.STARTING;
    }

    public boolean isInProgress() {
        return state == SessionState.IN_PROGRESS;
    }

    public boolean isBossRound() {
        return state == SessionState.BOSS_ROUND;
    }

    public boolean isEnded() {
        return state == SessionState.ENDED;
    }

    // Round management
    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int round) {
        this.currentRound = round;
        this.roundStartTime = System.currentTimeMillis();
    }

    public void nextRound() {
        this.currentRound++;
        this.roundStartTime = System.currentTimeMillis();
    }

    public boolean hasMoreRounds() {
        if (!dungeon.isLimitedRounds()) return true;
        return currentRound < dungeon.getTotalRounds();
    }

    // Kill tracking
    public int getMobsKilled() {
        return mobsKilled;
    }

    public void incrementMobsKilled() {
        this.mobsKilled++;
    }

    public int getTotalMobsSpawned() {
        return totalMobsSpawned;
    }

    public void setTotalMobsSpawned(int total) {
        this.totalMobsSpawned = total;
    }

    public void addMobsSpawned(int count) {
        this.totalMobsSpawned += count;
    }

    public int getMobsLeft() {
        return getAliveMobCount();
    }

    // Time tracking
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getDuration() {
        if (startTime == 0) return 0;
        return System.currentTimeMillis() - startTime;
    }

    public String getFormattedDuration() {
        long duration = getDuration();
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Task management
    public BukkitTask getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(BukkitTask task) {
        cancelCountdownTask();
        this.countdownTask = task;
    }

    public void cancelCountdownTask() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
        countdownTask = null;
    }

    public BukkitTask getMobSpawnTask() {
        return mobSpawnTask;
    }

    public void setMobSpawnTask(BukkitTask task) {
        cancelMobSpawnTask();
        this.mobSpawnTask = task;
    }

    public void cancelMobSpawnTask() {
        if (mobSpawnTask != null && !mobSpawnTask.isCancelled()) {
            mobSpawnTask.cancel();
        }
        mobSpawnTask = null;
    }

    public BukkitTask getActionBarTask() {
        return actionBarTask;
    }

    public void setActionBarTask(BukkitTask task) {
        cancelActionBarTask();
        this.actionBarTask = task;
    }

    public void cancelActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
        }
        actionBarTask = null;
    }

    public void cancelAllTasks() {
        cancelCountdownTask();
        cancelMobSpawnTask();
        cancelActionBarTask();
    }

    // Inventory management
    public Map<UUID, PlayerInventoryData> getStoredInventories() {
        return storedInventories;
    }

    public void storeInventory(Player player, PlayerInventoryData data) {
        storedInventories.put(player.getUniqueId(), data);
    }

    public PlayerInventoryData getStoredInventory(Player player) {
        return storedInventories.get(player.getUniqueId());
    }

    public void removeStoredInventory(Player player) {
        storedInventories.remove(player.getUniqueId());
    }

    // Broadcast to all players
    public void broadcast(String message) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    public List<Player> getOnlinePlayers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }

    public enum SessionState {
        WAITING,
        STARTING,
        IN_PROGRESS,
        BOSS_ROUND,
        ENDED
    }
}
