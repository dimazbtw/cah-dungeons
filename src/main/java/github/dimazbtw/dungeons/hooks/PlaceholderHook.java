package github.dimazbtw.dungeons.hooks;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.dungeons.models.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderHook extends PlaceholderExpansion {

    private final Main plugin;

    public PlaceholderHook(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dungeons";
    }

    @Override
    public @NotNull String getAuthor() {
        return "dimazbtw";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);

        return switch (params.toLowerCase()) {
            // Player stats
            case "level" -> data != null ? String.valueOf(data.getLevel()) : "0";
            case "experience", "exp" -> data != null ? String.valueOf(data.getExperience()) : "0";
            case "points" -> data != null ? String.valueOf(data.getPoints()) : "0";
            case "dungeons_completed" -> data != null ? String.valueOf(data.getDungeonsCompleted()) : "0";
            case "dungeons_failed" -> data != null ? String.valueOf(data.getDungeonsFailed()) : "0";
            case "mobs_killed" -> data != null ? String.valueOf(data.getMobsKilled()) : "0";
            case "bosses_killed" -> data != null ? String.valueOf(data.getBossesKilled()) : "0";
            case "deaths" -> data != null ? String.valueOf(data.getDeaths()) : "0";
            case "playtime" -> data != null ? data.getFormattedPlayTime() : "0h 0m";
            case "highest_round" -> data != null ? String.valueOf(data.getHighestRound()) : "0";
            
            // Player attributes
            case "vitality" -> data != null ? String.valueOf(data.getVitality()) : "20";
            case "strength" -> data != null ? String.valueOf(data.getStrength()) : "0";
            case "agility" -> data != null ? String.valueOf(data.getAgility()) : "0";
            case "level_bar", "level_progress_bar" -> data != null ? data.getLevelProgressBar() : "■■■■■■";
            case "level_percentage" -> data != null ? String.format("%.1f%%", data.getLevelProgress() * 100) : "0%";

            // Session info
            case "in_dungeon" -> session != null ? "true" : "false";
            case "dungeon_name" -> session != null ? session.getDungeon().getId() : "None";
            case "dungeon_display" -> session != null ? session.getDungeon().getDisplayName() : "None";
            case "current_round" -> session != null ? String.valueOf(session.getCurrentRound()) : "0";
            case "max_rounds" -> session != null ? String.valueOf(session.getDungeon().getTotalRounds()) : "0";
            case "mobs_left" -> session != null ? String.valueOf(session.getMobsLeft()) : "0";
            case "session_mobs_killed" -> session != null ? String.valueOf(session.getMobsKilled()) : "0";
            case "session_duration" -> session != null ? session.getFormattedDuration() : "00:00";
            case "session_players" -> session != null ? String.valueOf(session.getPlayerCount()) : "0";
            case "session_alive_players" -> session != null ? String.valueOf(session.getAlivePlayerCount()) : "0";
            case "session_status" -> {
                if (session == null) yield "Not in dungeon";
                yield switch (session.getState()) {
                    case WAITING -> "Waiting";
                    case STARTING -> "Starting";
                    case IN_PROGRESS -> "In Progress";
                    case BOSS_ROUND -> "Boss Round";
                    case ENDED -> "Ended";
                };
            }

            // Global stats
            case "active_sessions" -> String.valueOf(plugin.getSessionManager().getActiveSessionCount());
            case "total_players_in_dungeons" -> String.valueOf(plugin.getSessionManager().getTotalPlayersInDungeons());
            case "total_dungeons" -> String.valueOf(plugin.getDungeonManager().getDungeonCount());

            // Enchant levels
            case "smite_level" -> data != null ? String.valueOf(data.getEnchantLevel("smite")) : "0";
            case "wisdom_level" -> data != null ? String.valueOf(data.getEnchantLevel("wisdom")) : "0";
            case "lifesteal_level" -> data != null ? String.valueOf(data.getEnchantLevel("lifesteal")) : "0";
            case "crit_level" -> data != null ? String.valueOf(data.getEnchantLevel("crit")) : "0";
            case "thor_level" -> data != null ? String.valueOf(data.getEnchantLevel("thor")) : "0";
            case "shockwave_level" -> data != null ? String.valueOf(data.getEnchantLevel("shockwave")) : "0";

            default -> null;
        };
    }
}
