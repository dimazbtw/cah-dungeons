package github.dimazbtw.dungeons.listeners;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonMob;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.dungeons.models.PlayerData;
import github.dimazbtw.dungeons.utils.RewardHandler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public class MobListener implements Listener {

    private final Main plugin;

    public MobListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Check if it's a dungeon mob
        if (!plugin.getMobManager().isDungeonMob(entity)) return;

        // Clear default drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Get session
        String sessionId = plugin.getMobManager().getSessionId(entity);
        if (sessionId == null) return;

        DungeonSession session = plugin.getSessionManager().getSession(sessionId);
        if (session == null) return;

        // Remove from session tracking
        session.removeMob(entity);

        // Get mob info
        String mobId = plugin.getMobManager().getMobId(entity);
        boolean isBoss = plugin.getMobManager().isBoss(entity);
        Location deathLocation = entity.getLocation();

        // Efeitos de morte
        if (isBoss) {
            plugin.getEffectsManager().playBossDeathEffects(deathLocation);
        } else {
            plugin.getEffectsManager().playDeathEffects(deathLocation);
        }

        // Get killer
        Player killer = entity.getKiller();
        if (killer == null) return;

        // Check if killer is in this session
        if (!session.hasPlayer(killer)) return;

        // Update player stats
        PlayerData playerData = plugin.getPlayerDataManager().getData(killer.getUniqueId());
        if (playerData != null) {
            playerData.incrementMobsKilled();
            if (isBoss) {
                playerData.incrementBossesKilled();
            }
        }

        // Calcular XP base
        int baseXp = isBoss ? 100 : 10;

        // Process rewards e XP
        if (isBoss) {
            var boss = plugin.getBossManager().getBoss(mobId);
            if (boss != null) {
                // Processar recompensas configuradas do boss
                for (var reward : boss.getConfiguredRewards()) {
                    plugin.getConfiguredRewardManager().processReward(
                            killer, reward.getRewardId(), reward.getChance(), session.getDungeon().getId()
                    );
                }
                RewardHandler.processDrops(plugin, killer, boss.getDrops());
            }
        } else {
            DungeonMob mob = plugin.getMobManager().getMob(mobId);
            if (mob != null) {
                // Processar recompensas configuradas do mob
                for (var reward : mob.getConfiguredRewards()) {
                    plugin.getConfiguredRewardManager().processReward(
                            killer, reward.getRewardId(), reward.getChance(), session.getDungeon().getId()
                    );
                }
                RewardHandler.processDrops(plugin, killer, mob.getDrops());
            }
        }

        // Dar XP e mostrar holograma
        int finalXp = RewardHandler.giveExperience(plugin, killer, baseXp);
        plugin.getEffectsManager().showXpHologram(deathLocation, finalXp);

        // Notify session manager
        plugin.getSessionManager().onMobKilled(session, isBoss);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        Entity target = event.getTarget();

        // Check if entity is a dungeon mob
        if (!plugin.getMobManager().isDungeonMob(entity)) return;

        // If targeting a player, check if they're in the same session
        if (target instanceof Player player) {
            String sessionId = plugin.getMobManager().getSessionId(entity);
            if (sessionId == null) return;

            DungeonSession session = plugin.getSessionManager().getSession(sessionId);
            if (session == null || !session.hasPlayer(player)) {
                event.setCancelled(true);
            }
        }
    }
}
