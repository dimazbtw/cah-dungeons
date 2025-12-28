package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;

public class EffectsManager {

    private final Main plugin;
    private FileConfiguration rewardsConfig;

    public EffectsManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "rewards.yml");
        if (!file.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        loadConfig();
    }

    /**
     * Mostra holograma de XP flutuante
     */
    public void showXpHologram(Location location, int amount) {
        String format = rewardsConfig.getString("visuals.xp-hologram.format", "&a+{amount} XP");
        int duration = rewardsConfig.getInt("visuals.xp-hologram.duration", 40);
        double riseSpeed = rewardsConfig.getDouble("visuals.xp-hologram.rise-speed", 0.05);

        String text = ColorUtils.colorize(format.replace("{amount}", String.valueOf(amount)));

        Location holoLoc = location.clone().add(0, 1.5, 0);

        // Criar ArmorStand invisível como holograma
        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCustomName(text);
        hologram.setCustomNameVisible(true);
        hologram.setMarker(true);
        hologram.setSmall(true);
        hologram.setInvulnerable(true);

        // Animação de subir e desaparecer
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !hologram.isValid()) {
                    hologram.remove();
                    cancel();
                    return;
                }

                // Subir gradualmente
                hologram.teleport(hologram.getLocation().add(0, riseSpeed, 0));
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Mostra holograma genérico
     */
    public void showHologram(Location location, String text, int duration) {
        Location holoLoc = location.clone().add(0, 1.5, 0);

        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCustomName(ColorUtils.colorize(text));
        hologram.setCustomNameVisible(true);
        hologram.setMarker(true);
        hologram.setSmall(true);
        hologram.setInvulnerable(true);

        // Remover após duração
        plugin.getServer().getScheduler().runTaskLater(plugin, hologram::remove, duration);
    }

    /**
     * Efeitos de spawn de mob (partículas + trovão)
     */
    public void playSpawnEffects(Location location) {
        if (!rewardsConfig.getBoolean("visuals.spawn-particles.enabled", true)) return;

        World world = location.getWorld();
        if (world == null) return;

        // Partículas
        String particleType = rewardsConfig.getString("visuals.spawn-particles.type", "SMOKE_LARGE");
        int count = rewardsConfig.getInt("visuals.spawn-particles.count", 20);
        double offset = rewardsConfig.getDouble("visuals.spawn-particles.offset", 0.5);

        try {
            Particle particle = Particle.valueOf(particleType);
            world.spawnParticle(particle, location.clone().add(0, 1, 0), count, offset, offset, offset, 0.1);
        } catch (IllegalArgumentException e) {
            world.spawnParticle(Particle.SMOKE_LARGE, location.clone().add(0, 1, 0), count, offset, offset, offset, 0.1);
        }

        // Trovão visual (sem dano)
        if (rewardsConfig.getBoolean("visuals.spawn-particles.lightning", true)) {
            // Efeito de trovão visual usando partículas e som
            world.spawnParticle(Particle.FLASH, location.clone().add(0, 1, 0), 1);
            world.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0, 0.5, 0), 30, 0.3, 0.5, 0.3, 0.1);
            
            // Som de trovão mais suave
            world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
            world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 1.5f);
        }

        // Partículas de círculo no chão
        spawnCircleParticles(location, Particle.FLAME, 1.0, 20);
    }

    /**
     * Efeitos de morte de mob
     */
    public void playDeathEffects(Location location) {
        if (!rewardsConfig.getBoolean("visuals.death-particles.enabled", true)) return;

        World world = location.getWorld();
        if (world == null) return;

        String particleType = rewardsConfig.getString("visuals.death-particles.type", "SOUL");
        int count = rewardsConfig.getInt("visuals.death-particles.count", 15);
        double offset = rewardsConfig.getDouble("visuals.death-particles.offset", 0.3);

        try {
            Particle particle = Particle.valueOf(particleType);
            world.spawnParticle(particle, location.clone().add(0, 1, 0), count, offset, offset, offset, 0.05);
        } catch (IllegalArgumentException e) {
            world.spawnParticle(Particle.SOUL, location.clone().add(0, 1, 0), count, offset, offset, offset, 0.05);
        }

        // Partículas extras de fumaça
        world.spawnParticle(Particle.SMOKE_NORMAL, location.clone().add(0, 0.5, 0), 10, 0.2, 0.3, 0.2, 0.02);
        
        // Efeito de "poof"
        world.spawnParticle(Particle.CLOUD, location.clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.02);
    }

    /**
     * Efeitos de spawn de boss (mais intenso)
     */
    public void playBossSpawnEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Trovão real (visual apenas, sem dano - usa strikeLightningEffect)
        world.strikeLightningEffect(location);

        // Partículas intensas
        world.spawnParticle(Particle.EXPLOSION_LARGE, location.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.FLAME, location.clone().add(0, 0.5, 0), 50, 1, 0.5, 1, 0.1);
        world.spawnParticle(Particle.LAVA, location.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);

        // Círculo de partículas
        spawnCircleParticles(location, Particle.FLAME, 2.0, 40);
        spawnCircleParticles(location, Particle.SOUL_FIRE_FLAME, 1.5, 30);

        // Sons
        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f);
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1f);
    }

    /**
     * Efeitos de morte de boss
     */
    public void playBossDeathEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Explosão visual
        world.spawnParticle(Particle.EXPLOSION_HUGE, location.clone().add(0, 1, 0), 1);
        
        // Muitas partículas
        world.spawnParticle(Particle.TOTEM, location.clone().add(0, 1, 0), 100, 1, 1, 1, 0.5);
        world.spawnParticle(Particle.FIREWORKS_SPARK, location.clone().add(0, 2, 0), 50, 1, 1, 1, 0.3);

        // Sons
        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1f);
        world.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // Fogos de artifício em sequência
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 5) {
                    cancel();
                    return;
                }
                
                Location randomLoc = location.clone().add(
                        (Math.random() - 0.5) * 4,
                        Math.random() * 2 + 1,
                        (Math.random() - 0.5) * 4
                );
                
                world.spawnParticle(Particle.FIREWORKS_SPARK, randomLoc, 20, 0.2, 0.2, 0.2, 0.1);
                world.playSound(randomLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1f);
                
                count++;
            }
        }.runTaskTimer(plugin, 5L, 10L);
    }

    /**
     * Partículas em círculo
     */
    private void spawnCircleParticles(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            
            Location loc = center.clone().add(x, 0.1, z);
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Efeito de recompensa coletada
     */
    public void playRewardCollectEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 15, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.TOTEM, loc, 5, 0.3, 0.3, 0.3, 0.1);
        player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    /**
     * Efeito de level up
     */
    public void playLevelUpEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        world.spawnParticle(Particle.TOTEM, loc, 50, 0.5, 1, 0.5, 0.3);
        world.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
        
        spawnCircleParticles(player.getLocation(), Particle.FLAME, 1.5, 30);
    }

    // ==================== EFEITOS DE ENCANTAMENTOS ====================

    /**
     * Efeito de Thor - Raio no mob
     */
    public void playThorEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Raio visual (sem dano)
        world.strikeLightningEffect(location);

        // Partículas elétricas extras
        world.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        world.spawnParticle(Particle.FLASH, location.clone().add(0, 0.5, 0), 2);

        // Partículas de energia no chão
        spawnCircleParticles(location, Particle.ELECTRIC_SPARK, 1.5, 25);
    }

    /**
     * Efeito de Crit - Partículas de sangue/dano crítico
     */
    public void playCritEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Partículas de dano crítico (estrelas)
        world.spawnParticle(Particle.CRIT_MAGIC, location.clone().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.2);
        world.spawnParticle(Particle.CRIT, location.clone().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.15);

        // Partículas de "sangue" (redstone vermelha)
        Particle.DustOptions bloodDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.2f);
        world.spawnParticle(Particle.REDSTONE, location.clone().add(0, 1, 0), 20, 0.4, 0.5, 0.4, bloodDust);

        // Partículas de dano espalhando
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double x = 0.5 * Math.cos(angle);
            double z = 0.5 * Math.sin(angle);
            Location particleLoc = location.clone().add(x, 1 + Math.random() * 0.5, z);
            world.spawnParticle(Particle.REDSTONE, particleLoc, 3, 0.1, 0.1, 0.1, bloodDust);
        }

        // Som de impacto crítico
        world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
    }

    /**
     * Efeito de Lifesteal - Partículas de vida/cura
     */
    public void playLifestealEffect(Player player, Location targetLocation) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation().add(0, 1, 0);

        // Corações no jogador
        world.spawnParticle(Particle.HEART, playerLoc, 8, 0.4, 0.5, 0.4, 0);

        // Partículas verdes de cura
        Particle.DustOptions healDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 255, 100), 1.0f);
        world.spawnParticle(Particle.REDSTONE, playerLoc, 15, 0.3, 0.5, 0.3, healDust);

        // Partículas vermelhas saindo do mob em direção ao jogador
        Particle.DustOptions bloodDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 0.8f);
        
        // Criar linha de partículas do mob até o jogador
        Vector direction = playerLoc.toVector().subtract(targetLocation.toVector()).normalize();
        double distance = playerLoc.distance(targetLocation);
        
        new BukkitRunnable() {
            double traveled = 0;
            
            @Override
            public void run() {
                if (traveled >= distance) {
                    // Efeito final no jogador
                    world.spawnParticle(Particle.HEART, playerLoc, 3, 0.2, 0.3, 0.2, 0);
                    world.playSound(playerLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                    cancel();
                    return;
                }
                
                Location current = targetLocation.clone().add(direction.clone().multiply(traveled));
                current.add(0, 1, 0);
                world.spawnParticle(Particle.REDSTONE, current, 3, 0.1, 0.1, 0.1, bloodDust);
                world.spawnParticle(Particle.REDSTONE, current, 2, 0.1, 0.1, 0.1, healDust);
                
                traveled += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Som de absorção
        world.playSound(playerLoc, Sound.ENTITY_WITCH_DRINK, 0.5f, 1.5f);
    }

    /**
     * Efeito de Shockwave - Onda de choque expandindo
     */
    public void playShockwaveEffect(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        // Animação de onda expandindo
        new BukkitRunnable() {
            double currentRadius = 0.5;
            int ticks = 0;
            
            @Override
            public void run() {
                if (currentRadius > radius || ticks > 20) {
                    cancel();
                    return;
                }
                
                // Círculo de partículas expandindo
                int points = (int) (currentRadius * 12);
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = currentRadius * Math.cos(angle);
                    double z = currentRadius * Math.sin(angle);
                    
                    Location particleLoc = center.clone().add(x, 0.2, z);
                    world.spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.05, 0.1, 0.02);
                }
                
                // Partículas de poeira/impacto
                world.spawnParticle(Particle.EXPLOSION_NORMAL, center.clone().add(0, 0.3, 0), 5, currentRadius * 0.5, 0.1, currentRadius * 0.5, 0.05);
                
                currentRadius += radius / 8;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Explosão inicial no centro
        world.spawnParticle(Particle.EXPLOSION_LARGE, center.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
        
        // Som de impacto
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
    }
}
