
package tr.minemmo.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tr.minemmo.core.booster.BoosterBonusManager;

public final class BoosterDamageListener
        implements Listener {

    private final BoosterBonusManager boosterBonusManager;

    public BoosterDamageListener(
            BoosterBonusManager boosterBonusManager
    ) {

        this.boosterBonusManager =
                boosterBonusManager;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityDamage(
            EntityDamageByEntityEvent event
    ) {

        /*
         * Hasarı veren doğrudan oyuncu değilse
         * booster uygulanmaz.
         */
        if (!(event.getDamager()
                instanceof Player player)) {

            return;
        }

        double originalDamage =
                event.getDamage();

        if (originalDamage <= 0.0D) {
            return;
        }

        double bonusPercent =
                boosterBonusManager
                        .getDamageBonusPercent(
                                player
                        );

        if (bonusPercent <= 0.0D) {
            return;
        }

        double boostedDamage =
                boosterBonusManager
                        .applyDamageBonus(
                                player,
                                originalDamage
                        );

        if (boostedDamage <= originalDamage) {
            return;
        }

        event.setDamage(
                boostedDamage
        );
    }
}

