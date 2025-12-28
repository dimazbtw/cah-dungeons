package github.dimazbtw.dungeons.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int dungeonsCompleted;
    private int dungeonsFailed;
    private int mobsKilled;
    private int bossesKilled;
    private int deaths;
    private long totalPlayTime; // In milliseconds
    private int highestRound;
    private int points; // Currency for weapon upgrades

    // Weapon enchant levels
    private Map<String, Integer> weaponEnchants;

    // Stats/Perks
    private int vitality;      // Vida máxima (sem limite, +1 por ponto)
    private int strength;      // Dano extra (sem limite, +1 por ponto)
    private double agility;    // Speed (limite 3.0, +0.5 por ponto)
    private int perkPoints;    // Pontos disponíveis para distribuir
    
    private int level;
    private int experience;

    // Constantes
    public static final double MAX_AGILITY = 3.0;
    public static final double AGILITY_PER_POINT = 0.5;
    public static final int BASE_VITALITY = 20;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.weaponEnchants = new HashMap<>();
        this.dungeonsCompleted = 0;
        this.dungeonsFailed = 0;
        this.mobsKilled = 0;
        this.bossesKilled = 0;
        this.deaths = 0;
        this.totalPlayTime = 0;
        this.highestRound = 0;
        this.points = 0;
        this.vitality = 0;      // Começa com 0, vida base é 20
        this.strength = 0;
        this.agility = 0;
        this.perkPoints = 0;
        this.level = 1;
        this.experience = 0;
    }

    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }

    public int getDungeonsCompleted() {
        return dungeonsCompleted;
    }

    public void setDungeonsCompleted(int dungeonsCompleted) {
        this.dungeonsCompleted = dungeonsCompleted;
    }

    public void incrementDungeonsCompleted() {
        this.dungeonsCompleted++;
    }

    public int getDungeonsFailed() {
        return dungeonsFailed;
    }

    public void setDungeonsFailed(int dungeonsFailed) {
        this.dungeonsFailed = dungeonsFailed;
    }

    public void incrementDungeonsFailed() {
        this.dungeonsFailed++;
    }

    public int getMobsKilled() {
        return mobsKilled;
    }

    public void setMobsKilled(int mobsKilled) {
        this.mobsKilled = mobsKilled;
    }

    public void incrementMobsKilled() {
        this.mobsKilled++;
    }

    public void addMobsKilled(int amount) {
        this.mobsKilled += amount;
    }

    public int getBossesKilled() {
        return bossesKilled;
    }

    public void setBossesKilled(int bossesKilled) {
        this.bossesKilled = bossesKilled;
    }

    public void incrementBossesKilled() {
        this.bossesKilled++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public long getTotalPlayTime() {
        return totalPlayTime;
    }

    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
    }

    public void addPlayTime(long time) {
        this.totalPlayTime += time;
    }

    public String getFormattedPlayTime() {
        long hours = totalPlayTime / (1000 * 60 * 60);
        long minutes = (totalPlayTime / (1000 * 60)) % 60;
        return String.format("%dh %dm", hours, minutes);
    }

    public int getHighestRound() {
        return highestRound;
    }

    public void setHighestRound(int highestRound) {
        if (highestRound > this.highestRound) {
            this.highestRound = highestRound;
        }
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public boolean removePoints(int amount) {
        if (this.points >= amount) {
            this.points -= amount;
            return true;
        }
        return false;
    }

    public Map<String, Integer> getWeaponEnchants() {
        return weaponEnchants;
    }

    public void setWeaponEnchants(Map<String, Integer> weaponEnchants) {
        this.weaponEnchants = weaponEnchants;
    }

    public int getEnchantLevel(String enchant) {
        return weaponEnchants.getOrDefault(enchant, 0);
    }

    public void setEnchantLevel(String enchant, int level) {
        weaponEnchants.put(enchant, level);
    }

    // ============ STATS/PERKS ============

    public int getVitality() {
        return vitality;
    }

    public void setVitality(int vitality) {
        this.vitality = vitality;
    }

    /**
     * Retorna a vida máxima total (base + bônus de vitalidade)
     */
    public double getMaxHealth() {
        return BASE_VITALITY + (vitality * 2); // Cada ponto de vitalidade = +2 HP
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    /**
     * Retorna o dano extra baseado na força
     */
    public double getDamageBonus() {
        return strength * 0.5; // Cada ponto de força = +0.5 dano
    }

    public double getAgility() {
        return agility;
    }

    public void setAgility(double agility) {
        this.agility = Math.min(agility, MAX_AGILITY);
    }

    /**
     * Retorna o multiplicador de speed (0.0 a 0.3)
     * Speed padrão do Minecraft é 0.2, máximo permitido é 0.5
     */
    public float getSpeedModifier() {
        return (float) (agility * 0.1); // Cada 1.0 de agilidade = +0.1 speed
    }

    public int getPerkPoints() {
        return perkPoints;
    }

    public void setPerkPoints(int perkPoints) {
        this.perkPoints = perkPoints;
    }

    public void addPerkPoints(int amount) {
        this.perkPoints += amount;
    }

    /**
     * Tenta usar um perk point para aumentar vitalidade
     */
    public boolean upgradeVitality() {
        if (perkPoints <= 0) return false;
        perkPoints--;
        vitality++;
        return true;
    }

    /**
     * Tenta usar um perk point para aumentar força
     */
    public boolean upgradeStrength() {
        if (perkPoints <= 0) return false;
        perkPoints--;
        strength++;
        return true;
    }

    /**
     * Tenta usar um perk point para aumentar agilidade
     */
    public boolean upgradeAgility() {
        if (perkPoints <= 0) return false;
        if (agility >= MAX_AGILITY) return false;
        perkPoints--;
        agility += AGILITY_PER_POINT;
        agility = Math.min(agility, MAX_AGILITY);
        return true;
    }

    /**
     * Verifica se pode aumentar agilidade
     */
    public boolean canUpgradeAgility() {
        return perkPoints > 0 && agility < MAX_AGILITY;
    }

    // ============ LEVEL/XP ============

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    /**
     * Adiciona experiência e verifica level up
     * Retorna quantos níveis subiu
     */
    public int addExperience(int amount) {
        this.experience += amount;
        return checkLevelUp();
    }

    /**
     * Verifica e processa level ups
     * Retorna quantos níveis subiu
     */
    private int checkLevelUp() {
        int levelsGained = 0;
        int expRequired = getExpRequiredForNextLevel();
        
        while (experience >= expRequired) {
            experience -= expRequired;
            level++;
            perkPoints++; // Ganha 1 perk point por nível
            levelsGained++;
            expRequired = getExpRequiredForNextLevel();
        }
        
        return levelsGained;
    }

    public int getExpRequiredForNextLevel() {
        return 100 + (level * 50); // Nível 1 = 150 XP, Nível 2 = 200 XP, etc.
    }

    public double getLevelProgress() {
        return (double) experience / getExpRequiredForNextLevel();
    }

    public String getLevelProgressBar() {
        double progress = getLevelProgress();
        int filled = (int) (progress * 10);
        int empty = 10 - filled;
        return "§a" + "▌".repeat(filled) + "§8" + "▌".repeat(empty);
    }

    /**
     * Total de pontos de perk já utilizados
     */
    public int getTotalPerksUsed() {
        return vitality + strength + (int) (agility / AGILITY_PER_POINT);
    }

    /**
     * Pontos de atributos disponíveis para gastar
     * Cada nível dá 1 ponto de atributo
     */
    public int getAvailableAttributePoints() {
        int totalEarned = level; // 1 ponto por nível
        int totalUsed = getTotalPerksUsed();
        return Math.max(0, totalEarned - totalUsed);
    }
}
