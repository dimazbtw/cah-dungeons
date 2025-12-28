package github.dimazbtw.dungeons.listeners;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.menus.PerksMenu;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.dungeons.models.PlayerData;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data
        plugin.getPlayerDataManager().loadPlayer(player.getUniqueId());
        
        // Load equipment data
        plugin.getEquipmentManager().loadPlayer(player.getUniqueId());
        
        // Apply stats after a tick to ensure everything is loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
            if (data != null) {
                PerksMenu.applyStats(player, data);
            }
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Reset stats to default
        PerksMenu.resetStats(player);

        // Leave dungeon if in one
        if (plugin.getSessionManager() != null && plugin.getSessionManager().isInDungeon(player)) {
            plugin.getSessionManager().leaveSession(player);
        }

        // Save and unload player data
        plugin.getPlayerDataManager().unloadPlayer(player.getUniqueId());
        
        // Save and unload equipment data
        plugin.getEquipmentManager().unloadPlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (plugin.getSessionManager() == null) return;
        
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);

        if (session == null) return;
        
        // Clear drops in dungeon
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Handle death (coloca em espectador)
        plugin.getSessionManager().onPlayerDeath(player, session);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getSessionManager() == null) return;
        
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);

        if (session == null) return;

        // Respawn at entry location if still in dungeon
        if (!session.isEnded()) {
            var loc = session.getDungeon().getRandomEntryLocation();
            if (loc != null) {
                event.setRespawnLocation(loc);
            }
        }
        
        // Re-apply stats after respawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
            if (data != null) {
                PerksMenu.applyStats(player, data);
            }
        }, 2L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Check for leave item
        String leaveItemName = ColorUtils.colorize(
            plugin.getConfig().getString("custom-items.leave-item.name", "&c&lLeave Dungeon")
        );

        if (meta.getDisplayName().equals(leaveItemName)) {
            event.setCancelled(true);

            if (plugin.getSessionManager() != null && plugin.getSessionManager().isInDungeon(player)) {
                plugin.getSessionManager().leaveSession(player);
            }
        }
    }
}
