package tr.minemmo.core.booster;

import java.util.UUID;

public final class PlayerBooster {

    private final UUID playerUuid;
    private final String boosterId;

    private final int slot;
    private final long activatedAt;
    private final long expiresAt;

    public PlayerBooster(
            UUID playerUuid,
            String boosterId,
            int slot,
            long activatedAt,
            long expiresAt
    ) {

        if (playerUuid == null) {
            throw new IllegalArgumentException(
                    "Oyuncu UUID değeri null olamaz."
            );
        }

        if (boosterId == null || boosterId.isBlank()) {
            throw new IllegalArgumentException(
                    "Booster ID boş olamaz."
            );
        }

        if (slot < 0) {
            throw new IllegalArgumentException(
                    "Booster slotu negatif olamaz."
            );
        }

        if (activatedAt < 0) {
            throw new IllegalArgumentException(
                    "Aktivasyon zamanı negatif olamaz."
            );
        }

        if (expiresAt < 0) {
            throw new IllegalArgumentException(
                    "Bitiş zamanı negatif olamaz."
            );
        }

        if (expiresAt != 0 && expiresAt < activatedAt) {
            throw new IllegalArgumentException(
                    "Bitiş zamanı aktivasyon zamanından önce olamaz."
            );
        }

        this.playerUuid = playerUuid;
        this.boosterId = boosterId.toLowerCase();
        this.slot = slot;
        this.activatedAt = activatedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getBoosterId() {
        return boosterId;
    }

    public int getSlot() {
        return slot;
    }

    public long getActivatedAt() {
        return activatedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt == 0;
    }

    public boolean isExpired() {

        if (isPermanent()) {
            return false;
        }

        return System.currentTimeMillis() >= expiresAt;
    }

    public long getRemainingSeconds() {

        if (isPermanent()) {
            return 0;
        }

        long remainingMillis =
                expiresAt - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            return 0;
        }

        return (remainingMillis + 999) / 1000;
    }
}