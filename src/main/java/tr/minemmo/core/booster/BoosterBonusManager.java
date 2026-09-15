
package tr.minemmo.core.booster;

import org.bukkit.entity.Player;

import java.util.Collection;

public final class BoosterBonusManager {

    private final PlayerBoosterManager playerBoosterManager;
    private final BoosterManager boosterManager;

    public BoosterBonusManager(
            PlayerBoosterManager playerBoosterManager,
            BoosterManager boosterManager
    ) {

        this.playerBoosterManager =
                playerBoosterManager;

        this.boosterManager =
                boosterManager;
    }

    /**
     * Oyuncunun aktif XP bonus yüzdesini döndürür.
     *
     * Aynı türde birden fazla XP booster aktif olsa bile
     * bonuslar toplanmaz.
     *
     * Örnek:
     *
     * %25 + %25 = %25
     * %25 + %50 = %50
     */
    public double getXpBonusPercent(
            Player player
    ) {

        if (player == null) {
            return 0.0D;
        }

        Collection<PlayerBooster> activeBoosters =
                playerBoosterManager.getActiveBoosters(
                        player.getUniqueId()
                );

        double highestBonus = 0.0D;

        for (PlayerBooster playerBooster :
                activeBoosters) {

            if (playerBooster.isExpired()) {
                continue;
            }

            Booster booster =
                    boosterManager.get(
                            playerBooster.getBoosterId()
                    );

            if (booster == null) {
                continue;
            }

            if (booster.getEffectType()
                    != BoosterEffectType.XP_BONUS) {

                continue;
            }

            double value =
                    booster.getEffectValue();

            if (value <= 0.0D) {
                continue;
            }

            if (value > highestBonus) {
                highestBonus = value;
            }
        }

        return highestBonus;
    }

    /**
     * Verilen XP miktarına aktif XP booster bonusunu uygular.
     */
    public int applyXpBonus(
            Player player,
            int originalExperience
    ) {

        if (player == null
                || originalExperience <= 0) {

            return originalExperience;
        }

        double bonusPercent =
                getXpBonusPercent(player);

        if (bonusPercent <= 0.0D) {
            return originalExperience;
        }

        double multiplier =
                1.0D + (bonusPercent / 100.0D);

        double boostedExperience =
                originalExperience * multiplier;

        return (int) Math.round(
                boostedExperience
        );
    }

    /**
     * Oyuncunun aktif hasar bonus yüzdesini döndürür.
     *
     * Aynı anda birden fazla DAMAGE_BONUS varsa
     * değerler toplanmaz.
     *
     * Sadece en yüksek bonus kullanılır.
     */
    public double getDamageBonusPercent(
            Player player
    ) {

        if (player == null) {
            return 0.0D;
        }

        Collection<PlayerBooster> activeBoosters =
                playerBoosterManager.getActiveBoosters(
                        player.getUniqueId()
                );

        double highestBonus = 0.0D;

        for (PlayerBooster playerBooster :
                activeBoosters) {

            if (playerBooster.isExpired()) {
                continue;
            }

            Booster booster =
                    boosterManager.get(
                            playerBooster.getBoosterId()
                    );

            if (booster == null) {
                continue;
            }

            if (booster.getEffectType()
                    != BoosterEffectType.DAMAGE_BONUS) {

                continue;
            }

            double value =
                    booster.getEffectValue();

            if (value <= 0.0D) {
                continue;
            }

            if (value > highestBonus) {
                highestBonus = value;
            }
        }

        return highestBonus;
    }

    /**
     * Verilen hasara aktif DAMAGE_BONUS uygular.
     */
    public double applyDamageBonus(
            Player player,
            double originalDamage
    ) {

        if (player == null
                || originalDamage <= 0.0D) {

            return originalDamage;
        }

        double bonusPercent =
                getDamageBonusPercent(player);

        if (bonusPercent <= 0.0D) {
            return originalDamage;
        }

        double multiplier =
                1.0D + (bonusPercent / 100.0D);

        return originalDamage * multiplier;
    }
}

