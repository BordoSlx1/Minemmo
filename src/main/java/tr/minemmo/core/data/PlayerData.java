package tr.minemmo.core.data;

import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;

    private int boosterSlots;
    private String rank;

    private final long createdAt;
    private volatile long updatedAt;

    public PlayerData(UUID uuid) {
        this(
                uuid,
                2,
                "Oyuncu",
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );
    }

    public PlayerData(
            UUID uuid,
            int boosterSlots,
            String rank,
            long createdAt,
            long updatedAt
    ) {
        this.uuid = uuid;
        this.boosterSlots = boosterSlots;
        this.rank = rank;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public synchronized int getBoosterSlots() {
        return boosterSlots;
    }

    public synchronized void setBoosterSlots(int boosterSlots) {
        if (boosterSlots < 0) {
            throw new IllegalArgumentException(
                    "Booster slot sayısı negatif olamaz."
            );
        }

        this.boosterSlots = boosterSlots;
        touch();
    }

    public synchronized String getRank() {
        return rank;
    }

    public synchronized void setRank(String rank) {
        if (rank == null || rank.isBlank()) {
            throw new IllegalArgumentException(
                    "Rank boş olamaz."
            );
        }

        this.rank = rank;
        touch();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    private synchronized void touch() {
        updatedAt = System.currentTimeMillis();
    }
}