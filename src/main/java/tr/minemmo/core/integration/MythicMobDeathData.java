package tr.minemmo.core.integration;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Öldürülen MythicMob hakkında standart ölüm verisi.
 *
 * MineMMO-Core içerisindeki ölüm tabanlı sistemler
 * bu sınıf üzerinden ortak verilere erişir.
 */
public final class MythicMobDeathData {

    private final UUID entityUuid;
    private final Entity entity;
    private final String mobId;
    private final Player killer;
    private final Location location;

    public MythicMobDeathData(
            UUID entityUuid,
            Entity entity,
            String mobId,
            Player killer,
            Location location
    ) {
        this.entityUuid = entityUuid;
        this.entity = entity;
        this.mobId = mobId;
        this.killer = killer;
        this.location = location;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getMobId() {
        return mobId;
    }

    public Player getKiller() {
        return killer;
    }

    public Location getLocation() {
        return location;
    }

    public boolean hasKiller() {
        return killer != null;
    }
}