package github.dimazbtw.dungeons.models;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class ActiveMob {

    private final UUID entityId;
    private final String mobId;
    private final String sessionId;
    private final boolean isBoss;

    public ActiveMob(LivingEntity entity, String mobId, String sessionId, boolean isBoss) {
        this.entityId = entity.getUniqueId();
        this.mobId = mobId;
        this.sessionId = sessionId;
        this.isBoss = isBoss;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getMobId() {
        return mobId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isBoss() {
        return isBoss;
    }
}
