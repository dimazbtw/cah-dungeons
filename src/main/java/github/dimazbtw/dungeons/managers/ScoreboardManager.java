package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final Main plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private final Map<UUID, BukkitTask> titleAnimationTasks;
    private final Map<UUID, BukkitTask> updateTasks;

    // Color codes for unique empty lines
    private static final String[] EMPTY_LINE_COLORS = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f",
            "§0§r", "§1§r", "§2§r", "§3§r", "§4§r", "§5§r"
    };

    private List<String> animatedTitles;
    private int titleAnimationInterval;
    private int updateInterval;
    private boolean animateTitle;

    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new ConcurrentHashMap<>();
        this.titleAnimationTasks = new ConcurrentHashMap<>();
        this.updateTasks = new ConcurrentHashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        animatedTitles = plugin.getConfig().getStringList("scoreboards.title").stream()
                .map(ColorUtils::colorize)
                .toList();
        titleAnimationInterval = plugin.getConfig().getInt("scoreboards.title-animation-interval", 10);
        updateInterval = plugin.getConfig().getInt("scoreboards.update-interval", 20);
        animateTitle = plugin.getConfig().getBoolean("scoreboards.animate-title", true);

        if (animatedTitles.isEmpty()) {
            animatedTitles = List.of(ColorUtils.colorize("&a&lDUNGEON"));
        }
    }

    public void applyWaitingScoreboard(Player player, DungeonSession session) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("dungeon", Criteria.DUMMY, animatedTitles.get(0));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), scoreboard);

        updateWaitingScoreboard(player, session, 0);

        if (animateTitle && animatedTitles.size() > 1) {
            startTitleAnimation(player, objective);
        }
    }

    public void updateWaitingScoreboard(Player player, DungeonSession session, int countdown) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        // Clear existing entries
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        List<String> lines = plugin.getConfig().getStringList("scoreboards.waiting");
        String status = session.isStarting()
                ? plugin.getConfig().getString("scoreboards.status.starting", "&fStarting in &a{time}")
                        .replace("{time}", String.valueOf(countdown))
                : plugin.getConfig().getString("scoreboards.status.waiting", "&eWaiting for players");

        int emptyLineIndex = 0;
        int score = lines.size();

        for (String line : lines) {
            String processed = processPlaceholders(line, session, player)
                    .replace("{status}", ColorUtils.colorize(status));
            processed = ColorUtils.colorize(processed);

            // Handle empty lines with unique color codes
            if (processed.isEmpty() || processed.equals(" ") || processed.equals("")) {
                processed = getUniqueEmptyLine(emptyLineIndex++);
            }

            // Ensure line is unique (max 40 chars for scoreboard)
            if (processed.length() > 40) {
                processed = processed.substring(0, 40);
            }

            objective.getScore(processed).setScore(score--);
        }
    }

    public void applyInGameScoreboard(Player player, DungeonSession session) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("dungeon", Criteria.DUMMY, animatedTitles.get(0));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(scoreboard);
            playerScoreboards.put(player.getUniqueId(), scoreboard);

            if (animateTitle && animatedTitles.size() > 1) {
                startTitleAnimation(player, objective);
            }
        }

        updateInGameScoreboard(player, session);
        startUpdateTask(player, session);
    }

    public void updateInGameScoreboard(Player player, DungeonSession session) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        // Clear existing entries
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        List<String> lines = plugin.getConfig().getStringList("scoreboards.in-game");

        int emptyLineIndex = 0;
        int score = lines.size();

        for (String line : lines) {
            String processed = processPlaceholders(line, session, player);
            processed = ColorUtils.colorize(processed);

            // Handle empty lines with unique color codes
            if (processed.isEmpty() || processed.equals(" ") || processed.equals("")) {
                processed = getUniqueEmptyLine(emptyLineIndex++);
            }

            // Ensure line is unique (max 40 chars for scoreboard)
            if (processed.length() > 40) {
                processed = processed.substring(0, 40);
            }

            objective.getScore(processed).setScore(score--);
        }
    }

    private String getUniqueEmptyLine(int index) {
        if (index < EMPTY_LINE_COLORS.length) {
            return EMPTY_LINE_COLORS[index];
        }
        // Generate more unique combinations if needed
        return "§" + Integer.toHexString(index % 16) + "§r§" + Integer.toHexString((index / 16) % 16);
    }

    private String processPlaceholders(String line, DungeonSession session, Player player) {
        String maxRounds = session.getDungeon().isLimitedRounds() 
                ? String.valueOf(session.getDungeon().getTotalRounds()) 
                : "∞";
        
        return line
                .replace("{dungeon_display}", session.getDungeon().getDisplayName())
                .replace("{players}", String.valueOf(session.getPlayerCount()))
                .replace("{max_players}", String.valueOf(session.getDungeon().getMaxPlayers()))
                .replace("{round}", String.valueOf(session.getCurrentRound()))
                .replace("{max_rounds}", maxRounds)
                .replace("{mobs_left}", String.valueOf(session.getMobsLeft()))
                .replace("{mobs_killed}", String.valueOf(session.getMobsKilled()))
                .replace("{duration}", session.getFormattedDuration())
                .replace("{player}", player.getName());
    }

    private void startTitleAnimation(Player player, Objective objective) {
        // Cancel existing task if any
        BukkitTask existingTask = titleAnimationTasks.get(player.getUniqueId());
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !playerScoreboards.containsKey(player.getUniqueId())) {
                    cancel();
                    titleAnimationTasks.remove(player.getUniqueId());
                    return;
                }

                try {
                    objective.setDisplayName(animatedTitles.get(index));
                    index = (index + 1) % animatedTitles.size();
                } catch (Exception e) {
                    cancel();
                    titleAnimationTasks.remove(player.getUniqueId());
                }
            }
        }.runTaskTimer(plugin, 0L, titleAnimationInterval);

        titleAnimationTasks.put(player.getUniqueId(), task);
    }

    private void startUpdateTask(Player player, DungeonSession session) {
        // Cancel existing task if any
        BukkitTask existingTask = updateTasks.get(player.getUniqueId());
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !playerScoreboards.containsKey(player.getUniqueId())) {
                    cancel();
                    updateTasks.remove(player.getUniqueId());
                    return;
                }

                DungeonSession currentSession = plugin.getSessionManager().getPlayerSession(player);
                if (currentSession == null || currentSession.isEnded()) {
                    cancel();
                    updateTasks.remove(player.getUniqueId());
                    return;
                }

                updateInGameScoreboard(player, currentSession);
            }
        }.runTaskTimer(plugin, updateInterval, updateInterval);

        updateTasks.put(player.getUniqueId(), task);
    }

    public void removeScoreboard(Player player) {
        // Cancel title animation task
        BukkitTask titleTask = titleAnimationTasks.remove(player.getUniqueId());
        if (titleTask != null && !titleTask.isCancelled()) {
            titleTask.cancel();
        }

        // Cancel update task
        BukkitTask updateTask = updateTasks.remove(player.getUniqueId());
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void reload() {
        loadConfig();
    }
}
