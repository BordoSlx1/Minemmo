package tr.minemmo.core.booster;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BoosterManager {

    private final JavaPlugin plugin;
    private final BoosterConfigManager boosterConfigManager;

    private final Map<String, Booster> boosters =
            new ConcurrentHashMap<>();

    public BoosterManager(
            JavaPlugin plugin
    ) {

        this.plugin = plugin;

        this.boosterConfigManager =
                new BoosterConfigManager(
                        plugin
                );
    }

    public void load() {

        boosters.clear();

        boosterConfigManager.load();

        ConfigurationSection section =
                boosterConfigManager
                        .getBoostersSection();

        if (section == null) {

            plugin.getLogger().warning(
                    "boosters.yml içerisinde "
                            + "'boosters' bölümü bulunamadı."
            );

            return;
        }

        int loaded = 0;

        for (String id :
                section.getKeys(false)) {

            ConfigurationSection boosterSection =
                    section.getConfigurationSection(
                            id
                    );

            if (boosterSection == null) {

                plugin.getLogger().warning(
                        "Booster atlandı. "
                                + "Geçersiz configuration bölümü: "
                                + id
                );

                continue;
            }

            try {

                Booster booster =
                        boosterConfigManager.createBooster(
                                id,
                                boosterSection
                        );

                register(booster);

                loaded++;

            } catch (Exception exception) {

                plugin.getLogger().severe(
                        "Booster yüklenemedi: "
                                + id
                );

                plugin.getLogger().severe(
                        "Hata: "
                                + exception.getMessage()
                );
            }
        }

        plugin.getLogger().info(
                "Booster config yüklendi. "
                        + "Başarılı: "
                        + loaded
                        + " / "
                        + section.getKeys(false).size()
        );
    }

    public void reload() {

        boosters.clear();

        boosterConfigManager.reload();

        ConfigurationSection section =
                boosterConfigManager
                        .getBoostersSection();

        if (section == null) {

            plugin.getLogger().warning(
                    "boosters.yml içerisinde "
                            + "'boosters' bölümü bulunamadı."
            );

            return;
        }

        int loaded = 0;

        for (String id :
                section.getKeys(false)) {

            ConfigurationSection boosterSection =
                    section.getConfigurationSection(
                            id
                    );

            if (boosterSection == null) {

                plugin.getLogger().warning(
                        "Booster atlandı: "
                                + id
                );

                continue;
            }

            try {

                Booster booster =
                        boosterConfigManager.createBooster(
                                id,
                                boosterSection
                        );

                register(booster);

                loaded++;

            } catch (Exception exception) {

                plugin.getLogger().severe(
                        "Booster yeniden yüklenemedi: "
                                + id
                );

                plugin.getLogger().severe(
                        "Hata: "
                                + exception.getMessage()
                );
            }
        }

        plugin.getLogger().info(
                "Booster config yeniden yüklendi. "
                        + "Başarılı: "
                        + loaded
                        + " / "
                        + section.getKeys(false).size()
        );
    }

    public void register(
            Booster booster
    ) {

        String id =
                booster.getId()
                        .toLowerCase();

        if (boosters.containsKey(id)) {

            throw new IllegalArgumentException(
                    "Bu ID ile kayıtlı bir booster zaten var: "
                            + id
            );
        }

        boosters.put(
                id,
                booster
        );

        plugin.getLogger().info(
                "Booster kaydedildi: "
                        + id
        );
    }

    public Booster get(
            String id
    ) {

        if (id == null || id.isBlank()) {
            return null;
        }

        return boosters.get(
                id.toLowerCase()
        );
    }

    public boolean exists(
            String id
    ) {

        return get(id) != null;
    }

    public Collection<Booster> getAll() {

        return Collections.unmodifiableCollection(
                boosters.values()
        );
    }

    public int getCount() {
        return boosters.size();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public BoosterConfigManager getBoosterConfigManager() {
        return boosterConfigManager;
    }
}
