package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.Dungeon;
import github.dimazbtw.dungeons.models.DungeonSchedule;
import github.dimazbtw.dungeons.models.MobSpawnConfig;
import github.dimazbtw.dungeons.models.RoundConfig;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class DungeonManager {

    private final Main plugin;
    private final Map<String, Dungeon> dungeons;
    // Armazena strings de localização para re-parse se mundo não estava carregado
    private final Map<String, Map<String, List<String>>> pendingLocations;

    public DungeonManager(Main plugin) {
        this.plugin = plugin;
        this.dungeons = new HashMap<>();
        this.pendingLocations = new HashMap<>();
        loadDungeons();
    }

    private void loadDungeons() {
        dungeons.clear();
        pendingLocations.clear();
        FileConfiguration config = plugin.getDungeonsConfig();
        ConfigurationSection dungeonsSection = config.getConfigurationSection("dungeons");

        if (dungeonsSection == null) {
            plugin.getLogger().warning("No dungeons section found in dungeons.yml!");
            return;
        }

        for (String dungeonId : dungeonsSection.getKeys(false)) {
            try {
                Dungeon dungeon = loadDungeon(dungeonId, dungeonsSection.getConfigurationSection(dungeonId));
                if (dungeon != null) {
                    dungeons.put(dungeonId, dungeon);
                    plugin.getLogger().info("Loaded dungeon: " + dungeonId);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load dungeon: " + dungeonId);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + dungeons.size() + " dungeons!");
        
        // Re-tentar carregar localizações pendentes
        retryPendingLocations();
    }

    private Dungeon loadDungeon(String id, ConfigurationSection section) {
        if (section == null) return null;

        Dungeon dungeon = new Dungeon(id);

        // Basic info
        dungeon.setDisplayName(ColorUtils.colorize(section.getString("display", id)));
        dungeon.setDescription(section.getStringList("description").stream()
                .map(ColorUtils::colorize)
                .toList());

        // Material configuration
        ConfigurationSection materialSection = section.getConfigurationSection("material");
        if (materialSection != null) {
            String materialType = materialSection.getString("type", "STONE");
            try {
                dungeon.setMaterial(Material.valueOf(materialType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                dungeon.setMaterial(Material.STONE);
            }
            dungeon.setMaterialName(ColorUtils.colorize(materialSection.getString("name", "&a" + id)));
            dungeon.setMaterialLore(materialSection.getStringList("lore").stream()
                    .map(ColorUtils::colorize)
                    .toList());
            dungeon.setMaterialGlow(materialSection.getBoolean("glow", false));
        }

        // Player limits
        ConfigurationSection playerLimit = section.getConfigurationSection("player-limit");
        if (playerLimit != null) {
            dungeon.setMinPlayers(playerLimit.getInt("min", 1));
            dungeon.setMaxPlayers(playerLimit.getInt("max", 4));
        }

        // Schedule (agendamento)
        ConfigurationSection scheduleSection = section.getConfigurationSection("schedule");
        if (scheduleSection != null && scheduleSection.getBoolean("enabled", false)) {
            DungeonSchedule schedule = new DungeonSchedule();
            schedule.setEnabled(true);
            schedule.setClosedMessage(ColorUtils.colorize(scheduleSection.getString("closed-message", 
                    "&cEsta dungeon está fechada no momento.")));

            // Carregar períodos
            List<?> periodsList = scheduleSection.getList("periods");
            if (periodsList != null) {
                for (Object obj : periodsList) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> periodMap = (Map<String, Object>) obj;
                        String start = (String) periodMap.get("start");
                        String end = (String) periodMap.get("end");
                        if (start != null && end != null) {
                            try {
                                schedule.addPeriod(new DungeonSchedule.TimePeriod(start, end));
                            } catch (Exception e) {
                                plugin.getLogger().warning("Invalid time period for dungeon " + id + ": " + start + "-" + end);
                            }
                        }
                    }
                }
            }

            // Carregar dias da semana
            List<Integer> days = scheduleSection.getIntegerList("days");
            if (!days.isEmpty()) {
                java.util.Set<java.time.DayOfWeek> allowedDays = new java.util.HashSet<>();
                for (int day : days) {
                    if (day >= 1 && day <= 7) {
                        allowedDays.add(java.time.DayOfWeek.of(day));
                    }
                }
                schedule.setAllowedDays(allowedDays);
            }

            dungeon.setSchedule(schedule);
        }

        // Rounds configuration
        ConfigurationSection roundsSection = section.getConfigurationSection("rounds");
        if (roundsSection != null) {
            dungeon.setLimitedRounds(roundsSection.getBoolean("limited", true));

            // Unlimited mode config
            ConfigurationSection unlimitedSection = roundsSection.getConfigurationSection("unlimited");
            if (unlimitedSection != null) {
                List<MobSpawnConfig> unlimitedMobs = new ArrayList<>();
                for (String mobString : unlimitedSection.getStringList("mobs")) {
                    try {
                        unlimitedMobs.add(MobSpawnConfig.parse(mobString));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid mob config: " + mobString);
                    }
                }
                dungeon.setUnlimitedMobs(unlimitedMobs);

                String spawnBoss = unlimitedSection.getString("spawn-boss", "false");
                if (spawnBoss.contains(",")) {
                    String[] parts = spawnBoss.split(",");
                    dungeon.setSpawnBossInUnlimited(Boolean.parseBoolean(parts[0].trim()));
                    dungeon.setBossSpawnRound(Integer.parseInt(parts[1].trim()));
                } else {
                    dungeon.setSpawnBossInUnlimited(Boolean.parseBoolean(spawnBoss));
                    dungeon.setBossSpawnRound(10);
                }
            }

            // Limited rounds config
            ConfigurationSection limitedRounds = roundsSection.getConfigurationSection("limited-rounds");
            if (limitedRounds != null) {
                Map<Integer, RoundConfig> rounds = new HashMap<>();
                for (String roundKey : limitedRounds.getKeys(false)) {
                    try {
                        int roundNum = Integer.parseInt(roundKey);
                        RoundConfig roundConfig = new RoundConfig(roundNum);

                        ConfigurationSection roundSection = limitedRounds.getConfigurationSection(roundKey);
                        if (roundSection != null) {
                            // Parse mobs
                            for (String mobString : roundSection.getStringList("mobs")) {
                                try {
                                    roundConfig.addMob(MobSpawnConfig.parse(mobString));
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Invalid mob config in round " + roundNum + ": " + mobString);
                                }
                            }

                            // Parse boss
                            String bossId = roundSection.getString("boss");
                            if (bossId != null && !bossId.isEmpty()) {
                                roundConfig.setBossId(bossId);
                            }
                        }

                        rounds.put(roundNum, roundConfig);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid round number: " + roundKey);
                    }
                }
                dungeon.setRounds(rounds);
            }
        }

        // Locations - com fallback para mundos não carregados
        ConfigurationSection locsSection = section.getConfigurationSection("locs");
        if (locsSection != null) {
            Map<String, List<String>> dungeonPending = new HashMap<>();
            
            // Entry locations
            List<String> entryStrings = locsSection.getStringList("entry");
            for (String entryLoc : entryStrings) {
                Location loc = parseLocation(entryLoc);
                if (loc != null) {
                    dungeon.addEntryLocation(loc);
                } else {
                    dungeonPending.computeIfAbsent("entry", k -> new ArrayList<>()).add(entryLoc);
                }
            }

            // Exit location
            String exitLoc = locsSection.getString("exit");
            if (exitLoc != null && !exitLoc.isEmpty()) {
                Location loc = parseLocation(exitLoc);
                if (loc != null) {
                    dungeon.setExitLocation(loc);
                } else {
                    dungeonPending.computeIfAbsent("exit", k -> new ArrayList<>()).add(exitLoc);
                }
            }

            // Mob spawn locations
            List<String> mobSpawnStrings = locsSection.getStringList("mobs-spawn");
            for (String mobLoc : mobSpawnStrings) {
                Location loc = parseLocation(mobLoc);
                if (loc != null) {
                    dungeon.addMobSpawnLocation(loc);
                } else {
                    dungeonPending.computeIfAbsent("mobs-spawn", k -> new ArrayList<>()).add(mobLoc);
                }
            }

            // Boss spawn location
            String bossLoc = locsSection.getString("boss-spawn");
            if (bossLoc != null && !bossLoc.isEmpty()) {
                Location loc = parseLocation(bossLoc);
                if (loc != null) {
                    dungeon.setBossSpawnLocation(loc);
                } else {
                    dungeonPending.computeIfAbsent("boss-spawn", k -> new ArrayList<>()).add(bossLoc);
                }
            }
            
            if (!dungeonPending.isEmpty()) {
                pendingLocations.put(id, dungeonPending);
            }
        }

        return dungeon;
    }

    /**
     * Re-tenta carregar localizações que falharam (mundo não estava carregado)
     */
    private void retryPendingLocations() {
        if (pendingLocations.isEmpty()) return;
        
        Iterator<Map.Entry<String, Map<String, List<String>>>> iter = pendingLocations.entrySet().iterator();
        
        while (iter.hasNext()) {
            Map.Entry<String, Map<String, List<String>>> entry = iter.next();
            String dungeonId = entry.getKey();
            Dungeon dungeon = dungeons.get(dungeonId);
            
            if (dungeon == null) {
                iter.remove();
                continue;
            }
            
            Map<String, List<String>> pending = entry.getValue();
            boolean allResolved = true;
            
            // Entry locations
            List<String> pendingEntry = pending.get("entry");
            if (pendingEntry != null) {
                Iterator<String> entryIter = pendingEntry.iterator();
                while (entryIter.hasNext()) {
                    Location loc = parseLocation(entryIter.next());
                    if (loc != null) {
                        dungeon.addEntryLocation(loc);
                        entryIter.remove();
                    } else {
                        allResolved = false;
                    }
                }
            }
            
            // Exit location
            List<String> pendingExit = pending.get("exit");
            if (pendingExit != null && !pendingExit.isEmpty()) {
                Location loc = parseLocation(pendingExit.get(0));
                if (loc != null) {
                    dungeon.setExitLocation(loc);
                    pendingExit.clear();
                } else {
                    allResolved = false;
                }
            }
            
            // Mob spawn locations
            List<String> pendingMobs = pending.get("mobs-spawn");
            if (pendingMobs != null) {
                Iterator<String> mobIter = pendingMobs.iterator();
                while (mobIter.hasNext()) {
                    Location loc = parseLocation(mobIter.next());
                    if (loc != null) {
                        dungeon.addMobSpawnLocation(loc);
                        mobIter.remove();
                    } else {
                        allResolved = false;
                    }
                }
            }
            
            // Boss spawn location
            List<String> pendingBoss = pending.get("boss-spawn");
            if (pendingBoss != null && !pendingBoss.isEmpty()) {
                Location loc = parseLocation(pendingBoss.get(0));
                if (loc != null) {
                    dungeon.setBossSpawnLocation(loc);
                    pendingBoss.clear();
                } else {
                    allResolved = false;
                }
            }
            
            if (allResolved) {
                iter.remove();
                plugin.getLogger().info("All locations loaded for dungeon: " + dungeonId);
            }
        }
        
        if (!pendingLocations.isEmpty()) {
            plugin.getLogger().warning("Some dungeon locations could not be loaded. Missing worlds?");
            for (String dungeonId : pendingLocations.keySet()) {
                plugin.getLogger().warning("  - " + dungeonId + ": " + pendingLocations.get(dungeonId).keySet());
            }
        }
    }

    private Location parseLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;

        String[] parts = locString.split(",");
        if (parts.length < 4) return null;

        try {
            String worldName = parts[0].trim();
            World world = Bukkit.getWorld(worldName);
            
            if (world == null) {
                // Não loga aqui pois pode ser retry
                return null;
            }

            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());

            float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid location format: " + locString);
            return null;
        }
    }

    public void reload() {
        loadDungeons();
    }
    
    /**
     * Chamado após os mundos carregarem para tentar novamente
     */
    public void retryLocations() {
        retryPendingLocations();
    }

    public Dungeon getDungeon(String id) {
        return dungeons.get(id);
    }

    public Collection<Dungeon> getAllDungeons() {
        return dungeons.values();
    }

    public Set<String> getDungeonIds() {
        return dungeons.keySet();
    }

    public boolean dungeonExists(String id) {
        return dungeons.containsKey(id);
    }

    public int getDungeonCount() {
        return dungeons.size();
    }

    public int getMobLimit(String dungeonId) {
        int perDungeonLimit = plugin.getConfig().getInt("dungeon-settings.per-dungeon-limits." + dungeonId, -1);
        if (perDungeonLimit > 0) {
            return perDungeonLimit;
        }
        return plugin.getConfig().getInt("dungeon-settings.mob-limit", 8);
    }

    /**
     * Salva uma localização no arquivo de configuração
     */
    public void saveLocation(String dungeonId, String type, Location location) {
        FileConfiguration config = plugin.getDungeonsConfig();
        String path = "dungeons." + dungeonId + ".locs." + type;
        String locString = formatLocation(location);

        switch (type.toLowerCase()) {
            case "entry" -> {
                List<String> entries = config.getStringList(path);
                entries.add(locString);
                config.set(path, entries);
            }
            case "exit", "boss-spawn" -> config.set(path, locString);
            case "mobs-spawn" -> {
                List<String> mobs = config.getStringList(path);
                mobs.add(locString);
                config.set(path, mobs);
            }
        }

        // Salvar arquivo
        try {
            config.save(new java.io.File(plugin.getDataFolder(), "dungeons.yml"));
            plugin.getLogger().info("Saved location for " + dungeonId + " - " + type);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save dungeon location: " + e.getMessage());
        }
    }

    /**
     * Formata Location para string no formato: world,x,y,z,yaw,pitch
     */
    private String formatLocation(Location loc) {
        return String.format("%s,%.1f,%.1f,%.1f,%.1f,%.1f",
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
        );
    }

    /**
     * Remove todas as localizações de um tipo
     */
    public void clearLocations(String dungeonId, String type) {
        FileConfiguration config = plugin.getDungeonsConfig();
        String path = "dungeons." + dungeonId + ".locs." + type;
        
        config.set(path, null);
        
        Dungeon dungeon = dungeons.get(dungeonId);
        if (dungeon != null) {
            switch (type.toLowerCase()) {
                case "entry" -> dungeon.getEntryLocations().clear();
                case "mobs-spawn" -> dungeon.getMobSpawnLocations().clear();
                case "exit" -> dungeon.setExitLocation(null);
                case "boss-spawn" -> dungeon.setBossSpawnLocation(null);
            }
        }

        try {
            config.save(new java.io.File(plugin.getDataFolder(), "dungeons.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to clear dungeon locations: " + e.getMessage());
        }
    }
}
