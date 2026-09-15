package tr.minemmo.core.booster;

import org.bukkit.plugin.java.JavaPlugin;
import tr.minemmo.core.data.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PlayerBoosterManager {

    private final JavaPlugin plugin;
    private final BoosterManager boosterManager;
    private final DatabaseManager databaseManager;

    private final Map<UUID, Map<Integer, PlayerBooster>> activeBoosters =
            new ConcurrentHashMap<>();

    public PlayerBoosterManager(
            JavaPlugin plugin,
            BoosterManager boosterManager,
            DatabaseManager databaseManager
    ) {
        this.plugin = plugin;
        this.boosterManager = boosterManager;
        this.databaseManager = databaseManager;
    }

    /**
     * Booster aktivasyonunu async olarak gerçekleştirir.
     *
     * Aynı booster zaten aktifse tekrar aktive edilmez.
     *
     * Slot önce RAM üzerinde rezerve edilir.
     * SQLite işlemi async thread'de yapılır.
     * Sonuç Bukkit ana thread'ine geri gönderilir.
     */
    public void activateAsync(
            UUID playerUuid,
            String boosterId,
            int slot,
            Consumer<Boolean> callback
    ) {

        if (playerUuid == null
                || boosterId == null
                || boosterId.isBlank()
                || slot < 0) {

            runCallback(callback, false);
            return;
        }

        Booster booster =
                boosterManager.get(boosterId);

        if (booster == null) {

            runCallback(callback, false);
            return;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.computeIfAbsent(
                        playerUuid,
                        ignored -> new ConcurrentHashMap<>()
                );

        synchronized (playerBoosters) {

            /*
             * Aynı booster zaten aktif mi?
             *
             * Slot boş olsa bile aynı booster ikinci kez
             * aktive edilemez.
             */
            if (containsActiveBooster(
                    playerBoosters,
                    booster.getId()
            )) {

                plugin.getLogger().info(
                        "Booster zaten aktif, tekrar aktive edilmedi: "
                                + playerUuid
                                + " -> "
                                + booster.getId()
                );

                runCallback(callback, false);
                return;
            }

            /*
             * İstenen slot doluysa aktivasyon yapılamaz.
             */
            if (playerBoosters.containsKey(slot)) {

                runCallback(callback, false);
                return;
            }

            long activatedAt =
                    System.currentTimeMillis();

            long expiresAt = 0;

            if (!booster.isPermanent()) {

                expiresAt =
                        activatedAt
                                + (booster.getDurationSeconds() * 1000L);
            }

            PlayerBooster playerBooster =
                    new PlayerBooster(
                            playerUuid,
                            booster.getId(),
                            slot,
                            activatedAt,
                            expiresAt
                    );

            playerBoosters.put(
                    slot,
                    playerBooster
            );

            plugin.getServer()
                    .getScheduler()
                    .runTaskAsynchronously(
                            plugin,
                            () -> {

                                boolean saved =
                                        saveBooster(
                                                playerBooster
                                        );

                                if (!saved) {

                                    synchronized (playerBoosters) {

                                        PlayerBooster current =
                                                playerBoosters.get(slot);

                                        if (current == playerBooster) {

                                            playerBoosters.remove(
                                                    slot
                                            );
                                        }

                                        if (playerBoosters.isEmpty()) {

                                            activeBoosters.remove(
                                                    playerUuid,
                                                    playerBoosters
                                            );
                                        }
                                    }
                                }

                                runCallback(
                                        callback,
                                        saved
                                );
                            }
                    );
        }
    }

    /**
     * Eski senkron aktivasyon metodu.
     *
     * Aynı booster zaten aktifse tekrar aktive edilmez.
     *
     * Yeni sistem activateAsync() kullanmalıdır.
     */
    public boolean activate(
            UUID playerUuid,
            String boosterId,
            int slot
    ) {

        if (playerUuid == null
                || boosterId == null
                || boosterId.isBlank()
                || slot < 0) {

            return false;
        }

        Booster booster =
                boosterManager.get(boosterId);

        if (booster == null) {
            return false;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.computeIfAbsent(
                        playerUuid,
                        ignored -> new ConcurrentHashMap<>()
                );

        synchronized (playerBoosters) {

            /*
             * Aynı booster zaten aktif mi?
             */
            if (containsActiveBooster(
                    playerBoosters,
                    booster.getId()
            )) {

                plugin.getLogger().info(
                        "Booster zaten aktif, tekrar aktive edilmedi: "
                                + playerUuid
                                + " -> "
                                + booster.getId()
                );

                return false;
            }

            /*
             * Slot dolu mu?
             */
            if (playerBoosters.containsKey(slot)) {
                return false;
            }

            long activatedAt =
                    System.currentTimeMillis();

            long expiresAt = 0;

            if (!booster.isPermanent()) {

                expiresAt =
                        activatedAt
                                + (booster.getDurationSeconds() * 1000L);
            }

            PlayerBooster playerBooster =
                    new PlayerBooster(
                            playerUuid,
                            booster.getId(),
                            slot,
                            activatedAt,
                            expiresAt
                    );

            if (!saveBooster(playerBooster)) {
                return false;
            }

            playerBoosters.put(
                    slot,
                    playerBooster
            );

            plugin.getLogger().info(
                    "Booster aktif edildi: "
                            + playerUuid
                            + " -> "
                            + booster.getId()
                            + " (Slot "
                            + slot
                            + ")"
            );

            return true;
        }
    }

    /**
     * Belirtilen booster'ın oyuncuda aktif olup olmadığını kontrol eder.
     *
     * Süresi dolmuş fakat henüz expiration task tarafından
     * temizlenmemiş kayıtlar aktif kabul edilmez.
     */
    public boolean hasBooster(
            UUID playerUuid,
            String boosterId
    ) {

        if (playerUuid == null
                || boosterId == null
                || boosterId.isBlank()) {

            return false;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.get(playerUuid);

        if (playerBoosters == null) {
            return false;
        }

        String normalizedId =
                boosterId.toLowerCase();

        for (PlayerBooster playerBooster :
                playerBoosters.values()) {

            if (!playerBooster.getBoosterId()
                    .equalsIgnoreCase(normalizedId)) {

                continue;
            }

            /*
             * Süresi dolmuşsa artık aktif kabul edilmez.
             */
            if (playerBooster.isExpired()) {
                continue;
            }

            return true;
        }

        return false;
    }

    /**
     * Aktif booster'lar içerisinde aynı booster ID'sinin
     * bulunup bulunmadığını kontrol eder.
     *
     * Bu metot synchronized blok içerisinde kullanılmak üzere
     * tasarlanmıştır.
     */
    private boolean containsActiveBooster(
            Map<Integer, PlayerBooster> playerBoosters,
            String boosterId
    ) {

        if (playerBoosters == null
                || boosterId == null
                || boosterId.isBlank()) {

            return false;
        }

        for (PlayerBooster playerBooster :
                playerBoosters.values()) {

            if (!playerBooster.getBoosterId()
                    .equalsIgnoreCase(boosterId)) {

                continue;
            }

            /*
             * Süresi dolmuş kayıt artık aktif değildir.
             */
            if (playerBooster.isExpired()) {
                continue;
            }

            return true;
        }

        return false;
    }

    public boolean deactivate(
            UUID playerUuid,
            int slot
    ) {

        if (playerUuid == null || slot < 0) {
            return false;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.get(playerUuid);

        if (playerBoosters == null) {
            return false;
        }

        PlayerBooster removed;

        synchronized (playerBoosters) {

            removed =
                    playerBoosters.remove(slot);
        }

        if (removed == null) {
            return false;
        }

        /*
         * Kayıt yalnızca aynı booster kimliği,
         * aktivasyon zamanı ve slot ile silinir.
         *
         * Böylece slot bu sırada yeniden kullanılmışsa
         * yeni booster yanlışlıkla silinmez.
         */
        if (!deleteBooster(removed)) {

            synchronized (playerBoosters) {

                PlayerBooster current =
                        playerBoosters.get(slot);

                if (current == null) {

                    playerBoosters.put(
                            slot,
                            removed
                    );
                }
            }

            return false;
        }

        if (playerBoosters.isEmpty()) {

            activeBoosters.remove(
                    playerUuid,
                    playerBoosters
            );
        }

        return true;
    }

    public PlayerBooster get(
            UUID playerUuid,
            int slot
    ) {

        if (playerUuid == null || slot < 0) {
            return null;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.get(playerUuid);

        if (playerBoosters == null) {
            return null;
        }

        return playerBoosters.get(slot);
    }

    public List<PlayerBooster> getActiveBoosters(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return Collections.emptyList();
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.get(playerUuid);

        if (playerBoosters == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        playerBoosters.values()
                )
        );
    }

    public int getActiveCount(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return 0;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.get(playerUuid);

        if (playerBoosters == null) {
            return 0;
        }

        return playerBoosters.size();
    }

    public void load(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                new ConcurrentHashMap<>();

        String sql = """
                SELECT
                    booster_id,
                    slot,
                    activated_at,
                    expires_at
                FROM player_boosters
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

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    String boosterId =
                            resultSet.getString(
                                    "booster_id"
                            );

                    int slot =
                            resultSet.getInt(
                                    "slot"
                            );

                    long activatedAt =
                            resultSet.getLong(
                                    "activated_at"
                            );

                    long expiresAt =
                            resultSet.getLong(
                                    "expires_at"
                            );

                    Booster booster =
                            boosterManager.get(
                                    boosterId
                            );

                    if (booster == null) {

                        plugin.getLogger().warning(
                                "Veritabanında bulunan "
                                        + "booster kayıtlı değil: "
                                        + boosterId
                        );

                        continue;
                    }

                    PlayerBooster playerBooster =
                            new PlayerBooster(
                                    playerUuid,
                                    boosterId,
                                    slot,
                                    activatedAt,
                                    expiresAt
                            );

                    if (playerBooster.isExpired()) {

                        deleteBooster(
                                playerBooster
                        );

                        continue;
                    }

                    /*
                     * Veritabanında teorik olarak aynı booster
                     * birden fazla kez bulunuyorsa yalnızca
                     * ilk aktif kayıt tutulur.
                     */
                    boolean duplicate =
                            false;

                    for (PlayerBooster existing :
                            playerBoosters.values()) {

                        if (existing.getBoosterId()
                                .equalsIgnoreCase(
                                        playerBooster.getBoosterId()
                                )) {

                            duplicate = true;

                            plugin.getLogger().warning(
                                    "Aynı booster için "
                                            + "birden fazla aktif kayıt bulundu: "
                                            + playerUuid
                                            + " -> "
                                            + boosterId
                            );

                            break;
                        }
                    }

                    if (duplicate) {
                        continue;
                    }

                    playerBoosters.put(
                            slot,
                            playerBooster
                    );
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Oyuncu boosterları yüklenemedi: "
                            + playerUuid,
                    exception
            );

            return;
        }

        if (playerBoosterManagerContains(playerUuid)) {

            /*
             * Oyuncu için mevcut RAM kayıtları varsa,
             * load işlemi onları ezmemeli.
             */
            return;
        }

        if (playerBoosters.isEmpty()) {

            activeBoosters.remove(
                    playerUuid
            );

        } else {

            activeBoosters.put(
                    playerUuid,
                    playerBoosters
            );
        }

        plugin.getLogger().info(
                "Oyuncu boosterları yüklendi: "
                        + playerUuid
                        + " ("
                        + playerBoosters.size()
                        + ")"
        );
    }

    private boolean playerBoosterManagerContains(
            UUID playerUuid
    ) {

        return activeBoosters.containsKey(
                playerUuid
        );
    }

    /**
     * Süresi dolan boosterları async olarak temizler.
     *
     * SQLite işlemleri Bukkit ana thread'i dışında çalışır.
     *
     * Callback ana thread'de çalışır ve başarılı şekilde
     * temizlenen oyuncuların UUID listesini alır.
     */
    public void removeExpiredAsync(
            Consumer<Set<UUID>> callback
    ) {

        plugin.getServer()
                .getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {

                            Set<PlayerBooster> expiredBoosters =
                                    new HashSet<>();

                            /*
                             * Önce süresi dolan kayıtların snapshot'ı alınır.
                             * Henüz RAM'den silinmezler.
                             */
                            for (Map.Entry<UUID, Map<Integer, PlayerBooster>> entry :
                                    activeBoosters.entrySet()) {

                                Map<Integer, PlayerBooster> playerBoosters =
                                        entry.getValue();

                                for (PlayerBooster playerBooster :
                                        playerBoosters.values()) {

                                    if (playerBooster.isExpired()) {

                                        expiredBoosters.add(
                                                playerBooster
                                        );
                                    }
                                }
                            }

                            Set<UUID> cleanedPlayers =
                                    new HashSet<>();

                            /*
                             * Her expired booster için önce RAM'de
                             * ilgili kaydın hâlâ aynı kayıt olduğu
                             * doğrulanır.
                             */
                            for (PlayerBooster expiredBooster :
                                    expiredBoosters) {

                                UUID playerUuid =
                                        expiredBooster.getPlayerUuid();

                                int slot =
                                        expiredBooster.getSlot();

                                Map<Integer, PlayerBooster> playerBoosters =
                                        activeBoosters.get(
                                                playerUuid
                                        );

                                if (playerBoosters == null) {
                                    continue;
                                }

                                boolean removedFromMemory = false;

                                synchronized (playerBoosters) {

                                    PlayerBooster current =
                                            playerBoosters.get(slot);

                                    if (current != expiredBooster) {
                                        continue;
                                    }

                                    playerBoosters.remove(
                                            slot
                                    );

                                    removedFromMemory = true;
                                }

                                if (!removedFromMemory) {
                                    continue;
                                }

                                /*
                                 * SQLite kaydı yalnızca aynı
                                 * booster kaydıysa silinir.
                                 */
                                boolean deleted =
                                        deleteBooster(
                                                expiredBooster
                                        );

                                if (deleted) {

                                    cleanedPlayers.add(
                                            playerUuid
                                    );

                                    plugin.getLogger().info(
                                            "Süresi dolan booster temizlendi: "
                                                    + playerUuid
                                                    + " -> "
                                                    + expiredBooster.getBoosterId()
                                                    + " (Slot "
                                                    + slot
                                                    + ")"
                                    );

                                } else {

                                    /*
                                     * SQLite silinemediyse RAM kaydı
                                     * geri yüklenir.
                                     *
                                     * Ancak slot bu sırada başka bir
                                     * booster tarafından doldurulduysa
                                     * yeni kayda dokunulmaz.
                                     */
                                    synchronized (playerBoosters) {

                                        PlayerBooster current =
                                                playerBoosters.get(slot);

                                        if (current == null) {

                                            playerBoosters.put(
                                                    slot,
                                                    expiredBooster
                                            );
                                        }
                                    }

                                    plugin.getLogger().warning(
                                            "Süresi dolan booster "
                                                    + "veritabanından silinemedi, "
                                                    + "RAM kaydı geri yüklendi: "
                                                    + playerUuid
                                                    + " -> "
                                                    + expiredBooster.getBoosterId()
                                    );
                                }

                                if (playerBoosters.isEmpty()) {

                                    activeBoosters.remove(
                                            playerUuid,
                                            playerBoosters
                                    );
                                }
                            }

                            runExpiredCallback(
                                    callback,
                                    cleanedPlayers
                            );
                        }
                );
    }

    /**
     * Oyuncunun tüm aktif boosterlarını temizler.
     *
     * Test ve yönetim amaçlı kullanılır.
     *
     * Her booster SQLite'tan kendi kimlik bilgileriyle silinir.
     * Böylece aynı slot daha sonra başka bir booster tarafından
     * kullanılırsa yeni kayıt yanlışlıkla silinmez.
     */
    public boolean clearAll(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return false;
        }

        Map<Integer, PlayerBooster> playerBoosters =
                activeBoosters.get(playerUuid);

        if (playerBoosters == null) {
            return false;
        }

        List<PlayerBooster> boosters;

        synchronized (playerBoosters) {

            boosters =
                    new ArrayList<>(
                            playerBoosters.values()
                    );
        }

        if (boosters.isEmpty()) {
            return false;
        }

        boolean allDeleted = true;

        for (PlayerBooster playerBooster : boosters) {

            boolean deleted =
                    deleteBooster(
                            playerBooster
                    );

            if (!deleted) {

                allDeleted = false;

                continue;
            }

            synchronized (playerBoosters) {

                PlayerBooster current =
                        playerBoosters.get(
                                playerBooster.getSlot()
                        );

                if (current == playerBooster) {

                    playerBoosters.remove(
                            playerBooster.getSlot()
                    );
                }
            }

            plugin.getLogger().info(
                    "Booster temizlendi: "
                            + playerUuid
                            + " -> "
                            + playerBooster.getBoosterId()
                            + " (Slot "
                            + playerBooster.getSlot()
                            + ")"
            );
        }

        if (playerBoosters.isEmpty()) {

            activeBoosters.remove(
                    playerUuid,
                    playerBoosters
            );
        }

        if (allDeleted) {

            plugin.getLogger().info(
                    "Oyuncunun tüm aktif boosterları temizlendi: "
                            + playerUuid
            );

        } else {

            plugin.getLogger().warning(
                    "Oyuncunun bazı boosterları temizlenemedi: "
                            + playerUuid
            );
        }

        return allDeleted;
    }

    public void clear(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return;
        }

        activeBoosters.remove(
                playerUuid
        );
    }

    public int getLoadedPlayerCount() {
        return activeBoosters.size();
    }

    private boolean saveBooster(
            PlayerBooster playerBooster
    ) {

        String sql = """
                INSERT INTO player_boosters (
                    player_uuid,
                    booster_id,
                    slot,
                    activated_at,
                    expires_at
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        databaseManager.createPlayerConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    playerBooster.getPlayerUuid()
                            .toString()
            );

            statement.setString(
                    2,
                    playerBooster.getBoosterId()
            );

            statement.setInt(
                    3,
                    playerBooster.getSlot()
            );

            statement.setLong(
                    4,
                    playerBooster.getActivatedAt()
            );

            statement.setLong(
                    5,
                    playerBooster.getExpiresAt()
            );

            statement.executeUpdate();

            return true;

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Booster veritabanına kaydedilemedi.",
                    exception
            );

            return false;
        }
    }

    /**
     * Booster kaydını kimlik bilgileriyle birlikte siler.
     *
     * UUID + slot tek başına kullanılmaz.
     * Böylece slot yeniden kullanılmışsa yeni kayıt silinmez.
     */
    private boolean deleteBooster(
            PlayerBooster playerBooster
    ) {

        String sql = """
                DELETE FROM player_boosters
                WHERE player_uuid = ?
                  AND booster_id = ?
                  AND slot = ?
                  AND activated_at = ?
                """;

        try (
                Connection connection =
                        databaseManager.createPlayerConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    playerBooster.getPlayerUuid()
                            .toString()
            );

            statement.setString(
                    2,
                    playerBooster.getBoosterId()
            );

            statement.setInt(
                    3,
                    playerBooster.getSlot()
            );

            statement.setLong(
                    4,
                    playerBooster.getActivatedAt()
            );

            int affectedRows =
                    statement.executeUpdate();

            /*
             * 1 = bizim kayıt silindi.
             * 0 = kayıt zaten yok veya farklı bir kayıt var.
             */
            return affectedRows == 1;

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Booster veritabanından silinemedi.",
                    exception
            );

            return false;
        }
    }

    private void runCallback(
            Consumer<Boolean> callback,
            boolean result
    ) {

        if (callback == null) {
            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> callback.accept(result)
                );
    }

    private void runExpiredCallback(
            Consumer<Set<UUID>> callback,
            Set<UUID> cleanedPlayers
    ) {

        if (callback == null) {
            return;
        }

        Set<UUID> result =
                Collections.unmodifiableSet(
                        new HashSet<>(
                                cleanedPlayers
                        )
                );

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> callback.accept(result)
                );
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}