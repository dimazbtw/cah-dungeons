package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.ActiveMob;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobManager {

    private final Main plugin;
    private final Map<String, DungeonMob> mobs;
    private final Map<UUID, ActiveMob> activeMobs;

    public MobManager(Main plugin) {
        this.plugin = plugin;
        this.mobs = new HashMap<>();
        this.activeMobs = new ConcurrentHashMap<>();
        loadMobs();
    }

    private void loadMobs() {
        mobs.clear();
        FileConfiguration config = plugin.getMobsConfig();
        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");

        if (mobsSection == null) {
            plugin.getLogger().warning("No mobs section found in mobs.yml!");
            return;
        }

        for (String mobId : mobsSection.getKeys(false)) {
            try {
                DungeonMob mob = loadMob(mobId, mobsSection.getConfigurationSection(mobId));
                if (mob != null) {
                    mobs.put(mobId, mob);
                    plugin.getLogger().info("Loaded mob: " + mobId);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load mob: " + mobId);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + mobs.size() + " mobs!");
    }

    private DungeonMob loadMob(String id, ConfigurationSection section) {
        if (section == null) return null;

        DungeonMob mob = new DungeonMob(id);

        // Entity type
        String entityTypeName = section.getString("entity", "ZOMBIE");
        try {
            mob.setEntityType(EntityType.valueOf(entityTypeName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            mob.setEntityType(EntityType.ZOMBIE);
        }

        // Basic info
        mob.setDisplayName(ColorUtils.colorize(section.getString("name", "&c" + id)));
        mob.setBaby(section.getBoolean("baby", false));
        mob.setRange(section.getDouble("range", 10.0));

        // Attributes
        ConfigurationSection attributes = section.getConfigurationSection("attributes");
        if (attributes != null) {
            mob.setSpeed(attributes.getDouble("speed", 0.25));
            mob.setDamage(attributes.getDouble("damage", 5.0));
            mob.setHealth(attributes.getDouble("health", 20.0));
            mob.setKnockbackResistance(attributes.getDouble("knockback-resistance", 0));
        }

        // Equipment
        ConfigurationSection equipment = section.getConfigurationSection("equipment");
        if (equipment != null) {
            mob.setHelmet(EquipmentParser.parseEquipment(equipment.getString("helmet")));
            mob.setChestplate(EquipmentParser.parseEquipment(equipment.getString("chestplate")));
            mob.setLeggings(EquipmentParser.parseEquipment(equipment.getString("leggings")));
            mob.setBoots(EquipmentParser.parseEquipment(equipment.getString("boots")));
            mob.setMainHand(EquipmentParser.parseEquipment(equipment.getString("main-hand")));
            mob.setOffHand(EquipmentParser.parseEquipment(equipment.getString("off-hand")));
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
                        mob.addConfiguredReward(rewardId, chance);
                    }
                }
            }
        }

        // Drops
        ConfigurationSection drops = section.getConfigurationSection("drops");
        if (drops != null) {
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

                    mob.addDrop(dropKey, drop);
                }
            }
        }

        return mob;
    }

    public void reload() {
        loadMobs();
    }

    public DungeonMob getMob(String id) {
        return mobs.get(id);
    }

    public LivingEntity spawnMob(String mobId, Location location, DungeonSession session) {
        DungeonMob mob = mobs.get(mobId);
        if (mob == null) {
            plugin.getLogger().warning("Mob not found: " + mobId);
            return null;
        }

        return spawnMob(mob, location, session, false);
    }

    public LivingEntity spawnMob(DungeonMob mob, Location location, DungeonSession session, boolean isBoss) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Invalid spawn location for mob: " + mob.getId());
            return null;
        }

        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, mob.getEntityType());

        // Set display name
        entity.setCustomName(mob.getDisplayName());
        entity.setCustomNameVisible(true);

        // Set baby if applicable
        if (entity instanceof Ageable ageable) {
            if (mob.isBaby()) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }

        // Set attributes
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mob.getHealth());
            entity.setHealth(mob.getHealth());
        }

        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mob.getSpeed());
        }

        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(mob.getDamage());
        }

        if (entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
            entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(mob.getKnockbackResistance());
        }

        // Set equipment
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            if (mob.getHelmet() != null) equipment.setHelmet(mob.getHelmet());
            if (mob.getChestplate() != null) equipment.setChestplate(mob.getChestplate());
            if (mob.getLeggings() != null) equipment.setLeggings(mob.getLeggings());
            if (mob.getBoots() != null) equipment.setBoots(mob.getBoots());
            if (mob.getMainHand() != null) equipment.setItemInMainHand(mob.getMainHand());
            if (mob.getOffHand() != null) equipment.setItemInOffHand(mob.getOffHand());

            // Set drop chances to 0
            equipment.setHelmetDropChance(0);
            equipment.setChestplateDropChance(0);
            equipment.setLeggingsDropChance(0);
            equipment.setBootsDropChance(0);
            equipment.setItemInMainHandDropChance(0);
            equipment.setItemInOffHandDropChance(0);
        }

        // Set metadata
        entity.setMetadata("dungeonMob", new FixedMetadataValue(plugin, mob.getId()));
        entity.setMetadata("dungeonSession", new FixedMetadataValue(plugin, session.getSessionId()));
        if (isBoss) {
            entity.setMetadata("dungeonBoss", new FixedMetadataValue(plugin, true));
        }

        // Remove default drops
        entity.setRemoveWhenFarAway(false);

        // Track active mob
        ActiveMob activeMob = new ActiveMob(entity, mob.getId(), session.getSessionId(), isBoss);
        activeMobs.put(entity.getUniqueId(), activeMob);

        // Add to session
        session.addMob(entity);

        // Efeitos de spawn
        if (isBoss) {
            plugin.getEffectsManager().playBossSpawnEffects(location);
        } else {
            plugin.getEffectsManager().playSpawnEffects(location);
        }

        return entity;
    }

    public void removeMob(Entity entity) {
        activeMobs.remove(entity.getUniqueId());
        if (!entity.isDead()) {
            entity.remove();
        }
    }

    public ActiveMob getActiveMob(Entity entity) {
        return activeMobs.get(entity.getUniqueId());
    }

    public boolean isDungeonMob(Entity entity) {
        return entity.hasMetadata("dungeonMob");
    }

    public String getMobId(Entity entity) {
        if (!entity.hasMetadata("dungeonMob")) return null;
        return entity.getMetadata("dungeonMob").get(0).asString();
    }

    public String getSessionId(Entity entity) {
        if (!entity.hasMetadata("dungeonSession")) return null;
        return entity.getMetadata("dungeonSession").get(0).asString();
    }

    public boolean isBoss(Entity entity) {
        return entity.hasMetadata("dungeonBoss");
    }

    public void clearSessionMobs(DungeonSession session) {
        for (UUID mobId : new HashSet<>(session.getActiveMobs())) {
            Entity entity = plugin.getServer().getEntity(mobId);
            if (entity != null) {
                removeMob(entity);
            }
        }
        session.getActiveMobs().clear();
    }

    public Collection<DungeonMob> getAllMobs() {
        return mobs.values();
    }

    public Set<String> getMobIds() {
        return mobs.keySet();
    }
}
