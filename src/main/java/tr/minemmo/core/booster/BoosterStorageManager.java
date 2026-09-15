package tr.minemmo.core.booster;

import org.bukkit.plugin.java.JavaPlugin;
import tr.minemmo.core.data.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class BoosterStorageManager {

    public static final int STORAGE_SIZE = 27;

    private final JavaPlugin plugin;
    private final BoosterManager boosterManager;
    private final DatabaseManager databaseManager;

    public BoosterStorageManager(
            JavaPlugin plugin,
            BoosterManager boosterManager,
            DatabaseManager databaseManager
    ) {
        this.plugin = plugin;
        this.boosterManager = boosterManager;
        this.databaseManager = databaseManager;
    }

    /**
     * Oyuncunun storage içindeki takviyelerini yükler.
     */
    public List<BoosterStorage> getStoredBoosters(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return Collections.emptyList();
        }

        List<BoosterStorage> result =
                new ArrayList<>();

        String sql = """
                SELECT
                    booster_id,
                    slot
                FROM player_booster_storage
                WHERE player_uuid = ?
                ORDER BY slot ASC
                """;

        try (
                Connection connection =
                        databaseManager.createPlayerConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    playerUuid.toString()
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    String boosterId =
                            resultSet.getString(
                                    "booster_id"
                            );

                    int slot =
                            resultSet.getInt(
                                    "slot"
                            );

                    if (boosterManager.get(boosterId) == null) {

                        plugin.getLogger().warning(
                                "Storage içerisinde kayıtlı olmayan "
                                        + "booster bulundu: "
                                        + boosterId
                        );

                        continue;
                    }

                    result.add(
                            new BoosterStorage(
                                    playerUuid,
                                    boosterId,
                                    slot
                            )
                    );
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Oyuncu booster storage verileri yüklenemedi: "
                            + playerUuid,
                    exception
            );
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Belirtilen slotta booster var mı?
     */
    public BoosterStorage get(
            UUID playerUuid,
            int slot
    ) {

        if (playerUuid == null || slot < 0) {
            return null;
        }

        String sql = """
                SELECT
                    booster_id
                FROM player_booster_storage
                WHERE player_uuid = ?
                  AND slot = ?
                """;

        try (
                Connection connection =
                        databaseManager.createPlayerConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    playerUuid.toString()
            );

            statement.setInt(
                    2,
                    slot
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return new BoosterStorage(
                        playerUuid,
                        resultSet.getString(
                                "booster_id"
                        ),
                        slot
                );
            }

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Booster storage kaydı okunamadı.",
                    exception
            );

            return null;
        }
    }

    /**
     * İlk boş storage slotunu bulur.
     */
    public int findEmptySlot(
            UUID playerUuid
    ) {

        boolean[] occupied =
                new boolean[STORAGE_SIZE];

        for (BoosterStorage storage :
                getStoredBoosters(playerUuid)) {

            int slot =
                    storage.getSlot();

            if (slot >= 0 && slot < STORAGE_SIZE) {
                occupied[slot] = true;
            }
        }

        for (int slot = 0;
             slot < STORAGE_SIZE;
             slot++) {

            if (!occupied[slot]) {
                return slot;
            }
        }

        return -1;
    }

    /**
     * Storage'a bir booster ekler.
     *
     * Aynı booster oyuncunun storage'ında
     * zaten bulunuyorsa tekrar eklenmesine izin verilmez.
     */
    public boolean add(
            UUID playerUuid,
            String boosterId,
            int slot
    ) {

        if (playerUuid == null
                || boosterId == null
                || boosterId.isBlank()
                || slot < 0
                || slot >= STORAGE_SIZE) {

            return false;
        }

        Booster booster =
                boosterManager.get(boosterId);

        if (booster == null) {
            return false;
        }

        /*
         * Seçilen slot zaten doluysa ekleme yapma.
         */
        if (get(playerUuid, slot) != null) {
            return false;
        }

        /*
         * Aynı booster oyuncunun storage'ında
         * zaten bulunuyorsa ikinci kez ekleme yapma.
         */
        String duplicateCheckSql = """
                SELECT 1
                FROM player_booster_storage
                WHERE player_uuid = ?
                  AND booster_id = ?
                LIMIT 1
                """;

        try (
                Connection connection =
                        databaseManager.createPlayerConnection();

                PreparedStatement duplicateCheck =
                        connection.prepareStatement(
                                duplicateCheckSql
                        )
        ) {

            duplicateCheck.setString(
                    1,
                    playerUuid.toString()
            );

            duplicateCheck.setString(
                    2,
                    booster.getId()
            );

            try (
                    ResultSet resultSet =
                            duplicateCheck.executeQuery()
            ) {

                if (resultSet.next()) {

                    plugin.getLogger().info(
                            "Booster storage'a eklenmedi; "
                                    + "aynı booster zaten mevcut: "
                                    + playerUuid
                                    + " -> "
                                    + booster.getId()
                    );

                    return false;
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Booster storage duplicate kontrolü başarısız.",
                    exception
            );

            return false;
        }

        /*
         * Aynı booster bulunmuyorsa yeni kayıt oluştur.
         */
        String sql = """
                INSERT INTO player_booster_storage (
                    player_uuid,
                    booster_id,
                    slot
                )
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection =
                        databaseManager.createPlayerConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    playerUuid.toString()
            );

            statement.setString(
                    2,
                    booster.getId()
            );

            statement.setInt(
                    3,
                    slot
            );

            statement.executeUpdate();

            return true;

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Booster storage'a eklenemedi.",
                    exception
            );

            return false;
        }
    }

    /**
     * Storage'dan belirtilen slotu siler.
     */
    public boolean remove(
            UUID playerUuid,
            int slot
    ) {

        if (playerUuid == null
                || slot < 0
                || slot >= STORAGE_SIZE) {

            return false;
        }

        String sql = """
                DELETE FROM player_booster_storage
                WHERE player_uuid = ?
                  AND slot = ?
                """;

        try (
                Connection connection =
                        databaseManager.createPlayerConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    playerUuid.toString()
            );

            statement.setInt(
                    2,
                    slot
            );

            int affectedRows =
                    statement.executeUpdate();

            return affectedRows == 1;

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Booster storage kaydı silinemedi.",
                    exception
            );

            return false;
        }
    }

    /**
     * Storage'ın dolu olup olmadığını kontrol eder.
     */
    public boolean isFull(
            UUID playerUuid
    ) {

        return findEmptySlot(playerUuid) == -1;
    }

    /**
     * Storage'da kaç booster olduğunu döndürür.
     */
    public int getCount(
            UUID playerUuid
    ) {

        return getStoredBoosters(playerUuid).size();
    }
}