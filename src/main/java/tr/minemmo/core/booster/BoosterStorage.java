package tr.minemmo.core.booster;

import java.util.UUID;

public final class BoosterStorage {

    private final UUID playerUuid;
    private final String boosterId;
    private final int slot;

    public BoosterStorage(
            UUID playerUuid,
            String boosterId,
            int slot
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
                    "Booster storage slotu negatif olamaz."
            );
        }

        this.playerUuid = playerUuid;
        this.boosterId = boosterId.toLowerCase();
        this.slot = slot;
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
}