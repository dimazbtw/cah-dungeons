package github.dimazbtw.dungeons.models;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dungeon {

    private final String id;
    private String displayName;
    private List<String> description;

    // Material configuration
    private Material material;
    private String materialName;
    private List<String> materialLore;
    private boolean materialGlow;

    // Player limits
    private int minPlayers;
    private int maxPlayers;

    // Schedule (agendamento)
    private DungeonSchedule schedule;

    // Round configuration
    private boolean limitedRounds;
    private Map<Integer, RoundConfig> rounds;

    // Unlimited mode configuration
    private List<MobSpawnConfig> unlimitedMobs;
    private boolean spawnBossInUnlimited;
    private int bossSpawnRound;

    // Locations
    private List<Location> entryLocations;
    private Location exitLocation;
    private List<Location> mobSpawnLocations;
    private Location bossSpawnLocation;

    public Dungeon(String id) {
        this.id = id;
        this.description = new ArrayList<>();
        this.rounds = new HashMap<>();
        this.entryLocations = new ArrayList<>();
        this.mobSpawnLocations = new ArrayList<>();
        this.unlimitedMobs = new ArrayList<>();
        this.schedule = new DungeonSchedule(); // Por padrão, sempre aberta
    }

    // Schedule methods
    public DungeonSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(DungeonSchedule schedule) {
        this.schedule = schedule;
    }

    public boolean isOpen() {
        return schedule == null || schedule.isOpen();
    }

    public String getScheduleString() {
        if (schedule == null || !schedule.isEnabled()) {
            return "&aSempre aberta";
        }
        return schedule.getScheduleString();
    }

    public String getClosedMessage() {
        if (schedule != null) {
            return schedule.getClosedMessage();
        }
        return "&cEsta dungeon está fechada no momento.";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public List<String> getMaterialLore() {
        return materialLore;
    }

    public void setMaterialLore(List<String> materialLore) {
        this.materialLore = materialLore;
    }

    public boolean isMaterialGlow() {
        return materialGlow;
    }

    public void setMaterialGlow(boolean materialGlow) {
        this.materialGlow = materialGlow;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isLimitedRounds() {
        return limitedRounds;
    }

    public void setLimitedRounds(boolean limitedRounds) {
        this.limitedRounds = limitedRounds;
    }

    public Map<Integer, RoundConfig> getRounds() {
        return rounds;
    }

    public void setRounds(Map<Integer, RoundConfig> rounds) {
        this.rounds = rounds;
    }

    public RoundConfig getRound(int round) {
        return rounds.get(round);
    }

    public int getTotalRounds() {
        return rounds.size();
    }

    public List<MobSpawnConfig> getUnlimitedMobs() {
        return unlimitedMobs;
    }

    public void setUnlimitedMobs(List<MobSpawnConfig> unlimitedMobs) {
        this.unlimitedMobs = unlimitedMobs;
    }

    public boolean isSpawnBossInUnlimited() {
        return spawnBossInUnlimited;
    }

    public void setSpawnBossInUnlimited(boolean spawnBossInUnlimited) {
        this.spawnBossInUnlimited = spawnBossInUnlimited;
    }

    public int getBossSpawnRound() {
        return bossSpawnRound;
    }

    public void setBossSpawnRound(int bossSpawnRound) {
        this.bossSpawnRound = bossSpawnRound;
    }

    public List<Location> getEntryLocations() {
        return entryLocations;
    }

    public void setEntryLocations(List<Location> entryLocations) {
        this.entryLocations = entryLocations;
    }

    public Location getExitLocation() {
        return exitLocation;
    }

    public void setExitLocation(Location exitLocation) {
        this.exitLocation = exitLocation;
    }

    public List<Location> getMobSpawnLocations() {
        return mobSpawnLocations;
    }

    public void setMobSpawnLocations(List<Location> mobSpawnLocations) {
        this.mobSpawnLocations = mobSpawnLocations;
    }

    public Location getBossSpawnLocation() {
        return bossSpawnLocation;
    }

    public void setBossSpawnLocation(Location bossSpawnLocation) {
        this.bossSpawnLocation = bossSpawnLocation;
    }

    public void addEntryLocation(Location location) {
        this.entryLocations.add(location);
    }

    public void addMobSpawnLocation(Location location) {
        this.mobSpawnLocations.add(location);
    }

    public Location getRandomEntryLocation() {
        if (entryLocations.isEmpty()) return null;
        return entryLocations.get((int) (Math.random() * entryLocations.size()));
    }

    public Location getRandomMobSpawnLocation() {
        if (mobSpawnLocations.isEmpty()) return null;
        return mobSpawnLocations.get((int) (Math.random() * mobSpawnLocations.size()));
    }

    public String getDescriptionString() {
        return String.join("\n", description);
    }
}
