package tr.minemmo.core.integration;

import tr.minemmo.core.MineMMOCore;

public final class MythicMobDeathService {

    private final MineMMOCore plugin;

    public MythicMobDeathService(
            MineMMOCore plugin
    ) {
        this.plugin = plugin;
    }

    public void handleDeath(
            MythicMobDeathData deathData
    ) {

        if (deathData == null) {
            return;
        }

        String mobId =
                deathData.getMobId();

        if (mobId == null || mobId.isBlank()) {
            return;
        }

        if (deathData.hasKiller()) {

            plugin.getLogger().info(
                    "MythicMob ölüm servisi işlendi: "
                            + mobId
                            + " | Oyuncu: "
                            + deathData.getKiller().getName()
            );

        } else {

            plugin.getLogger().info(
                    "MythicMob ölüm servisi işlendi: "
                            + mobId
                            + " | Öldüren oyuncu yok."
            );
        }

        /*
         * Drop işlemi burada yapılmaz.
         *
         * MythicMob'un drop sistemi MythicMobs
         * tarafından yönetilir.
         *
         * Bu servis ileride:
         * - MMO XP
         * - level
         * - görev
         * - booster
         * - mob ödülleri
         * gibi MineMMO-Core sistemlerini
         * yönetmek için kullanılacaktır.
         */
    }
}