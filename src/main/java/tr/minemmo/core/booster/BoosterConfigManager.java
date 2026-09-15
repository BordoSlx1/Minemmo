package tr.minemmo.core.booster;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class BoosterConfigManager {

    private final JavaPlugin plugin;

    private File boostersFile;
    private FileConfiguration boostersConfig;

    public BoosterConfigManager(
            JavaPlugin plugin
    ) {

        this.plugin = plugin;
    }

    public void load() {

        createFileIfNeeded();

        boostersConfig =
                YamlConfiguration.loadConfiguration(
                        boostersFile
                );

        plugin.getLogger().info(
                "boosters.yml yüklendi."
        );
    }

    private void createFileIfNeeded() {

        if (!plugin.getDataFolder().exists()
                && !plugin.getDataFolder().mkdirs()) {

            throw new IllegalStateException(
                    "MineMMO-Core veri klasörü oluşturulamadı."
            );
        }

        boostersFile =
                new File(
                        plugin.getDataFolder(),
                        "boosters.yml"
                );

        if (!boostersFile.exists()) {

            if (plugin.getResource("boosters.yml") == null) {

                throw new IllegalStateException(
                        "boosters.yml plugin kaynaklarında bulunamadı."
                );
            }

            plugin.saveResource(
                    "boosters.yml",
                    false
            );
        }
    }

    public void reload() {

        if (boostersFile == null) {

            load();

            return;
        }

        boostersConfig =
                YamlConfiguration.loadConfiguration(
                        boostersFile
                );

        plugin.getLogger().info(
                "boosters.yml yeniden yüklendi."
        );
    }

    public FileConfiguration getConfig() {

        if (boostersConfig == null) {
            load();
        }

        return boostersConfig;
    }

    public ConfigurationSection getBoostersSection() {

        return getConfig()
                .getConfigurationSection(
                        "boosters"
                );
    }

    public Booster createBooster(
            String id,
            ConfigurationSection section
    ) {

        if (id == null || id.isBlank()) {

            throw new IllegalArgumentException(
                    "Booster ID boş olamaz."
            );
        }

        if (section == null) {

            throw new IllegalArgumentException(
                    "Booster configuration bölümü null olamaz."
            );
        }

        String displayName =
                section.getString(
                        "display-name"
                );

        if (displayName == null
                || displayName.isBlank()) {

            throw new IllegalArgumentException(
                    "Booster '"
                            + id
                            + "' için display-name bulunamadı."
            );
        }

        String materialName =
                section.getString(
                        "material"
                );

        if (materialName == null
                || materialName.isBlank()) {

            throw new IllegalArgumentException(
                    "Booster '"
                            + id
                            + "' için material bulunamadı."
            );
        }

        Material material =
                Material.matchMaterial(
                        materialName
                );

        if (material == null) {

            throw new IllegalArgumentException(
                    "Booster '"
                            + id
                            + "' için geçersiz material: "
                            + materialName
            );
        }

        String typeName =
                section.getString(
                        "type",
                        "PASSIVE"
                );

        BoosterType type;

        try {

            type =
                    BoosterType.valueOf(
                            typeName.toUpperCase()
                    );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Booster '"
                            + id
                            + "' için geçersiz type: "
                            + typeName
            );
        }

        long duration =
                section.getLong(
                        "duration",
                        0
                );

        if (duration < 0) {

            throw new IllegalArgumentException(
                    "Booster '"
                            + id
                            + "' için duration negatif olamaz."
            );
        }

        ConfigurationSection effectSection =
                section.getConfigurationSection(
                        "effect"
                );

        BoosterEffectType effectType =
                BoosterEffectType.NONE;

        int effectLevel = 0;

        double effectValue = 0.0D;

        if (effectSection != null) {

            String effectTypeName =
                    effectSection.getString(
                            "type",
                            "NONE"
                    );

            try {

                effectType =
                        BoosterEffectType.valueOf(
                                effectTypeName.toUpperCase()
                        );

            } catch (IllegalArgumentException exception) {

                throw new IllegalArgumentException(
                        "Booster '"
                                + id
                                + "' için geçersiz "
                                + "effect.type: "
                                + effectTypeName
                );
            }

            effectLevel =
                    effectSection.getInt(
                            "level",
                            0
                    );

            if (effectLevel < 0) {

                throw new IllegalArgumentException(
                        "Booster '"
                                + id
                                + "' için effect.level "
                                + "negatif olamaz."
                );
            }

            effectValue =
                    effectSection.getDouble(
                            "value",
                            0.0D
                    );

            if (Double.isNaN(effectValue)
                    || Double.isInfinite(effectValue)) {

                throw new IllegalArgumentException(
                        "Booster '"
                                + id
                                + "' için effect.value "
                                + "geçersiz."
                );
            }
        }

        List<String> lore =
                section.getStringList(
                        "lore"
                );

        List<String> storageLore =
                section.getStringList(
                        "storage-lore"
                );

        return new Booster(
                id,
                displayName,
                material,
                type,
                duration,
                effectValue,
                effectType,
                effectLevel,
                lore,
                storageLore
        );
    }

    public File getBoostersFile() {
        return boostersFile;
    }
}