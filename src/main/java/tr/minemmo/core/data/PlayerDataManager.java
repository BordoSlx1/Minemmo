package tr.minemmo.core.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class PlayerDataManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    private final Map<UUID, PlayerData> loadedData =
            new ConcurrentHashMap<>();

    /**
     * Bukkit scheduler üzerinden çalışan normal async işlemler.
     */
    private final Executor asyncExecutor;

    /**
     * Plugin kapanışında Bukkit scheduler kullanmadan
     * çalışabilecek özel Java executor.
     */
    private final Executor shutdownExecutor;

    public PlayerDataManager(
            JavaPlugin plugin,
            DatabaseManager databaseManager
    ) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;

        this.asyncExecutor = runnable ->
                plugin.getServer()
                        .getScheduler()
                        .runTaskAsynchronously(
                                plugin,
                                runnable
                        );

        this.shutdownExecutor =
                Executors.newSingleThreadExecutor(runnable -> {

                    Thread thread =
                            new Thread(
                                    runnable,
                                    "MineMMO-Core-Data-Shutdown"
                            );

                    thread.setDaemon(false);

                    return thread;
                });
    }

    /**
     * Oyuncu verisini async olarak yükler.
     *
     * Veri zaten cache'de varsa tekrar veritabanına gitmez.
     */
    public CompletableFuture<PlayerData> loadAsync(UUID uuid) {

        PlayerData cached = loadedData.get(uuid);

        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {

            try (Connection connection =
                         databaseManager.createPlayerConnection()) {

                String selectSql = """
                        SELECT uuid,
                               booster_slots,
                               rank,
                               created_at,
                               updated_at
                        FROM player_data
                        WHERE uuid = ?
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setString(
                            1,
                            uuid.toString()
                    );

                    try (ResultSet resultSet =
                                 statement.executeQuery()) {

                        if (resultSet.next()) {

                            PlayerData data =
                                    new PlayerData(
                                            UUID.fromString(
                                                    resultSet.getString(
                                                            "uuid"
                                                    )
                                            ),
                                            resultSet.getInt(
                                                    "booster_slots"
                                            ),
                                            resultSet.getString(
                                                    "rank"
                                            ),
                                            resultSet.getLong(
                                                    "created_at"
                                            ),
                                            resultSet.getLong(
                                                    "updated_at"
                                            )
                                    );

                            loadedData.put(
                                    uuid,
                                    data
                            );

                            plugin.getLogger().info(
                                    "Oyuncu verisi yüklendi: "
                                            + uuid
                            );

                            return data;
                        }
                    }
                }

                PlayerData newData =
                        createNewPlayer(
                                connection,
                                uuid
                        );

                loadedData.put(
                        uuid,
                        newData
                );

                plugin.getLogger().info(
                        "Yeni oyuncu verisi oluşturuldu: "
                                + uuid
                );

                return newData;

            } catch (SQLException exception) {

                plugin.getLogger().log(
                        Level.SEVERE,
                        "Oyuncu verisi yüklenirken hata oluştu: "
                                + uuid,
                        exception
                );

                throw new IllegalStateException(
                        "Oyuncu verisi yüklenemedi: "
                                + uuid,
                        exception
                );
            }

        }, asyncExecutor);
    }

    private PlayerData createNewPlayer(
            Connection connection,
            UUID uuid
    ) throws SQLException {

        long now =
                System.currentTimeMillis();

        PlayerData data =
                new PlayerData(
                        uuid,
                        2,
                        "Oyuncu",
                        now,
                        now
                );

        String insertSql = """
                INSERT INTO player_data
                (
                    uuid,
                    booster_slots,
                    rank,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             insertSql
                     )) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.setInt(
                    2,
                    data.getBoosterSlots()
            );

            statement.setString(
                    3,
                    data.getRank()
            );

            statement.setLong(
                    4,
                    data.getCreatedAt()
            );

            statement.setLong(
                    5,
                    data.getUpdatedAt()
            );

            statement.executeUpdate();
        }

        return data;
    }

    /**
     * Cache'deki oyuncu verisini async olarak kaydeder.
     */
    public CompletableFuture<Void> saveAsync(
            UUID uuid
    ) {

        PlayerData data =
                loadedData.get(uuid);

        if (data == null) {
            return CompletableFuture.completedFuture(
                    null
            );
        }

        return saveAsync(data);
    }

    /**
     * Verilen PlayerData nesnesini async olarak kaydeder.
     */
    public CompletableFuture<Void> saveAsync(
            PlayerData data
    ) {

        return CompletableFuture.runAsync(() -> {

            saveDataToDatabase(data);

        }, asyncExecutor);
    }

    /**
     * Oyuncuyu cache'den çıkarır ve önce kaydeder.
     */
    public CompletableFuture<Void> unloadAsync(
            UUID uuid
    ) {

        PlayerData data =
                loadedData.get(uuid);

        if (data == null) {
            return CompletableFuture.completedFuture(
                    null
            );
        }

        return saveAsync(data)
                .thenRun(() -> {

                    loadedData.remove(
                            uuid,
                            data
                    );

                    plugin.getLogger().info(
                            "Oyuncu verisi kaydedildi: "
                                    + uuid
                    );
                });
    }

    /**
     * Tüm aktif oyuncuları async olarak kaydeder.
     *
     * Normal çalışma sırasında kullanılır.
     */
    public CompletableFuture<Void> saveAllAsync() {

        List<PlayerData> snapshot =
                new ArrayList<>(
                        loadedData.values()
                );

        if (snapshot.isEmpty()) {
            return CompletableFuture.completedFuture(
                    null
            );
        }

        return CompletableFuture.runAsync(() -> {

            saveDataListToDatabase(
                    snapshot
            );

        }, asyncExecutor);
    }

    /**
     * Plugin kapanırken kullanılır.
     *
     * Bukkit scheduler kullanılmaz.
     * Veriler doğrudan mevcut thread üzerinde
     * SQLite'a yazılır.
     */
    public void saveAllSync() {

        List<PlayerData> snapshot =
                new ArrayList<>(
                        loadedData.values()
                );

        if (snapshot.isEmpty()) {

            plugin.getLogger().info(
                    "Kaydedilecek aktif oyuncu verisi bulunamadı."
            );

            return;
        }

        saveDataListToDatabase(
                snapshot
        );
    }

    /**
     * Tek bir PlayerData nesnesini SQLite'a yazar.
     */
    private void saveDataToDatabase(
            PlayerData data
    ) {

        String updateSql = """
                UPDATE player_data
                SET booster_slots = ?,
                    rank = ?,
                    updated_at = ?
                WHERE uuid = ?
                """;

        try (Connection connection =
                     databaseManager.createPlayerConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             updateSql
                     )) {

            statement.setInt(
                    1,
                    data.getBoosterSlots()
            );

            statement.setString(
                    2,
                    data.getRank()
            );

            statement.setLong(
                    3,
                    data.getUpdatedAt()
            );

            statement.setString(
                    4,
                    data.getUuid().toString()
            );

            statement.executeUpdate();

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Oyuncu verisi kaydedilirken hata oluştu: "
                            + data.getUuid(),
                    exception
            );
        }
    }

    /**
     * Birden fazla PlayerData nesnesini SQLite'a yazar.
     */
    private void saveDataListToDatabase(
            List<PlayerData> snapshot
    ) {

        String updateSql = """
                UPDATE player_data
                SET booster_slots = ?,
                    rank = ?,
                    updated_at = ?
                WHERE uuid = ?
                """;

        try (Connection connection =
                     databaseManager.createPlayerConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             updateSql
                     )) {

            for (PlayerData data : snapshot) {

                statement.setInt(
                        1,
                        data.getBoosterSlots()
                );

                statement.setString(
                        2,
                        data.getRank()
                );

                statement.setLong(
                        3,
                        data.getUpdatedAt()
                );

                statement.setString(
                        4,
                        data.getUuid().toString()
                );

                statement.addBatch();
            }

            statement.executeBatch();

            plugin.getLogger().info(
                    "Aktif oyuncu verileri kaydedildi: "
                            + snapshot.size()
            );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Aktif oyuncu verileri kaydedilirken "
                            + "hata oluştu.",
                    exception
            );
        }
    }

    public PlayerData get(UUID uuid) {
        return loadedData.get(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return loadedData.containsKey(uuid);
    }

    public int getLoadedPlayerCount() {
        return loadedData.size();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Plugin tamamen kapatıldıktan sonra özel executor
     * kullanılmayacağı için kapatılır.
     */
    public void shutdown() {

        if (shutdownExecutor
                instanceof java.util.concurrent.ExecutorService executorService) {

            executorService.shutdown();
        }
    }
}