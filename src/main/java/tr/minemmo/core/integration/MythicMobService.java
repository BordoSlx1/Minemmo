package tr.minemmo.core.integration;

import org.bukkit.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class MythicMobService {

    private final MythicMobsHook mythicMobsHook;

    public MythicMobService(
            MythicMobsHook mythicMobsHook
    ) {
        this.mythicMobsHook = mythicMobsHook;
    }

    public boolean isEnabled() {
        return mythicMobsHook != null
                && mythicMobsHook.isEnabled();
    }

    public boolean isMythicMob(
            Entity entity
    ) {
        if (!isEnabled()) {
            return false;
        }

        return mythicMobsHook.isMythicMob(
                entity
        );
    }

    public Optional<MythicMobData> getMobData(
            Entity entity
    ) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        return mythicMobsHook.getMobData(
                entity
        );
    }

    public String getMobId(
            Entity entity
    ) {
        if (!isEnabled()) {
            return null;
        }

        return mythicMobsHook.getMobId(
                entity
        );
    }

    public boolean isMythicMob(
            UUID uuid
    ) {
        if (!isEnabled()) {
            return false;
        }

        return mythicMobsHook.isMythicMob(
                uuid
        );
    }
}