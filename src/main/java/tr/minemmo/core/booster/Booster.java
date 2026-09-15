package tr.minemmo.core.booster;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Booster {

    private final String id;
    private final String displayName;
    private final Material material;

    private final BoosterType type;

    private final long durationSeconds;
    private final double effectValue;

    private final BoosterEffectType effectType;
    private final int effectLevel;

    private final List<String> lore;
    private final List<String> storageLore;

    public Booster(
            String id,
            String displayName,
            String description,
            Material material,
            BoosterType type,
            long durationSeconds,
            double effectValue
    ) {

        this(
                id,
                displayName,
                material,
                type,
                durationSeconds,
                effectValue,
                BoosterEffectType.NONE,
                0,
                description == null
                        ? Collections.emptyList()
                        : List.of(description),
                Collections.emptyList()
        );
    }

    public Booster(
            String id,
            String displayName,
            String description,
            Material material,
            BoosterType type,
            long durationSeconds,
            double effectValue,
            BoosterEffectType effectType,
            int effectLevel
    ) {

        this(
                id,
                displayName,
                material,
                type,
                durationSeconds,
                effectValue,
                effectType,
                effectLevel,
                description == null
                        ? Collections.emptyList()
                        : List.of(description),
                Collections.emptyList()
        );
    }

    public Booster(
            String id,
            String displayName,
            Material material,
            BoosterType type,
            long durationSeconds,
            double effectValue,
            BoosterEffectType effectType,
            int effectLevel,
            List<String> lore,
            List<String> storageLore
    ) {

        if (id == null || id.isBlank()) {

            throw new IllegalArgumentException(
                    "Booster ID boş olamaz."
            );
        }

        if (displayName == null || displayName.isBlank()) {

            throw new IllegalArgumentException(
                    "Booster görünen adı boş olamaz."
            );
        }

        Objects.requireNonNull(
                material,
                "Booster materyali null olamaz."
        );

        Objects.requireNonNull(
                type,
                "Booster türü null olamaz."
        );

        Objects.requireNonNull(
                effectType,
                "Booster efekt tipi null olamaz."
        );

        Objects.requireNonNull(
                lore,
                "Booster lore listesi null olamaz."
        );

        Objects.requireNonNull(
                storageLore,
                "Booster storage lore listesi null olamaz."
        );

        if (durationSeconds < 0) {

            throw new IllegalArgumentException(
                    "Booster süresi negatif olamaz."
            );
        }

        if (effectLevel < 0) {

            throw new IllegalArgumentException(
                    "Booster efekt seviyesi negatif olamaz."
            );
        }

        this.id =
                id.toLowerCase();

        this.displayName =
                displayName;

        this.material =
                material;

        this.type =
                type;

        this.durationSeconds =
                durationSeconds;

        this.effectValue =
                effectValue;

        this.effectType =
                effectType;

        this.effectLevel =
                effectLevel;

        this.lore =
                List.copyOf(lore);

        this.storageLore =
                List.copyOf(storageLore);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public BoosterType getType() {
        return type;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public double getEffectValue() {
        return effectValue;
    }

    public BoosterEffectType getEffectType() {
        return effectType;
    }

    public int getEffectLevel() {
        return effectLevel;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getStorageLore() {
        return storageLore;
    }

    public boolean isPermanent() {
        return durationSeconds == 0;
    }
}