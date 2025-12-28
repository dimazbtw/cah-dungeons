package github.dimazbtw.dungeons.listeners;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonSession;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class WeaponListener implements Listener {

    private final Main plugin;

    public WeaponListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Check if player is in dungeon
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session == null) return;

        // Check if target is a dungeon mob
        if (!plugin.getMobManager().isDungeonMob(target)) return;

        // Check if player is using dungeon weapon
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!plugin.getWeaponManager().isDungeonWeapon(weapon)) return;

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;

        // Apply Smite bonus damage
        double smiteBonus = plugin.getWeaponManager().getSmiteDamageBonus(player);
        if (smiteBonus > 0) {
            finalDamage += baseDamage * (smiteBonus / 100);
        }

        // Apply Crit (double damage)
        if (plugin.getWeaponManager().shouldTriggerCrit(player)) {
            finalDamage *= 2;
            // Efeito de crítico com partículas de sangue
            plugin.getEffectsManager().playCritEffect(target.getLocation());
        }

        event.setDamage(finalDamage);

        // Show mob health ActionBar
        showMobHealthBar(player, target, finalDamage);

        // Apply Thor (lightning on nearby mobs)
        if (plugin.getWeaponManager().shouldTriggerThor(player)) {
            double radius = plugin.getWeaponManager().getThorRadius(player);
            Location targetLoc = target.getLocation();

            // Efeito de raio no alvo principal
            plugin.getEffectsManager().playThorEffect(targetLoc);

            for (Entity nearby : target.getNearbyEntities(radius, radius, radius)) {
                if (nearby instanceof LivingEntity livingNearby && plugin.getMobManager().isDungeonMob(nearby)) {
                    // Efeito de raio em cada mob próximo
                    plugin.getEffectsManager().playThorEffect(livingNearby.getLocation());
                    livingNearby.damage(5, player);
                }
            }
        }

        // Apply Shockwave (knockback nearby mobs)
        if (plugin.getWeaponManager().shouldTriggerShockwave(player)) {
            double radius = plugin.getWeaponManager().getShockwaveRadius(player);
            double knockback = plugin.getWeaponManager().getShockwaveKnockback(player);
            Location playerLoc = player.getLocation();

            // Efeito de onda de choque
            plugin.getEffectsManager().playShockwaveEffect(playerLoc, radius);

            for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
                if (nearby instanceof LivingEntity livingNearby && plugin.getMobManager().isDungeonMob(nearby)) {
                    Vector direction = livingNearby.getLocation().toVector()
                            .subtract(playerLoc.toVector())
                            .normalize()
                            .multiply(knockback)
                            .setY(0.5);

                    livingNearby.setVelocity(direction);
                }
            }
        }
    }

    private void showMobHealthBar(Player player, LivingEntity target, double damage) {
        // Calculate health after damage
        double currentHealth = Math.max(0, target.getHealth() - damage);
        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        // Get mob name
        String mobName = target.getCustomName();
        if (mobName == null) {
            mobName = target.getType().name();
        }

        // Create progress bar
        String healthBar = plugin.getMessageManager().createProgressBar(currentHealth, maxHealth);

        // Get actionbar message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mob_name", mobName);
        placeholders.put("health_bar", healthBar);

        String actionBar = plugin.getMessageManager().getActionBar("mob-health", placeholders);

        // Send to player
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBar));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Check if this damage will kill the target
        if (target.getHealth() - event.getFinalDamage() > 0) return;

        // Check if player is in dungeon
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session == null) return;

        // Check if target is a dungeon mob
        if (!plugin.getMobManager().isDungeonMob(target)) return;

        // Check if player is using dungeon weapon
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!plugin.getWeaponManager().isDungeonWeapon(weapon)) return;

        // Apply Lifesteal on kill
        if (plugin.getWeaponManager().shouldTriggerLifesteal(player)) {
            double healAmount = plugin.getWeaponManager().getLifestealAmount(player);
            double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
            player.setHealth(newHealth);

            // Efeito de lifesteal com partículas de vida
            plugin.getEffectsManager().playLifestealEffect(player, target.getLocation());
        }
    }
}
