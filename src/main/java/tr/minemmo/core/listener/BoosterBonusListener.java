package tr.minemmo.core.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import tr.minemmo.core.booster.BoosterBonusManager;

public final class BoosterBonusListener
        implements Listener {

    private final BoosterBonusManager boosterBonusManager;

    public BoosterBonusListener(
            BoosterBonusManager boosterBonusManager
    ) {

        this.boosterBonusManager =
                boosterBonusManager;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerExperience(
            PlayerExpChangeEvent event
    ) {

        int originalExperience =
                event.getAmount();

        if (originalExperience <= 0) {
            return;
        }

        double bonusPercent =
                boosterBonusManager.getXpBonusPercent(
                        event.getPlayer()
                );

        if (bonusPercent <= 0.0D) {
            return;
        }

        int boostedExperience =
                boosterBonusManager.applyXpBonus(
                        event.getPlayer(),
                        originalExperience
                );

        if (boostedExperience <= originalExperience) {
            return;
        }

        /*
         * Gerçek verilecek XP miktarını değiştir.
         */
        event.setAmount(
                boostedExperience
        );

        /*
         * Konsola log yazdırmıyoruz.
         *
         * Bunun yerine oyuncunun hotbarının hemen
         * üstünde bulunan ActionBar alanında gösteriyoruz.
         */
        event.getPlayer().sendActionBar(
                Component.text()
                        .append(
                                Component.text(
                                        "+"
                                                + boostedExperience
                                                + " EXP",
                                        NamedTextColor.YELLOW
                                )
                        )
                        .append(
                                Component.text(
                                        " | ",
                                        NamedTextColor.GRAY
                                )
                        )
                        .append(
                                Component.text(
                                        "XP Booster",
                                        NamedTextColor.GREEN
                                )
                        )
                        .append(
                                Component.text(
                                        " (+"
                                                + formatPercent(
                                                        bonusPercent
                                                )
                                                + "%)",
                                        NamedTextColor.GRAY
                                )
                        )
                        .build()
        );
    }

    private String formatPercent(
            double value
    ) {

        if (value == Math.rint(value)) {
            return String.valueOf(
                    (int) value
            );
        }

        return String.format(
                java.util.Locale.US,
                "%.2f",
                value
        );
    }
}
