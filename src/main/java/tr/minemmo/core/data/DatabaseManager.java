package tr.minemmo.core.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private final JavaPlugin plugin;

    private String databaseUrl;

    private volatile boolean connected;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {

        File databaseFile = new File(
                plugin.getDataFolder(),
                "minemmo.db"
        );

        File parent = databaseFile.getParentFile();

        if (parent != null
                && !parent.exists()
                && !parent.mkdirs()) {

            throw new SQLException(
                    "MineMMO-Core veri klasörü oluşturulamadı: "
                            + parent.getAbsolutePath()
            );
        }

        databaseUrl =
                "jdbc:sqlite:"
                        + databaseFile.getAbsolutePath();

        try (Connection connection =
                     createConnection()) {

            configureConnection(connection);
            createTables(connection);
        }

        connected = true;

        plugin.getLogger().info(
                "SQLite veritabanı bağlantısı başarıyla kuruldu."
        );
    }

    private Connection createConnection()
            throws SQLException {

        if (databaseUrl == null) {

            throw new SQLException(
                    "SQLite veritabanı adresi hazırlanmadı."
            );
        }

        return DriverManager.getConnection(
                databaseUrl
        );
    }

    private void configureConnection(
            Connection connection
    ) throws SQLException {

        try (Statement statement =
                     connection.createStatement()) {

            statement.execute(
                    "PRAGMA foreign_keys = ON"
            );

            statement.execute(
                    "PRAGMA journal_mode = WAL"
            );

            statement.execute(
                    "PRAGMA synchronous = NORMAL"
            );

            statement.execute(
                    "PRAGMA busy_timeout = 5000"
            );
        }
    }

    private void createTables(
            Connection connection
    ) throws SQLException {

        /*
         * =========================================================
         * OYUNCU VERİLERİ
         * =========================================================
         */

        String playerDataSql = """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    booster_slots INTEGER NOT NULL DEFAULT 2,
                    rank TEXT NOT NULL DEFAULT 'Oyuncu',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """;

        try (Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(
                    playerDataSql
            );
        }

        /*
         * =========================================================
         * AKTİF BOOSTERLAR
         * =========================================================
         *
         * Bu tablo yalnızca oyuncunun AKTİF ettiği
         * boosterları tutar.
         *
         * activated_at ve expires_at burada kullanılır.
         *
         * /takviyeler GUI'sindeki bekleyen boosterlar
         * bu tabloda tutulmaz.
         */

        String playerBoostersSql = """
                CREATE TABLE IF NOT EXISTS player_boosters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    booster_id TEXT NOT NULL,
                    slot INTEGER NOT NULL,
                    activated_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(player_uuid, slot)
                )
                """;

        try (Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(
                    playerBoostersSql
            );
        }

        /*
         * =========================================================
         * TAKVİYE DEPOSU
         * =========================================================
         *
         * /takviyeler GUI'sinde bekleyen boosterlar burada tutulur.
         *
         * Bu boosterlar AKTİF DEĞİLDİR.
         *
         * Bu nedenle:
         *
         * - süreleri başlamaz
         * - XP bonusu vermez
         * - hasar bonusu vermez
         * - aktif booster slotu işgal etmez
         *
         * Oyuncu envanterindeki boosterı sağ tıkladığında
         * bu tabloya eklenir.
         *
         * GUI'deki booster sağ tıklandığında
         * bu tablodan silinip oyuncunun envanterine verilir.
         */

        String playerBoosterStorageSql = """
                CREATE TABLE IF NOT EXISTS player_booster_storage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    booster_id TEXT NOT NULL,
                    slot INTEGER NOT NULL,
                    UNIQUE(player_uuid, slot)
                )
                """;

        try (Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(
                    playerBoosterStorageSql
            );
        }

        /*
         * =========================================================
         * TABLO KONTROLÜ TAMAMLANDI
         * =========================================================
         */

        plugin.getLogger().info(
                "SQLite tabloları kontrol edildi."
        );

        plugin.getLogger().info(
                "Takviye deposu tablosu hazır: "
                        + "player_booster_storage"
        );
    }

    public Connection createPlayerConnection()
            throws SQLException {

        if (!connected) {

            throw new SQLException(
                    "SQLite bağlantısı aktif değil."
            );
        }

        Connection connection =
                createConnection();

        configureConnection(
                connection
        );

        return connection;
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {

        if (!connected) {
            return;
        }

        connected = false;

        plugin.getLogger().info(
                "SQLite veritabanı bağlantısı kapatıldı."
        );
    }
}