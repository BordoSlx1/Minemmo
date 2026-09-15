package tr.minemmo.core.booster;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public final class BoosterEffectManager {

    private static final String MANAGED_EFFECT_KEY =
            "managed_effect";

    private final JavaPlugin plugin;
    private final PlayerBoosterManager playerBoosterManager;

    private final NamespacedKey managedEffectKey;

    public BoosterEffectManager(
            JavaPlugin plugin,
            PlayerBoosterManager playerBoosterManager
    ) {

        this.plugin = plugin;
        this.playerBoosterManager =
                playerBoosterManager;

        this.managedEffectKey =
                new NamespacedKey(
                        plugin,
                        MANAGED_EFFECT_KEY
                );
    }

    public void reconcile(Player player) {

        if (player == null
                || !player.isOnline()) {

            return;
        }

        Map<PotionEffectType, BoosterEffectData> effects =
                new HashMap<>();

        for (PlayerBooster playerBooster :
                playerBoosterManager.getActiveBoosters(
                        player.getUniqueId()
                )) {

            if (playerBooster.isExpired()) {
                continue;
            }

            Booster booster =
                    playerBoosterManager
                            .getBoosterManager()
                            .get(
                                    playerBooster
                                            .getBoosterId()
                            );

            if (booster == null) {
                continue;
            }

            /*
             * Sadece Potion tabanlı efektler
             * burada işlenir.
             *
             * XP_BONUS
             * DROP_BONUS
             * DAMAGE_BONUS
             * MINING_SPEED
             * CUSTOM
             *
             * ilerleyen sistemlerde kendi
             * mekanik yöneticileri tarafından
             * işlenecektir.
             */

            if (!isPotionEffect(
                    booster.getEffectType()
            )) {

                continue;
            }

            PotionEffectType effectType =
                    getPotionEffectType(
                            booster.getEffectType()
                    );

            if (effectType == null) {
                continue;
            }

            long remainingSeconds =
                    playerBooster.isPermanent()
                            ? Long.MAX_VALUE
                            : playerBooster
                                    .getRemainingSeconds();

            BoosterEffectData current =
                    effects.get(effectType);

            if (current == null
                    || booster.getEffectLevel()
                    > current.effectLevel()) {

                effects.put(
                        effectType,
                        new BoosterEffectData(
                                booster.getEffectLevel(),
                                remainingSeconds
                        )
                );

            } else if (
                    booster.getEffectLevel()
                            == current.effectLevel()
                    && remainingSeconds
                            > current.remainingSeconds()
            ) {

                effects.put(
                        effectType,
                        new BoosterEffectData(
                                booster.getEffectLevel(),
                                remainingSeconds
                        )
                );
            }
        }

        removeManagedEffects(player);

        for (Map.Entry<PotionEffectType, BoosterEffectData> entry :
                effects.entrySet()) {

            PotionEffectType effectType =
                    entry.getKey();

            BoosterEffectData data =
                    entry.getValue();

            int durationTicks;

            if (data.remainingSeconds()
                    == Long.MAX_VALUE) {

                durationTicks =
                        Integer.MAX_VALUE;

            } else {

                long ticks =
                        data.remainingSeconds() * 20L;

                durationTicks =
                        (int) Math.min(
                                ticks,
                                Integer.MAX_VALUE
                        );
            }

            PotionEffect effect =
                    new PotionEffect(
                            effectType,
                            durationTicks,
                            data.effectLevel(),
                            false,
                            false,
                            true
                    );

            player.addPotionEffect(
                    effect
            );

            markEffectAsManaged(
                    player,
                    effectType
            );
        }

        plugin.getLogger().info(
                "Booster efektleri senkronize edildi: "
                        + player.getUniqueId()
                        + " ("
                        + effects.size()
                        + " efekt)"
        );
    }

    public void apply(
            Player player,
            Booster booster
    ) {

        if (player == null
                || booster == null
                || !player.isOnline()) {

            return;
        }

        reconcile(player);
    }

    public void remove(
            Player player,
            Booster booster
    ) {

        if (player == null
                || !player.isOnline()) {

            return;
        }

        reconcile(player);
    }

    private boolean isPotionEffect(
            BoosterEffectType effectType
    ) {

        if (effectType == null) {
            return false;
        }

        return switch (effectType) {

            case SPEED,
                 STRENGTH,
                 HASTE,
                 JUMP_BOOST,
                 REGENERATION,
                 FIRE_RESISTANCE,
                 NIGHT_VISION,
                 WATER_BREATHING,
                 LUCK -> true;

            case NONE,
                 XP_BONUS,
                 DROP_BONUS,
                 DAMAGE_BONUS,
                 MINING_SPEED,
                 CUSTOM -> false;
        };
    }

    private void removeManagedEffects(
            Player player
    ) {

        PersistentDataContainer container =
                player.getPersistentDataContainer();

        String storedEffects =
                container.get(
                        managedEffectKey,
                        PersistentDataType.STRING
                );

        if (storedEffects == null
                || storedEffects.isBlank()) {

            return;
        }

        String[] effectNames =
                storedEffects.split(",");

        for (String effectName :
                effectNames) {

            if (effectName.isBlank()) {
                continue;
            }

            PotionEffectType effectType =
                    PotionEffectType.getByName(
                            effectName
                    );

            if (effectType == null) {
                continue;
            }

            PotionEffect currentEffect =
                    player.getPotionEffect(
                            effectType
                    );

            if (currentEffect != null) {

                player.removePotionEffect(
                        effectType
                );
            }
        }

        container.remove(
                managedEffectKey
        );
    }

    private void markEffectAsManaged(
            Player player,
            PotionEffectType effectType
    ) {

        PersistentDataContainer container =
                player.getPersistentDataContainer();

        String current =
                container.get(
                        managedEffectKey,
                        PersistentDataType.STRING
                );

        String effectName =
                effectType.getName();

        if (current == null
                || current.isBlank()) {

            container.set(
                    managedEffectKey,
                    PersistentDataType.STRING,
                    effectName
            );

            return;
        }

        for (String existing :
                current.split(",")) {

            if (existing.equalsIgnoreCase(
                    effectName
            )) {

                return;
            }
        }

        container.set(
                managedEffectKey,
                PersistentDataType.STRING,
                current + "," + effectName
        );
    }

    private PotionEffectType getPotionEffectType(
            BoosterEffectType effectType
    ) {

        if (effectType == null) {
            return null;
        }

        return switch (effectType) {

            case SPEED ->
                    PotionEffectType.SPEED;

            case STRENGTH ->
                    PotionEffectType.STRENGTH;

            case HASTE ->
                    PotionEffectType.HASTE;

            case JUMP_BOOST ->
                    PotionEffectType.JUMP_BOOST;

            case REGENERATION ->
                    PotionEffectType.REGENERATION;

            case FIRE_RESISTANCE ->
                    PotionEffectType.FIRE_RESISTANCE;

            case NIGHT_VISION ->
                    PotionEffectType.NIGHT_VISION;

            case WATER_BREATHING ->
                    PotionEffectType.WATER_BREATHING;

            case LUCK ->
                    PotionEffectType.LUCK;

            case NONE,
                 XP_BONUS,
                 DROP_BONUS,
                 DAMAGE_BONUS,
                 MINING_SPEED,
                 CUSTOM -> null;
        };
    }

    private record BoosterEffectData(
            int effectLevel,
            long remainingSeconds
    ) {
    }
}