package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gerencia rankings de jogadores por diferentes critérios
 */
public class RankingManager {

    private final Main plugin;
    
    // Cache de rankings (atualizado periodicamente)
    private List<RankingEntry> mobsKilledRanking;
    private List<RankingEntry> bossesKilledRanking;
    private List<RankingEntry> dungeonsCompletedRanking;
    private List<RankingEntry> pointsRanking;
    private List<RankingEntry> levelRanking;
    
    private long lastUpdate = 0;
    private static final long CACHE_DURATION = 60000; // 1 minuto

    public RankingManager(Main plugin) {
        this.plugin = plugin;
        this.mobsKilledRanking = new ArrayList<>();
        this.bossesKilledRanking = new ArrayList<>();
        this.dungeonsCompletedRanking = new ArrayList<>();
        this.pointsRanking = new ArrayList<>();
        this.levelRanking = new ArrayList<>();
    }

    /**
     * Atualiza todos os rankings
     */
    public void updateRankings() {
        if (System.currentTimeMillis() - lastUpdate < CACHE_DURATION) {
            return; // Cache ainda válido
        }

        Map<UUID, PlayerData> allData = plugin.getPlayerDataManager().getAllData();
        
        List<RankingEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : allData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            String playerName = getPlayerName(uuid);
            entries.add(new RankingEntry(uuid, playerName, data));
        }

        // Mobs Killed
        mobsKilledRanking = entries.stream()
                .sorted((a, b) -> Integer.compare(b.getData().getMobsKilled(), a.getData().getMobsKilled()))
                .limit(100)
                .collect(Collectors.toList());

        // Bosses Killed
        bossesKilledRanking = entries.stream()
                .sorted((a, b) -> Integer.compare(b.getData().getBossesKilled(), a.getData().getBossesKilled()))
                .limit(100)
                .collect(Collectors.toList());

        // Dungeons Completed
        dungeonsCompletedRanking = entries.stream()
                .sorted((a, b) -> Integer.compare(b.getData().getDungeonsCompleted(), a.getData().getDungeonsCompleted()))
                .limit(100)
                .collect(Collectors.toList());

        // Points
        pointsRanking = entries.stream()
                .sorted((a, b) -> Integer.compare(b.getData().getPoints(), a.getData().getPoints()))
                .limit(100)
                .collect(Collectors.toList());

        // Level
        levelRanking = entries.stream()
                .sorted((a, b) -> Integer.compare(b.getData().getLevel(), a.getData().getLevel()))
                .limit(100)
                .collect(Collectors.toList());

        lastUpdate = System.currentTimeMillis();
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Unknown";
    }

    // ==================== GETTERS ====================

    public List<RankingEntry> getMobsKilledRanking(int limit) {
        updateRankings();
        return mobsKilledRanking.stream().limit(limit).collect(Collectors.toList());
    }

    public List<RankingEntry> getBossesKilledRanking(int limit) {
        updateRankings();
        return bossesKilledRanking.stream().limit(limit).collect(Collectors.toList());
    }

    public List<RankingEntry> getDungeonsCompletedRanking(int limit) {
        updateRankings();
        return dungeonsCompletedRanking.stream().limit(limit).collect(Collectors.toList());
    }

    public List<RankingEntry> getPointsRanking(int limit) {
        updateRankings();
        return pointsRanking.stream().limit(limit).collect(Collectors.toList());
    }

    public List<RankingEntry> getLevelRanking(int limit) {
        updateRankings();
        return levelRanking.stream().limit(limit).collect(Collectors.toList());
    }

    // ==================== POSIÇÕES ====================

    public int getPlayerPosition(UUID uuid, RankingType type) {
        updateRankings();
        List<RankingEntry> ranking = switch (type) {
            case MOBS_KILLED -> mobsKilledRanking;
            case BOSSES_KILLED -> bossesKilledRanking;
            case DUNGEONS_COMPLETED -> dungeonsCompletedRanking;
            case POINTS -> pointsRanking;
            case LEVEL -> levelRanking;
        };

        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1; // Não encontrado
    }

    public RankingEntry getPlayerEntry(UUID uuid, RankingType type) {
        updateRankings();
        List<RankingEntry> ranking = switch (type) {
            case MOBS_KILLED -> mobsKilledRanking;
            case BOSSES_KILLED -> bossesKilledRanking;
            case DUNGEONS_COMPLETED -> dungeonsCompletedRanking;
            case POINTS -> pointsRanking;
            case LEVEL -> levelRanking;
        };

        return ranking.stream()
                .filter(e -> e.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Força atualização imediata do cache
     */
    public void forceUpdate() {
        lastUpdate = 0;
        updateRankings();
    }

    // ==================== CLASSES INTERNAS ====================

    public enum RankingType {
        MOBS_KILLED,
        BOSSES_KILLED,
        DUNGEONS_COMPLETED,
        POINTS,
        LEVEL
    }

    public static class RankingEntry {
        private final UUID uuid;
        private final String playerName;
        private final PlayerData data;

        public RankingEntry(UUID uuid, String playerName, PlayerData data) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.data = data;
        }

        public UUID getUuid() { return uuid; }
        public String getPlayerName() { return playerName; }
        public PlayerData getData() { return data; }

        public int getValue(RankingType type) {
            return switch (type) {
                case MOBS_KILLED -> data.getMobsKilled();
                case BOSSES_KILLED -> data.getBossesKilled();
                case DUNGEONS_COMPLETED -> data.getDungeonsCompleted();
                case POINTS -> data.getPoints();
                case LEVEL -> data.getLevel();
            };
        }
    }
}
