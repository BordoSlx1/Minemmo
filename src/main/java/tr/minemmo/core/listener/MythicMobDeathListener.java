package tr.minemmo.core.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import tr.minemmo.core.MineMMOCore;
import tr.minemmo.core.integration.MythicMobData;
import tr.minemmo.core.integration.MythicMobDeathData;
import tr.minemmo.core.integration.MythicMobDeathService;
import tr.minemmo.core.integration.MythicMobService;

import java.util.Optional;

/**
 * MythicMob ölüm eventlerini yakalar.
 *
 * Bu sınıf oyun mantığını işlemez.
 * Yalnızca eventten gerekli veriyi toplar ve
 * MythicMobDeathService katmanına aktarır.
 */
public final class MythicMobDeathListener implements Listener {

    private final MythicMobService mythicMobService;
    private final MythicMobDeathService mythicMobDeathService;

    public MythicMobDeathListener(
            MineMMOCore mineMMOCore
    ) {

        this.mythicMobService =
                mineMMOCore.getMythicMobService();

        this.mythicMobDeathService =
                mineMMOCore.getMythicMobDeathService();
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onEntityDeath(
            EntityDeathEvent event
    ) {

        Entity entity =
                event.getEntity();

        if (!mythicMobService.isEnabled()) {
            return;
        }

        Optional<MythicMobData> mobData =
                mythicMobService.getMobData(entity);

        if (mobData.isEmpty()) {
            return;
        }

        MythicMobData data =
                mobData.get();

        Player killer = null;

        if (entity instanceof LivingEntity livingEntity) {
            killer = livingEntity.getKiller();
        }

        MythicMobDeathData deathData =
                new MythicMobDeathData(
                        data.getEntityUuid(),
                        data.getEntity(),
                        data.getMobId(),
                        killer,
                        entity.getLocation().clone()
                );

        mythicMobDeathService.handleDeath(
                deathData
        );
    }
}