package tr.minemmo.core.integration;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

public final class MythicMobsHook {

    private final JavaPlugin plugin;

    private boolean enabled;

    public MythicMobsHook(
            JavaPlugin plugin
    ) {

        this.plugin = plugin;
        this.enabled = false;
    }

    /**
     * MythicMobs bağlantısını başlatır.
     *
     * Config kapalıysa veya MythicMobs sunucuda yoksa
     * bağlantı kurulmaz.
     */
    public void initialize(
            boolean configEnabled
    ) {

        enabled = false;

        if (!configEnabled) {

            plugin.getLogger().info(
                    "MythicMobs entegrasyonu config tarafından kapatıldı."
            );

            return;
        }

        if (plugin.getServer()
                .getPluginManager()
                .getPlugin("MythicMobs") == null) {

            plugin.getLogger().warning(
                    "MythicMobs bulunamadı."
            );

            plugin.getLogger().warning(
                    "MythicMobs entegrasyonu devre dışı bırakıldı."
            );

            return;
        }

        try {

            MythicBukkit mythicBukkit =
                    MythicBukkit.inst();

            if (mythicBukkit == null) {

                plugin.getLogger().warning(
                        "MythicMobs API instance alınamadı."
                );

                return;
            }

            enabled = true;

            plugin.getLogger().info(
                    "MythicMobs entegrasyonu aktif."
            );

            plugin.getLogger().info(
                    "MythicMobs sürümü: "
                            + mythicBukkit.getVersion()
            );

        } catch (Exception exception) {

            enabled = false;

            plugin.getLogger().log(
                    java.util.logging.Level.WARNING,
                    "MythicMobs entegrasyonu başlatılamadı.",
                    exception
            );
        }
    }

    /**
     * MythicMobs entegrasyonunun aktif olup olmadığını döndürür.
     */
    public boolean isEnabled() {

        return enabled;
    }

    /**
     * Verilen Bukkit entity'sinin aktif bir MythicMob olup
     * olmadığını kontrol eder.
     */
    public boolean isMythicMob(
            Entity entity
    ) {

        if (!enabled
                || entity == null) {

            return false;
        }

        try {

            return MythicBukkit
                    .inst()
                    .getMobManager()
                    .getActiveMob(
                            entity.getUniqueId()
                    )
                    .isPresent();

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "MythicMob kontrolü sırasında hata oluştu: "
                            + exception.getMessage()
            );

            return false;
        }
    }

    /**
     * Bukkit entity'sinden MythicMobs ActiveMob nesnesini alır.
     */
    public Optional<ActiveMob> getActiveMob(
            Entity entity
    ) {

        if (!enabled
                || entity == null) {

            return Optional.empty();
        }

        try {

            return MythicBukkit
                    .inst()
                    .getMobManager()
                    .getActiveMob(
                            entity.getUniqueId()
                    );

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "ActiveMob alınırken hata oluştu: "
                            + exception.getMessage()
            );

            return Optional.empty();
        }
    }

    /**
     * Entity'nin MythicMob ID'sini döndürür.
     *
     * MythicMob değilse null döner.
     */
    public String getMobId(
            Entity entity
    ) {

        Optional<ActiveMob> activeMob =
                getActiveMob(entity);

        if (activeMob.isEmpty()) {

            return null;
        }

        try {

            return activeMob
                    .get()
                    .getType()
                    .getInternalName();

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "MythicMob ID alınırken hata oluştu: "
                            + exception.getMessage()
            );

            return null;
        }
    }

    /**
     * Entity'den MineMMO-Core standart MythicMob verisini oluşturur.
     *
     * MythicMob değilse boş Optional döner.
     */
    public Optional<MythicMobData> getMobData(
            Entity entity
    ) {

        if (!enabled
                || entity == null) {

            return Optional.empty();
        }

        Optional<ActiveMob> activeMob =
                getActiveMob(entity);

        if (activeMob.isEmpty()) {

            return Optional.empty();
        }

        try {

            String mobId =
                    activeMob
                            .get()
                            .getType()
                            .getInternalName();

            if (mobId == null
                    || mobId.isBlank()) {

                return Optional.empty();
            }

            MythicMobData data =
                    new MythicMobData(
                            entity.getUniqueId(),
                            entity,
                            mobId
                    );

            return Optional.of(data);

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "MythicMob verisi oluşturulurken hata oluştu: "
                            + exception.getMessage()
            );

            return Optional.empty();
        }
    }

    /**
     * UUID üzerinden MythicMob kontrolü yapar.
     */
    public boolean isMythicMob(
            UUID uuid
    ) {

        if (!enabled
                || uuid == null) {

            return false;
        }

        try {

            return MythicBukkit
                    .inst()
                    .getMobManager()
                    .getActiveMob(uuid)
                    .isPresent();

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "UUID üzerinden MythicMob kontrolü başarısız: "
                            + exception.getMessage()
            );

            return false;
        }
    }
}