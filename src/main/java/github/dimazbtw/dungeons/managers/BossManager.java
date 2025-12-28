package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonBoss;
import github.dimazbtw.dungeons.models.DungeonMob;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.dungeons.utils.EquipmentParser;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BossManager {

    private final Main plugin;
    private final Map<String, DungeonBoss> bosses;

    public BossManager(Main plugin) {
        this.plugin = plugin;
        this.bosses = new HashMap<>();
        loadBosses();
    }

    private void loadBosses() {
        bosses.clear();
        FileConfiguration config = plugin.getBossesConfig();
        ConfigurationSection bossesSection = config.getConfigurationSection("bosses");

        if (bossesSection == null) {
            plugin.getLogger().warning("No bosses section found in bosses.yml!");
            return;
        }

        for (String bossId : bossesSection.getKeys(false)) {
            try {
                DungeonBoss boss = loadBoss(bossId, bossesSection.getConfigurationSection(bossId));
                if (boss != null) {
                    bosses.put(bossId, boss);
                    plugin.getLogger().info("Loaded boss: " + bossId);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load boss: " + bossId);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + bosses.size() + " bosses!");
    }

    private DungeonBoss loadBoss(String id, ConfigurationSection section) {
        if (section == null) return null;

        DungeonBoss boss = new DungeonBoss(id);

        // Entity type
        String entityTypeName = section.getString("entity", "ZOMBIE");
        try {
            boss.setEntityType(EntityType.valueOf(entityTypeName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            boss.setEntityType(EntityType.ZOMBIE);
        }

        // Basic info
        boss.setDisplayName(ColorUtils.colorize(section.getString("name", "&c" + id)));
        boss.setBaby(section.getBoolean("baby", false));
        boss.setRange(section.getDouble("range", 20.0));

        // Attributes
        ConfigurationSection attributes = section.getConfigurationSection("attributes");
        if (attributes != null) {
            boss.setSpeed(attributes.getDouble("speed", 0.25));
            boss.setDamage(attributes.getDouble("damage", 15.0));
            boss.setHealth(attributes.getDouble("health", 200.0));
            boss.setKnockbackResistance(attributes.getDouble("knockback-resistance", 0.5));
        }

        // Equipment
        ConfigurationSection equipment = section.getConfigurationSection("equipment");
        if (equipment != null) {
            boss.setHelmet(EquipmentParser.parseEquipment(equipment.getString("helmet")));
            boss.setChestplate(EquipmentParser.parseEquipment(equipment.getString("chestplate")));
            boss.setLeggings(EquipmentParser.parseEquipment(equipment.getString("leggings")));
            boss.setBoots(EquipmentParser.parseEquipment(equipment.getString("boots")));
            boss.setMainHand(EquipmentParser.parseEquipment(equipment.getString("main-hand")));
            boss.setOffHand(EquipmentParser.parseEquipment(equipment.getString("off-hand")));
        }

        // Abilities
        ConfigurationSection abilities = section.getConfigurationSection("abilities");
        if (abilities != null) {
            for (String abilityKey : abilities.getKeys(false)) {
                ConfigurationSection abilitySection = abilities.getConfigurationSection(abilityKey);
                if (abilitySection != null) {
                    DungeonBoss.BossAbility ability = new DungeonBoss.BossAbility();
                    ability.setEnabled(abilitySection.getBoolean("enabled", false));
                    ability.setInterval(abilitySection.getInt("interval", 30));

                    Map<String, Object> params = new HashMap<>();
                    for (String key : abilitySection.getKeys(false)) {
                        if (!key.equals("enabled") && !key.equals("interval")) {
                            params.put(key, abilitySection.get(key));
                        }
                    }
                    ability.setParameters(params);

                    boss.addAbility(abilityKey, ability);
                }
            }
        }

        // Rewards (novo formato com id e chance)
        List<?> rewardsList = section.getList("rewards");
        if (rewardsList != null) {
            for (Object obj : rewardsList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rewardMap = (Map<String, Object>) obj;
                    String rewardId = (String) rewardMap.get("id");
                    double chance = 100;
                    Object chanceObj = rewardMap.get("chance");
                    if (chanceObj instanceof Number) {
                        chance = ((Number) chanceObj).doubleValue();
                    }
                    if (rewardId != null) {
                        boss.addConfiguredReward(rewardId, chance);
                    }
                }
            }
        }

        // Drops
        ConfigurationSection drops = section.getConfigurationSection("drops");
        if (drops != null) {
            Map<String, DungeonMob.MobDrop> bossDrops = new HashMap<>();
            for (String dropKey : drops.getKeys(false)) {
                ConfigurationSection dropSection = drops.getConfigurationSection(dropKey);
                if (dropSection != null) {
                    DungeonMob.MobDrop drop = new DungeonMob.MobDrop();

                    String materialStr = dropSection.getString("material", "STONE");
                    try {
                        drop.setMaterial(Material.valueOf(materialStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        drop.setMaterial(Material.STONE);
                    }

                    drop.setName(ColorUtils.colorize(dropSection.getString("name", "&f" + dropKey)));
                    drop.setLore(dropSection.getStringList("lore").stream()
                            .map(ColorUtils::colorize)
                            .toList());
                    drop.setChance(dropSection.getDouble("chance", 100));

                    String amount = dropSection.getString("amount", "1");
                    drop.setAmount(amount);

                    // Enchants
                    List<String> enchants = dropSection.getStringList("enchants");
                    Map<String, Integer> enchantMap = new HashMap<>();
                    for (String enchant : enchants) {
                        String[] parts = enchant.split(":");
                        if (parts.length == 2) {
                            enchantMap.put(parts[0], Integer.parseInt(parts[1]));
                        }
                    }
                    drop.setEnchants(enchantMap);

                    bossDrops.put(dropKey, drop);
                }
            }
            boss.setDrops(bossDrops);
        }

        return boss;
    }

    public void reload() {
        loadBosses();
    }

    public DungeonBoss getBoss(String id) {
        return bosses.get(id);
    }

    public LivingEntity spawnBoss(String bossId, Location location, DungeonSession session) {
        DungeonBoss boss = bosses.get(bossId);
        if (boss == null) {
            plugin.getLogger().warning("Boss not found: " + bossId);
            return null;
        }

        return spawnBoss(boss, location, session);
    }

    public LivingEntity spawnBoss(DungeonBoss boss, Location location, DungeonSession session) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Invalid spawn location for boss: " + boss.getId());
            return null;
        }

        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, boss.getEntityType());

        // Set display name
        entity.setCustomName(boss.getDisplayName());
        entity.setCustomNameVisible(true);

        // Set baby if applicable
        if (entity instanceof Ageable ageable) {
            if (boss.isBaby()) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }

        // Set attributes
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(boss.getHealth());
            entity.setHealth(boss.getHealth());
        }

        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(boss.getSpeed());
        }

        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(boss.getDamage());
        }

        if (entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
            entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(boss.getKnockbackResistance());
        }

        // Set equipment
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            if (boss.getHelmet() != null) equipment.setHelmet(boss.getHelmet());
            if (boss.getChestplate() != null) equipment.setChestplate(boss.getChestplate());
            if (boss.getLeggings() != null) equipment.setLeggings(boss.getLeggings());
            if (boss.getBoots() != null) equipment.setBoots(boss.getBoots());
            if (boss.getMainHand() != null) equipment.setItemInMainHand(boss.getMainHand());
            if (boss.getOffHand() != null) equipment.setItemInOffHand(boss.getOffHand());

            // Set drop chances to 0
            equipment.setHelmetDropChance(0);
            equipment.setChestplateDropChance(0);
            equipment.setLeggingsDropChance(0);
            equipment.setBootsDropChance(0);
            equipment.setItemInMainHandDropChance(0);
            equipment.setItemInOffHandDropChance(0);
        }

        // Set metadata
        entity.setMetadata("dungeonMob", new FixedMetadataValue(plugin, boss.getId()));
        entity.setMetadata("dungeonSession", new FixedMetadataValue(plugin, session.getSessionId()));
        entity.setMetadata("dungeonBoss", new FixedMetadataValue(plugin, true));

        // Remove default drops
        entity.setRemoveWhenFarAway(false);

        // Set as session boss
        session.setActiveBoss(entity);
        session.addMob(entity);

        // Start ability tasks
        startBossAbilities(boss, entity, session);

        return entity;
    }

    private void startBossAbilities(DungeonBoss boss, LivingEntity entity, DungeonSession session) {
        for (Map.Entry<String, DungeonBoss.BossAbility> entry : boss.getAbilities().entrySet()) {
            String abilityName = entry.getKey();
            DungeonBoss.BossAbility ability = entry.getValue();

            if (!ability.isEnabled()) continue;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isDead() || !entity.isValid() || session.isEnded()) {
                        cancel();
                        return;
                    }

                    executeAbility(abilityName, ability, entity, session);
                }
            }.runTaskTimer(plugin, ability.getInterval() * 20L, ability.getInterval() * 20L);
        }
    }

    private void executeAbility(String abilityName, DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        switch (abilityName.toLowerCase()) {
            case "summon-minions" -> executeSummonMinions(ability, boss, session);
            case "ground-slam" -> executeGroundSlam(ability, boss, session);
            case "arrow-rain" -> executeArrowRain(ability, boss, session);
            case "teleport" -> executeTeleport(ability, boss, session);
            case "web-trap" -> executeWebTrap(ability, boss, session);
            case "wither-strike" -> executeWitherStrike(ability, boss, session);
            case "dark-charge" -> executeDarkCharge(ability, boss, session);
        }
    }

    private void executeSummonMinions(DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        int count = ability.getIntParameter("count", 3);
        String mobType = (String) ability.getParameter("mob-type");
        if (mobType == null) mobType = "zombie";

        for (int i = 0; i < count; i++) {
            Location loc = boss.getLocation().add(
                    (Math.random() - 0.5) * 4,
                    0,
                    (Math.random() - 0.5) * 4
            );
            plugin.getMobManager().spawnMob(mobType, loc, session);
        }
    }

    private void executeGroundSlam(DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        double radius = ability.getDoubleParameter("radius", 5.0);
        double damage = ability.getDoubleParameter("damage", 10.0);

        Location loc = boss.getLocation();
        for (Player player : session.getOnlinePlayers()) {
            if (player.getLocation().distance(loc) <= radius) {
                player.damage(damage, boss);
                player.setVelocity(player.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5).setY(0.5));
            }
        }

        // Visual effect
        loc.getWorld().createExplosion(loc, 0, false, false);
    }

    private void executeArrowRain(DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        double radius = ability.getDoubleParameter("radius", 8.0);
        int arrows = ability.getIntParameter("arrows", 10);

        Location center = boss.getLocation();
        for (int i = 0; i < arrows; i++) {
            Location spawnLoc = center.clone().add(
                    (Math.random() - 0.5) * radius * 2,
                    10,
                    (Math.random() - 0.5) * radius * 2
            );
            Arrow arrow = center.getWorld().spawnArrow(spawnLoc, new org.bukkit.util.Vector(0, -1, 0), 1.5f, 0);
            arrow.setShooter(boss);
            arrow.setDamage(5);
        }
    }

    private void executeTeleport(DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        double range = ability.getDoubleParameter("range", 10.0);

        List<Player> players = session.getOnlinePlayers();
        if (players.isEmpty()) return;

        Player target = players.get((int) (Math.random() * players.size()));
        Location targetLoc = target.getLocation().add(
                (Math.random() - 0.5) * range,
                0,
                (Math.random() - 0.5) * range
        );

        boss.teleport(targetLoc);
        boss.getWorld().playSound(targetLoc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private void executeWebTrap(DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        double radius = ability.getDoubleParameter("radius", 4.0);
        int duration = ability.getIntParameter("duration", 5);

        for (Player player : session.getOnlinePlayers()) {
            if (player.getLocation().distance(boss.getLocation()) <= radius) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOW,
                        duration * 20,
                        2
                ));
            }
        }
    }

    private void executeWitherStrike(DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        int duration = ability.getIntParameter("duration", 5);
        int amplifier = ability.getIntParameter("amplifier", 1);

        if (boss.getTargetBlock(null, 10) != null) {
            Entity target = ((Mob) boss).getTarget();
            if (target instanceof Player player) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WITHER,
                        duration * 20,
                        amplifier
                ));
            }
        }
    }

    private void executeDarkCharge(DungeonBoss.BossAbility ability, LivingEntity boss, DungeonSession session) {
        double speedBoost = ability.getDoubleParameter("speed-boost", 2.0);
        int duration = ability.getIntParameter("duration", 3);

        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                duration * 20,
                (int) speedBoost
        ));
        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INCREASE_DAMAGE,
                duration * 20,
                1
        ));
    }

    public Collection<DungeonBoss> getAllBosses() {
        return bosses.values();
    }

    public Set<String> getBossIds() {
        return bosses.keySet();
    }
}
