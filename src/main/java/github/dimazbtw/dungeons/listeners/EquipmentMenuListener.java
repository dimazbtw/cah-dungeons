package github.dimazbtw.dungeons.listeners;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.menus.EquipmentMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class EquipmentMenuListener implements Listener {

    private final Main plugin;

    public EquipmentMenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof EquipmentMenu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof EquipmentMenu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EquipmentMenu menu) {
            menu.handleClose(event);
        }
    }
}
