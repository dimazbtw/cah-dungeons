package github.dimazbtw.dungeons.listeners;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Arrays;
import java.util.List;

public class DungeonListener implements Listener {

    private final Main plugin;
    private final List<String> allowedCommands = Arrays.asList(
        "/dungeon", "/dg", "/dungeons",
        "/dungeon leave", "/dg leave", "/dg l"
    );

    public DungeonListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session == null) return;

        // Prevent damage in waiting/starting state
        if (session.isWaiting() || session.isStarting()) {
            event.setCancelled(true);
            return;
        }

        // Prevent damage to dead players
        if (session.isPlayerDead(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        DungeonSession victimSession = plugin.getSessionManager().getPlayerSession(victim);
        DungeonSession attackerSession = plugin.getSessionManager().getPlayerSession(attacker);

        // Prevent PvP in dungeons
        if (victimSession != null || attackerSession != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);

        if (session != null) {
            // Prevent item drops in dungeon
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);

        if (session == null) return;

        // Allow internal teleports (same world, close distance)
        if (event.getFrom().getWorld() == event.getTo().getWorld()) {
            double distance = event.getFrom().distance(event.getTo());
            if (distance < 100) return;
        }

        // Block external teleports during dungeon
        if (session.isInProgress() || session.isBossRound()) {
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("already-in-dungeon"));
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);

        if (session == null) return;

        // Allow certain commands
        String command = event.getMessage().toLowerCase();
        for (String allowed : allowedCommands) {
            if (command.startsWith(allowed.toLowerCase())) {
                return;
            }
        }

        // Allow admin commands
        if (player.hasPermission("dungeons.admin")) {
            return;
        }

        // Block other commands during active dungeon
        if (session.isInProgress() || session.isBossRound()) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("already-in-dungeon"));
        }
    }
}
