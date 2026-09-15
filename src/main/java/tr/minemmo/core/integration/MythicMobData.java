package tr.minemmo.core.integration;

import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * MineMMO-Core tarafından kullanılan standart MythicMob verisi.
 *
 * Bu sınıf doğrudan MythicMobs API'sine bağımlı değildir.
 * MythicMobs verisini MineMMO-Core tarafında standartlaştırır.
 */
public final class MythicMobData {

    private final UUID entityUuid;
    private final Entity entity;
    private final String mobId;

    public MythicMobData(
            UUID entityUuid,
            Entity entity,
            String mobId
    ) {

        this.entityUuid = entityUuid;
        this.entity = entity;
        this.mobId = mobId;
    }

    /**
     * Bukkit entity UUID.
     */
    public UUID getEntityUuid() {

        return entityUuid;
    }

    /**
     * Bukkit entity.
     */
    public Entity getEntity() {

        return entity;
    }

    /**
     * MythicMobs internal mob ID.
     *
     * Örnek:
     * SkeletalKnight
     */
    public String getMobId() {

        return mobId;
    }
}