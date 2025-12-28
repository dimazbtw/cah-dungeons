package github.dimazbtw.dungeons.models;

import java.util.ArrayList;
import java.util.List;

public class RoundConfig {

    private final int roundNumber;
    private List<MobSpawnConfig> mobs;
    private String bossId;
    private boolean hasBoss;

    public RoundConfig(int roundNumber) {
        this.roundNumber = roundNumber;
        this.mobs = new ArrayList<>();
        this.hasBoss = false;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public List<MobSpawnConfig> getMobs() {
        return mobs;
    }

    public void setMobs(List<MobSpawnConfig> mobs) {
        this.mobs = mobs;
    }

    public void addMob(MobSpawnConfig mob) {
        this.mobs.add(mob);
    }

    public String getBossId() {
        return bossId;
    }

    public void setBossId(String bossId) {
        this.bossId = bossId;
        this.hasBoss = bossId != null && !bossId.isEmpty();
    }

    public boolean hasBoss() {
        return hasBoss;
    }

    public int getTotalMobCount() {
        return mobs.stream().mapToInt(MobSpawnConfig::getCount).sum();
    }
}
