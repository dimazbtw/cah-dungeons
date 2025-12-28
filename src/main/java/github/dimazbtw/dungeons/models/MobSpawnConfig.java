package github.dimazbtw.dungeons.models;

public class MobSpawnConfig {

    private final String mobId;
    private final int count;
    private final double spawnDelay; // Delay between spawns in seconds

    public MobSpawnConfig(String mobId, int count) {
        this(mobId, count, 0);
    }

    public MobSpawnConfig(String mobId, int count, double spawnDelay) {
        this.mobId = mobId;
        this.count = count;
        this.spawnDelay = spawnDelay;
    }

    public String getMobId() {
        return mobId;
    }

    public int getCount() {
        return count;
    }

    public double getSpawnDelay() {
        return spawnDelay;
    }

    /**
     * Parse from config string format: "mobId,count" or "mobId,count,delay"
     */
    public static MobSpawnConfig parse(String configString) {
        String[] parts = configString.split(",");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid mob spawn config: " + configString);
        }

        String mobId = parts[0].trim();
        int count = Integer.parseInt(parts[1].trim());
        double delay = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 0;

        return new MobSpawnConfig(mobId, count, delay);
    }
}
