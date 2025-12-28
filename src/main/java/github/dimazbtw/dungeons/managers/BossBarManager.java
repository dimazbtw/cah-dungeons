package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonSession;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {

    private final Main plugin;
    private final Map<String, BossBar> sessionBossBars; // sessionId -> BossBar
    private final Map<String, BukkitTask> updateTasks;

    public BossBarManager(Main plugin) {
        this.plugin = plugin;
        this.sessionBossBars = new ConcurrentHashMap<>();
        this.updateTasks = new ConcurrentHashMap<>();
    }

    public void createBossBar(DungeonSession session, LivingEntity boss, String bossName) {
        // Remove existing boss bar if any
        removeBossBar(session);

        String format = plugin.getMessageManager().getBossBarFormat()
                .replace("{boss_name}", bossName);

        BossBar bossBar = Bukkit.createBossBar(
                format,
                plugin.getMessageManager().getBossBarColor(),
                plugin.getMessageManager().getBossBarStyle()
        );

        // Add all players in session
        for (Player player : session.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        bossBar.setVisible(true);
        bossBar.setProgress(1.0);

        sessionBossBars.put(session.getSessionId(), bossBar);

        // Start update task
        startUpdateTask(session, boss);
    }

    private void startUpdateTask(DungeonSession session, LivingEntity boss) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                BossBar bossBar = sessionBossBars.get(session.getSessionId());
                if (bossBar == null || boss == null || boss.isDead() || !boss.isValid()) {
                    removeBossBar(session);
                    cancel();
                    return;
                }

                // Update progress based on boss health
                double maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                double currentHealth = boss.getHealth();
                double progress = currentHealth / maxHealth;

                bossBar.setProgress(Math.max(0, Math.min(1, progress)));

                // Update players (in case new players joined)
                for (Player player : session.getOnlinePlayers()) {
                    if (!bossBar.getPlayers().contains(player)) {
                        bossBar.addPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // Update every 5 ticks

        updateTasks.put(session.getSessionId(), task);
    }

    public void removeBossBar(DungeonSession session) {
        String sessionId = session.getSessionId();

        // Cancel update task
        BukkitTask task = updateTasks.remove(sessionId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        // Remove boss bar
        BossBar bossBar = sessionBossBars.remove(sessionId);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    public void addPlayerToBossBar(DungeonSession session, Player player) {
        BossBar bossBar = sessionBossBars.get(session.getSessionId());
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    public void removePlayerFromBossBar(DungeonSession session, Player player) {
        BossBar bossBar = sessionBossBars.get(session.getSessionId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    public void removeAllBossBars() {
        for (BossBar bossBar : sessionBossBars.values()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        sessionBossBars.clear();

        for (BukkitTask task : updateTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        updateTasks.clear();
    }
}
